//
// Created by pengliqiang on 2022/9/8.
//

#ifndef MPC4J_NATIVE_FHE_UTILS_H
#define MPC4J_NATIVE_FHE_UTILS_H

#include <iomanip>
#include "seal/seal.h"

using namespace seal;
using namespace std;


parms_id_type get_parms_id_for_chain_idx(const SEALContext& seal_context, size_t chain_idx);

EncryptionParameters generate_encryption_parameters(scheme_type type, uint32_t poly_modulus_degree, uint64_t plain_modulus, const vector<Modulus>& coeff_modulus);

GaloisKeys generate_galois_keys(const SEALContext& context, KeyGenerator &keygen);

void poc_expand_flat(vector<vector<Ciphertext>>::iterator &result, vector<Ciphertext> &packed_swap_bits,
                     const SEALContext& context, int size, seal::GaloisKeys &galkey);

vector<Ciphertext> poc_rlwe_expand(Ciphertext packed_query, const SEALContext& context, const seal::GaloisKeys& galkey, uint64_t size);

void multiply_power_of_X(const Ciphertext &encrypted, Ciphertext &destination,
                         uint32_t index, const SEALContext& context);

void poc_decompose_array(uint64_t *value, size_t count, std::vector<Modulus> coeff_modulus, size_t coeff_mod_count);

void plain_decomposition(Plaintext &pt, const SEALContext &context, uint64_t decomp_size, uint64_t base_bit,
                         vector<uint64_t *> &plain_decom);

Ciphertext decomp_mul(vector<Ciphertext> ct_decomp, vector<uint64_t *> pt_decomp, SEALContext context);

#endif //MPC4J_NATIVE_FHE_UTILS_H
