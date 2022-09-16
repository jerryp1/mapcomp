//
// Created by pengliqiang on 2022/9/13.
//

#ifndef MPC4J_NATIVE_FHE_INDEX_PIR_H
#define MPC4J_NATIVE_FHE_INDEX_PIR_H

#include "seal/seal.h"

using namespace std;
using namespace seal;


void compose_to_ciphertext(const EncryptionParameters& params,
                               vector<Plaintext>::const_iterator pt_iter,
                               const size_t ct_poly_count, Ciphertext &ct);

vector<Plaintext> decompose_to_plaintexts(const EncryptionParameters& params,
                                          const Ciphertext &ct);

uint32_t compute_expansion_ratio(const EncryptionParameters& params);

EncryptionParameters generate_encryption_parameters(scheme_type type, uint32_t poly_modulus_degree, uint64_t plain_modulus);

vector<uint64_t> get_coeff_modulus(const EncryptionParameters& params);

void key_generation(const EncryptionParameters& params, PublicKey& public_key, SecretKey& secret_key);

void compose_to_ciphertext(const EncryptionParameters& params,
                           const vector<Plaintext> &pts, Ciphertext &ct);

#endif //MPC4J_NATIVE_FHE_INDEX_PIR_H
