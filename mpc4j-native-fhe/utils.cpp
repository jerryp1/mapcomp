#include "utils.h"
#include <utility>
#include <seal/util/polyarithsmallmod.h>

parms_id_type get_parms_id_for_chain_idx(const SEALContext& seal_context, size_t chain_idx) {
    // This function returns a parms_id matching the given chain index or -- if the chain
    // index is too large -- for the largest possible parameters (first data level).
    parms_id_type parms_id = seal_context.first_parms_id();
    while (seal_context.get_context_data(parms_id)->chain_index() > chain_idx) {
        parms_id = seal_context.get_context_data(parms_id)->next_context_data()->parms_id();
    }
    return parms_id;
}

EncryptionParameters generate_encryption_parameters(scheme_type type, uint32_t poly_modulus_degree, uint64_t plain_modulus, const vector<Modulus>& coeff_modulus) {
    EncryptionParameters parms = EncryptionParameters(type);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(coeff_modulus);
    return parms;
}

GaloisKeys generate_galois_keys(const SEALContext& context, KeyGenerator &keygen) {
    std::vector<uint32_t> galois_elts;
    const auto &context_data = context.first_context_data();
    auto &parms = context_data->parms();
    int N = parms.poly_modulus_degree();
    int logN = seal::util::get_power_of_two(N);
    for (int i = 0; i < logN; i++) {
        galois_elts.push_back((N + seal::util::exponentiate_uint(2, i)) / seal::util::exponentiate_uint(2, i));
    }
    GaloisKeys galois_keys;
    keygen.create_galois_keys(galois_elts, galois_keys);
    return galois_keys;
}

void poc_expand_flat(vector<vector<Ciphertext>>::iterator &result, vector<Ciphertext> &packed_swap_bits,
                     const SEALContext& context, int size, seal::GaloisKeys &galkey) {
    const auto &context_data2 = context.first_context_data();
    auto &parms2 = context_data2->parms();
    auto &coeff_modulus = parms2.coeff_modulus();
    size_t coeff_modulus_size = coeff_modulus.size();
    auto small_ntt_tables = context_data2->small_ntt_tables();
    size_t coeff_count = parms2.poly_modulus_degree();
    auto pool = MemoryManager::GetPool(mm_prof_opt::mm_force_new);
    int logN = seal::util::get_power_of_two(coeff_count);
    vector<Ciphertext> expanded_ciphers(coeff_count);
    //outloop is from 0-to-(l-1)
    for (int i = 0; i < packed_swap_bits.size(); i++) {
        auto rlwe_start = std::chrono::high_resolution_clock::now();
        expanded_ciphers = poc_rlwe_expand(packed_swap_bits[i], context, galkey, size);

        auto rlwe_end = std::chrono::high_resolution_clock::now();
        vector<uint64_t *> rlwe_decom;
        for (int j = 0; j < size; j++) {
            ///put jth expanded ct in ith idx slot  of jt gswct
            result[j][i] = expanded_ciphers[j];
        }
    }
}


vector<Ciphertext> poc_rlwe_expand(Ciphertext packed_query, const SEALContext& context, const seal::GaloisKeys& galkey, uint64_t size) {
    // this function return size vector of RLWE ciphertexts it takes a single RLWE packed ciphertext
    Evaluator evaluator1(context);
    const auto &context_data = context.first_context_data();
    auto &parms = context_data->parms();
    auto &coeff_modulus = parms.coeff_modulus();
    int N2 = parms.poly_modulus_degree();
    Ciphertext tempctxt;
    Ciphertext tempctxt_rotated;
    Ciphertext tempctxt_shifted;
    vector<Ciphertext> temp;
    Ciphertext tmp;
    temp.push_back(packed_query);
    int numIters = ceil(log2(size));   // size is a ---rlwe---power of 2.
    if (numIters > ceil(log2(N2))) {
        throw logic_error("m > coeff_count is not allowed.");
    }
    int startIndex = static_cast<int>(log2(N2) - numIters);
    for (long i = 0; i < numIters; i++) {
        vector<Ciphertext> newtemp(temp.size() << 1);
        int index = startIndex + i;
        int power = (N2 >> index) + 1;//k
        int ai = (1 << index);
        for (int j = 0; j < (1 << i); j++) {
            // tempctxt_rotated = subs(result[j])
            evaluator1.apply_galois(temp[j], power, galkey, tempctxt_rotated);
            // result[j+ 2**i] = result[j] - tempctxt_rotated;
            evaluator1.sub(temp[j], tempctxt_rotated, newtemp[j + (1 << i)]);
            // divide by x^ai = multiply by x^(2N - ai).
            multiply_power_of_X(newtemp[j + (1 << i)], tempctxt_shifted, (N2 << 1) - ai, context);
            newtemp[j + (1 << i)] = tempctxt_shifted;
            evaluator1.add(tempctxt_rotated, temp[j], newtemp[j]);
        }
        temp = newtemp;
    }
    return temp;
}

