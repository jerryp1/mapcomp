/*
 * @Description: 
 * @Author: Qixian Zhou
 * @Date: 2023-05-28 19:35:10
 */
/*
 * Created by pengliqiang on 2022/9/13.
 */

#ifndef MPC4J_NATIVE_FHE_INDEX_PIR_H
#define MPC4J_NATIVE_FHE_INDEX_PIR_H

#include "seal/seal.h"
#include "tfhe/tfhe.h"
using namespace std;
using namespace seal;


void compose_to_ciphertext(const EncryptionParameters& parms, vector<Plaintext>::const_iterator pt_iter,
                           uint32_t ct_poly_count, Ciphertext &ct);

vector<Plaintext> decompose_to_plaintexts(const EncryptionParameters& parms, const Ciphertext &ct);

uint32_t compute_expansion_ratio(const EncryptionParameters& parms);

void compose_to_ciphertext(const EncryptionParameters& parms, const vector<Plaintext> &pts, Ciphertext &ct);

Ciphertext decomp_mul(vector<Ciphertext> ct_decomp, vector<uint64_t *> pt_decomp, const SEALContext& context);

void poc_expand_flat(vector<vector<Ciphertext>>::iterator &result, vector<Ciphertext> &packed_swap_bits,
                     const SEALContext& context, uint32_t size, seal::GaloisKeys &galois_keys);

vector<Ciphertext> poc_rlwe_expand(const Ciphertext& packed_query, const SEALContext& context, const seal::GaloisKeys& galois_keys, uint32_t size);

void multiply_power_of_X(const Ciphertext &encrypted, Ciphertext &destination,
                         uint32_t index, const SEALContext& context);

void poc_decompose_array(uint64_t *value, uint32_t count, std::vector<Modulus> coeff_modulus, uint32_t coeff_mod_count);

void plain_decomposition(Plaintext &pt, const SEALContext &context, uint32_t decomp_size, uint32_t base_bit,
                         vector<uint64_t *> &plain_decomp);

vector<Ciphertext> expand_query(const EncryptionParameters& parms, const Ciphertext &encrypted,
                                const GaloisKeys& galois_keys, uint32_t m);

uint32_t get_next_power_of_two(uint32_t number);

uint32_t get_number_of_bits(uint64_t number);

Ciphertext get_sum(vector<Ciphertext> &query, Evaluator& evaluator, GaloisKeys &gal_keys, vector<Plaintext> &encoded_db,
                   uint32_t start, uint32_t end);

// For MulPIR, reference: https://github.com/OpenMined/PIR/blob/master/pir/cpp/server.cpp#L148
vector<Ciphertext> new_expand_query(const EncryptionParameters& parms, const std::vector<Ciphertext>& cts, uint32_t total_items,
                                const GaloisKeys& galois_keys);
// For MulPIR, reference: https://github.com/OpenMined/PIR/blob/master/pir/cpp/server.cpp#L106
// ct is Single Ciphertext
vector<Ciphertext> new_single_expand_query(const EncryptionParameters& parms, const Ciphertext& ct, uint32_t num_items,
                                const GaloisKeys& galois_keys);


// For MulPIR, reference:  https://github.com/OpenMined/PIR/blob/master/pir/cpp/database.cpp#L170
vector<Ciphertext> multiply_mulpir( const EncryptionParameters& parms,  
                                    const RelinKeys* const relin_keys, 
                                    const vector<Plaintext>& database,
                                    uint32_t database_it, 
                                    vector<Ciphertext>& selection_vector,
                                    uint32_t selection_vector_it, 
                                    vector<uint32_t>& dimensions, 
                                    uint32_t depth);


#endif //MPC4J_NATIVE_FHE_INDEX_PIR_H
