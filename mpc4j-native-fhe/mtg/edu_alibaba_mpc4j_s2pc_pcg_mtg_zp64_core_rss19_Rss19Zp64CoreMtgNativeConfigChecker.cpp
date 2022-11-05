//
// Created by Liqiang Peng on 2022/9/7.
//

#include "edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeConfigChecker.h"
#include "seal/seal.h"
#include "../utils.h"

using namespace std;
using namespace seal;

JNIEXPORT jlong JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeConfigChecker_checkCreatePlainModulus(
        JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size) {
    uint64_t plain_modulus;
    jclass exception = env->FindClass("java/lang/Exception");
    try {
        plain_modulus = PlainModulus::Batching(poly_modulus_degree, plain_modulus_size).value();
    } catch (...) {
        return env->ThrowNew(exception, "Failed to find enough qualifying primes.");
    }
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus);
    SEALContext context(parms, true);
    if (!context.parameters_set()) {
        return env->ThrowNew(exception, "SEAL parameters not valid.");
    }
    if (!context.first_context_data()->qualifiers().using_batching) {
        return env->ThrowNew(exception, "SEAL parameters do not support batching.");
    }
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    PublicKey public_key;
    key_gen.create_public_key(public_key);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    encryptor.set_secret_key(secret_key);
    Decryptor decryptor(context, secret_key);
    size_t slot_count = encoder.slot_count();
    vector<uint64_t> coeffs(slot_count);
    vector<uint64_t> a0(slot_count), a1(slot_count), b0(slot_count), b1(slot_count), r(slot_count);
    for (int j = 0; j < slot_count; j++) {
        a0[j] = random_uint64() % plain_modulus;
        a1[j] = random_uint64() % plain_modulus;
        b0[j] = random_uint64() % plain_modulus;
        b1[j] = random_uint64() % plain_modulus;
        r[j] = random_uint64() % plain_modulus;
    }
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    vector<Ciphertext> ct(2);
    Plaintext plaintext_a0, plaintext_b0;
    encoder.encode(a0, plaintext_a0);
    encoder.encode(b0, plaintext_b0);
    encryptor.encrypt_symmetric(plaintext_a0, ct[0]);
    encryptor.encrypt_symmetric(plaintext_b0, ct[1]);
    for (auto & i : ct) {
        // Only one ciphertext-plaintext multiplication is needed after this
        evaluator.mod_switch_to_inplace(i, parms_id);
        // All ciphertexts must be in NTT form
        evaluator.transform_to_ntt_inplace(i);
    }
    Plaintext plaintext_a1, plaintext_b1, plaintext_r;
    encoder.encode(a1, plaintext_a1);
    encoder.encode(b1, plaintext_b1);
    encoder.encode(r, plaintext_r);
    evaluator.transform_to_ntt_inplace(plaintext_a1, parms_id);
    evaluator.transform_to_ntt_inplace(plaintext_b1, parms_id);
    evaluator.multiply_plain_inplace(ct[0], plaintext_b1);
    evaluator.multiply_plain_inplace(ct[1], plaintext_a1);
    evaluator.add_inplace(ct[0], ct[1]);
    Ciphertext ct_d;
    evaluator.transform_from_ntt_inplace(ct[0]);
    evaluator.add_plain(ct[0], plaintext_r, ct_d);
    while (ct_d.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(ct_d);
    }
    if (decryptor.invariant_noise_budget(ct_d) <= 0) {
        return env->ThrowNew(exception, "Noise budget is not enough.");
    }
    return (jlong) plain_modulus;
}