void multiply_power_of_X(const Ciphertext &encrypted, Ciphertext &destination, uint32_t index, const SEALContext& context) {
    const auto &context_data = context.first_context_data();
    auto &parms = context_data->parms();
    auto coeff_mod_count = parms.coeff_modulus().size();
    auto coeff_count = parms.poly_modulus_degree();
    auto encrypted_count = encrypted.size();


    destination = encrypted;


    for (int i = 0; i < encrypted_count; i++) {
        for (int j = 0; j < coeff_mod_count; j++) {
            seal::util::negacyclic_shift_poly_coeffmod(encrypted.data(i) + (j * coeff_count),
                                                       coeff_count, index,
                                                       parms.coeff_modulus()[j],
                                                       destination.data(i) + (j * coeff_count));
        }
    }

}

void plain_decomposition(Plaintext &pt, const SEALContext &context, uint64_t decomp_size, uint64_t base_bit,
                         vector<uint64_t *> &plain_decom) {
    auto context_data = context.first_context_data();
    auto parms = context_data->parms();
    const auto& coeff_modulus = parms.coeff_modulus();
    size_t coeff_modulus_size = coeff_modulus.size();
    size_t coeff_count = parms.poly_modulus_degree();
    auto plain_modulus = parms.plain_modulus();
    const uint64_t base = UINT64_C(1) << base_bit;
    const uint64_t mask = base - 1;
    uint64_t r_l = decomp_size;
    std::uint64_t *res;
    int total_bits = (plain_modulus.bit_count()); // in normal rlwe decomp we use total bits of q, here total bits of t is required
    uint64_t *raw_ptr = pt.data();
    for (int p = 0; p < r_l; p++) {
        vector<uint64_t *> results;
        res = (std::uint64_t *) calloc((coeff_count * coeff_modulus_size), sizeof(uint64_t)); //we are allocating larger space to cater for ct modulus later
        int shift_amount = ((total_bits) - ((p + 1) * (int) base_bit));
        for (size_t k = 0; k < coeff_count; k++) {
            auto ptr(seal::util::allocate_uint(2, MemoryManager::GetPool()));
            auto ptr1(seal::util::allocate_uint(2, MemoryManager::GetPool()));
            ptr[0] = 0;
            ptr[1] = 0;
            ptr1[0] = raw_ptr[k];
            ptr1[1] = 0;
            seal::util::right_shift_uint128(ptr1.get(), shift_amount, ptr.get());
            uint64_t temp1 = ptr[0] & mask;
            res[k * coeff_modulus_size] = temp1;
        }
        plain_decom.push_back(res);
    }
    int ssize = (int) plain_decom.size();
    for (int i = 0; i < ssize; i++) {
        poc_decompose_array(plain_decom[i], coeff_count, coeff_modulus, coeff_modulus_size);
    }
}

