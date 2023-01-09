#include "index_pir.h"
#include "seal/seal.h"
#include "tfhe/params.h"

using namespace std;
using namespace seal;

uint32_t compute_expansion_ratio(const EncryptionParameters& parms) {
    uint32_t expansion_ratio = 0;
    auto pt_bits_per_coeff = (uint32_t) log2(parms.plain_modulus().value());
    for (const auto & i : parms.coeff_modulus()) {
        double coeff_bit_size = log2(i.value());
        expansion_ratio += ceil(coeff_bit_size / pt_bits_per_coeff);
    }
    return expansion_ratio;
}

vector<Plaintext> decompose_to_plaintexts(const EncryptionParameters& parms, const Ciphertext &ct) {
    const auto pt_bits_per_coeff = (uint32_t) log2(parms.plain_modulus().value());
    const auto coeff_count = parms.poly_modulus_degree();
    const auto coeff_mod_count = parms.coeff_modulus().size();
    const uint64_t pt_bitmask = (1 << pt_bits_per_coeff) - 1;
    vector<Plaintext> result(compute_expansion_ratio(parms) * ct.size());
    auto pt_iter = result.begin();
    for (size_t poly_index = 0; poly_index < ct.size(); ++poly_index) {
        for (size_t coeff_mod_index = 0; coeff_mod_index < coeff_mod_count; ++coeff_mod_index) {
            const double coeff_bit_size = log2(parms.coeff_modulus()[coeff_mod_index].value());
            const size_t local_expansion_ratio = ceil(coeff_bit_size / pt_bits_per_coeff);
            size_t shift = 0;
            for (size_t i = 0; i < local_expansion_ratio; ++i) {
                pt_iter->resize(coeff_count);
                for (size_t c = 0; c < coeff_count; ++c) {
                    (*pt_iter)[c] = (ct.data(poly_index)[coeff_mod_index * coeff_count + c] >> shift) & pt_bitmask;
                }
                ++pt_iter;
                shift += pt_bits_per_coeff;
            }
        }
    }
    return result;
}

void compose_to_ciphertext(const EncryptionParameters& parms, const vector<Plaintext> &pts, Ciphertext &ct) {
    return compose_to_ciphertext(parms, pts.begin(), pts.size() / compute_expansion_ratio(parms), ct);
}

void compose_to_ciphertext(const EncryptionParameters& parms, vector<Plaintext>::const_iterator pt_iter,
                           const size_t ct_poly_count, Ciphertext &ct) {
    const auto pt_bits_per_coeff = (uint32_t) log2(parms.plain_modulus().value());
    const auto coeff_count = parms.poly_modulus_degree();
    const auto coeff_mod_count = parms.coeff_modulus().size();
    ct.resize(ct_poly_count);
    for (size_t poly_index = 0; poly_index < ct_poly_count; ++poly_index) {
        for (size_t coeff_mod_index = 0; coeff_mod_index < coeff_mod_count; ++coeff_mod_index) {
            const double coeff_bit_size = log2(parms.coeff_modulus()[coeff_mod_index].value());
            const size_t local_expansion_ratio = ceil(coeff_bit_size / pt_bits_per_coeff);
            size_t shift = 0;
            for (size_t i = 0; i < local_expansion_ratio; ++i) {
                for (size_t c = 0; c < pt_iter->coeff_count(); ++c) {
                    if (shift == 0) {
                        ct.data(poly_index)[coeff_mod_index * coeff_count + c] = (*pt_iter)[c];
                    } else {
                        ct.data(poly_index)[coeff_mod_index * coeff_count + c] += ((*pt_iter)[c] << shift);
                    }
                }
                ++pt_iter;
                shift += pt_bits_per_coeff;
            }
        }
    }
}

vector<Ciphertext> generate_first_dimension_query(const EncryptionParameters& parms, Encryptor encryptor, int index,
                                                  int dimension_size) {
    auto pool = MemoryManager::GetPool();
    uint64_t decomp_size = 2;
    int plain_bits = parms.plain_modulus().bit_count();
    int plain_base = (int) (parms.plain_modulus().bit_count() / decomp_size);
    auto ptr0(seal::util::allocate_uint(2, pool));
    auto ptr1(seal::util::allocate_uint(2, pool));
    size_t coeff_count = parms.poly_modulus_degree();
    Plaintext pt(coeff_count);
    pt.set_zero();
    uint64_t h;
    vector<Ciphertext> result(decomp_size);
    for (int i = 0; i < decomp_size; i++) {
        int shift_amount = plain_bits - (i + 1) * plain_base;
        ptr1[0] = 0;
        ptr1[1] = 0;
        ptr0[0] = 1;
        ptr0[1] = 0;
        util::left_shift_uint128(ptr0.get(), shift_amount, ptr1.get());
        h = seal::util::barrett_reduce_128(ptr1.get(), parms.plain_modulus().value());
        if (dimension_size > 0) {
            uint64_t tt;
            util::try_invert_uint_mod(dimension_size, parms.plain_modulus().value(),tt);
            h = util::multiply_uint_mod(h ,tt , parms.plain_modulus());
        }
        pt[index] = h;
        Ciphertext res;
        encryptor.encrypt_symmetric(pt,res);
        result[i] = res;
    }
    return result;
}

Ciphertext generate_remaining_dimension_query(const EncryptionParameters& parms, Encryptor encryptor,
                                              const vector<uint32_t>& indices, int dimension_size) {
    auto pool = MemoryManager::GetPool();
    Plaintext pt;
    pt.set_zero();
    for (unsigned int index : indices) {
        pt[index] = 1;
    }
    uint64_t h;
    auto &coeff_modulus = parms.coeff_modulus();
    Ciphertext res;
    encryptor.encrypt_zero_symmetric(res);
    size_t coeff_count = parms.poly_modulus_degree();
    int logsize = ceil(log2(dimension_size * targetP::l_));
    int gap = ceil(coeff_count / (1 << logsize));
    uint64_t total_dim_with_gap = dimension_size * gap;
    size_t coeff_mod_count = coeff_modulus.size();
    int inv = 1 << logsize;
    for (int p = 0; p < targetP::l_; p++) {
        int shift_amount = (int) (targetP::digits - ((p + 1) * targetP::Bgbit));
        for (size_t i = 0; i < total_dim_with_gap; i++) {
            for (size_t j = 0; j < coeff_mod_count; j++) {
                auto ptr0(seal::util::allocate_uint(coeff_mod_count, pool));
                auto ptr1(seal::util::allocate_uint(coeff_mod_count, pool));
                uint64_t poly_inv;
                uint64_t plain_coeff;
                if (inv > 0) {
                    seal::util::try_invert_uint_mod(inv, coeff_modulus[j], poly_inv);
                    plain_coeff = seal::util::multiply_uint_mod(pt.data()[i], poly_inv, coeff_modulus[j]);
                } else {
                    plain_coeff = pt.data()[i];
                }
                ptr1[0] = 0;
                ptr1[1] = 0;
                ptr0[0] = 1;
                ptr0[1] = 0;
                util::left_shift_uint128(ptr0.get(), shift_amount, ptr1.get());
                h = seal::util::barrett_reduce_128(ptr1.get(), coeff_modulus[j]);
                h = seal::util::multiply_uint_mod(h, plain_coeff, coeff_modulus[j]);
                uint64_t index = i + p * total_dim_with_gap + j * coeff_count;
                res.data(0)[index] = seal::util::add_uint_mod(res.data(0)[index], h, coeff_modulus[j]);
            }
        }
    }
    return res;
}