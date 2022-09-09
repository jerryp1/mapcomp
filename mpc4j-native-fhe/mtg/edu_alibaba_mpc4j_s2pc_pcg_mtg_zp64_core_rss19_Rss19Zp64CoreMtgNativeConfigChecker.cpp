//
// Created by pengliqiang on 2022/9/7.
//

#include "edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeConfigChecker.h"
#include "seal/seal.h"
#include "../utils.h"

using namespace std;
using namespace seal;

[[maybe_unused]] JNIEXPORT jlong JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeConfigChecker_checkCreatePlainModulus(
        JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size) {
    EncryptionParameters parms(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree, sec_level_type::tc128));
    uint64_t plain_modulus;
    jclass Exception = env->FindClass("java/lang/Exception");
    try {
        plain_modulus = PlainModulus::Batching(poly_modulus_degree, plain_modulus_size).value();
    } catch (...) {
        return env->ThrowNew(Exception, "Failed to find enough qualifying primes.");
    }
    parms.set_plain_modulus(plain_modulus);
    SEALContext context(parms, true);
    if (!context.parameters_set()) {
        // throw invalid_argument("SEAL parameters not valid.");
        return env->ThrowNew(Exception, "SEAL parameters not valid.");
    }
    if (!context.first_context_data()->qualifiers().using_batching) {
        // throw invalid_argument("SEAL parameters do not support batching.");
        return env->ThrowNew(Exception, "SEAL parameters do not support batching.");
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
    // 随机生成系数明文a0
    vector<uint64_t> a0(slot_count);
    for (int j = 0; j < slot_count; j++) {
        a0[j] = random_uint64() % plain_modulus;
    }
    // 随机生成系数明文a1
    vector<uint64_t> a1(slot_count);
    for (int j = 0; j < slot_count; j++) {
        a1[j] = random_uint64() % plain_modulus;
    }
    // 随机生成系数明文b0
    vector<uint64_t> b0(slot_count);
    for (int j = 0; j < slot_count; j++) {
        b0[j] = random_uint64() % plain_modulus;
    }
    // 随机生成系数明文b1
    vector<uint64_t> b1(slot_count);
    for (int j = 0; j < slot_count; j++) {
        b1[j] = random_uint64() % plain_modulus;
    }
    // 随机生成系数明文r
    vector<uint64_t> r(slot_count);
    for (int j = 0; j < slot_count; j++) {
        r[j] = random_uint64() % plain_modulus;
    }
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    // server phase 1
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

    // client phase 1
    Plaintext plaintext_a1, plaintext_b1, plaintext_r;
    encoder.encode(a1, plaintext_a1);
    encoder.encode(b1, plaintext_b1);
    encoder.encode(r, plaintext_r);

    evaluator.transform_to_ntt_inplace(plaintext_a1, parms_id);
    evaluator.transform_to_ntt_inplace(plaintext_b1, parms_id);

    evaluator.multiply_plain_inplace(ct[0], plaintext_b1);
    evaluator.multiply_plain_inplace(ct[1], plaintext_a1);
    // add random mask
    evaluator.add_inplace(ct[0], ct[1]);
    Ciphertext ct_d;
    evaluator.transform_from_ntt_inplace(ct[0]);
    evaluator.add_plain(ct[0], plaintext_r, ct_d);

    while (ct_d.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(ct_d);
    }

    if (decryptor.invariant_noise_budget(ct_d) <= 0) {
        return env->ThrowNew(Exception, "noise budget is not enough.");
    }
    return (jlong) plain_modulus;
}