void poc_decompose_array(uint64_t *value, size_t count, std::vector<Modulus> coeff_modulus, size_t coeff_mod_count) {
    if (!value) {
        throw invalid_argument("value cannot be null");
    }

    if (coeff_mod_count > 1) {
        if (!seal::util::product_fits_in(count, coeff_mod_count)) {
            throw logic_error("invalid parameters");
        }
        // Decompose an array of multi-precision integers into an array of arrays, one per each base element
        auto temp_array(seal::util::allocate_uint(count * coeff_mod_count, MemoryManager::GetPool()));
        // Merge the coefficients first
        for (size_t i = 0; i < count; i++) {
            for (size_t j = 0; j < coeff_mod_count; j++) {
                temp_array[j + (i * coeff_mod_count)] = value[j + (i * coeff_mod_count)];
            }
        }
        seal::util::set_zero_uint(count * coeff_mod_count, value);

        for (size_t i = 0; i < count; i++) {
            // Temporary space for 128-bit reductions
            for (size_t j = 0; j < coeff_mod_count; j++) {
                // Reduce in blocks
                uint64_t temp[2]{0, temp_array[(i * coeff_mod_count) + coeff_mod_count - 1]};
                for (size_t k = coeff_mod_count - 1; k--;) {
                    temp[0] = temp_array[(i * coeff_mod_count) + k];
                    temp[1] = seal::util::barrett_reduce_128(temp, coeff_modulus[j]);
                }
                // Save the result modulo i-th base element
                value[(j * count) + i] = temp[1];
            }
        }
    }
}

Ciphertext decomp_mul(vector<Ciphertext> ct_decomp, vector<uint64_t *> pt_decomp, SEALContext context) {
    assert(ct_decomp.size() == 2);
    assert(pt_decomp.size() == 2);
    const auto &context_data = context.first_context_data();
    auto &parms = context_data->parms();
    auto &coeff_modulus = parms.coeff_modulus();
    int poly_modulus_degree = parms.poly_modulus_degree();
    int coeff_modulus_size = coeff_modulus.size();
    Ciphertext dst, product;
    dst.resize(context, 2);
    product.resize(context, 2);
    util::RNSIter res_iter0(dst.data(0), poly_modulus_degree);
    util::RNSIter res_iter1(dst.data(1), poly_modulus_degree);
    util::RNSIter prod_iter0(product.data(0), poly_modulus_degree);
    util::RNSIter prod_iter1(product.data(1), poly_modulus_degree);

    util::RNSIter pt_iter0(pt_decomp[0], poly_modulus_degree);
    util::RNSIter pt_iter1(pt_decomp[1], poly_modulus_degree);

    util::RNSIter ct_iter00(ct_decomp[0].data(0), poly_modulus_degree);
    util::RNSIter ct_iter01(ct_decomp[0].data(1), poly_modulus_degree);
    util::RNSIter ct_iter10(ct_decomp[1].data(0), poly_modulus_degree);
    util::RNSIter ct_iter11(ct_decomp[1].data(1), poly_modulus_degree);

    util::ntt_negacyclic_harvey_lazy(pt_iter0, coeff_modulus_size, context.first_context_data()->small_ntt_tables());
    util::ntt_negacyclic_harvey_lazy(pt_iter1, coeff_modulus_size, context.first_context_data()->small_ntt_tables());
    util::ntt_negacyclic_harvey_lazy(ct_iter00, coeff_modulus_size, context.first_context_data()->small_ntt_tables());
    util::ntt_negacyclic_harvey_lazy(ct_iter01, coeff_modulus_size, context.first_context_data()->small_ntt_tables());
    util::ntt_negacyclic_harvey_lazy(ct_iter10, coeff_modulus_size, context.first_context_data()->small_ntt_tables());
    util::ntt_negacyclic_harvey_lazy(ct_iter11, coeff_modulus_size, context.first_context_data()->small_ntt_tables());

    util::dyadic_product_coeffmod(pt_iter0, ct_iter00, coeff_modulus_size, coeff_modulus, prod_iter0);
    util::dyadic_product_coeffmod(pt_iter0, ct_iter01, coeff_modulus_size, coeff_modulus, prod_iter1);
    util::add_poly_coeffmod(res_iter0, prod_iter0, coeff_modulus_size, coeff_modulus, res_iter0);
    util::add_poly_coeffmod(res_iter1, prod_iter1, coeff_modulus_size, coeff_modulus, res_iter1);

    util::dyadic_product_coeffmod(pt_iter1, ct_iter10, coeff_modulus_size, coeff_modulus, prod_iter0);
    util::dyadic_product_coeffmod(pt_iter1, ct_iter11, coeff_modulus_size, coeff_modulus, prod_iter1);
    util::add_poly_coeffmod(res_iter0, prod_iter0, coeff_modulus_size, coeff_modulus, res_iter0);
    util::add_poly_coeffmod(res_iter1, prod_iter1, coeff_modulus_size, coeff_modulus, res_iter1);
    dst.is_ntt_form() = true;
    return dst;
}