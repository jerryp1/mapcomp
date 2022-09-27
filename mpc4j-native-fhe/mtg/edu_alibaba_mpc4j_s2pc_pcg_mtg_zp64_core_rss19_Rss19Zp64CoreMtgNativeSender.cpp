//
// Created by pengliqiang on 2022/9/7.
//

#include "edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeSender.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../utils.h"

using namespace std;
using namespace seal;

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeSender_keyGen(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable public_key = key_gen.create_public_key();
    return serialize_public_key_secret_key(env, parms, public_key, secret_key);
}

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeSender_encryption(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray public_key_bytes, jbyteArray secret_key_bytes,
        jlongArray coeff_array0, jlongArray coeff_array1) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, public_key_bytes, context);
    if (!is_metadata_valid_for(public_key, context)) {
        env->ThrowNew(exception, "invalid public key for this SEALContext!");
    }
    SecretKey secret_key = deserialize_secret_key(env, secret_key_bytes, context);
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext!");
    }
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    int size = env->GetArrayLength(coeff_array0);
    long *ptr0 = env->GetLongArrayElements(coeff_array0, JNI_FALSE);
    long *ptr1 = env->GetLongArrayElements(coeff_array1, JNI_FALSE);
    vector<uint64_t> vec0(ptr0, ptr0 + size), vec1(ptr1, ptr1 + size);
    Plaintext plaintext0, plaintext1;
    encoder.encode(vec0, plaintext0);
    encoder.encode(vec1, plaintext1);
    vector<Ciphertext> ct(2);
    encryptor.encrypt_symmetric(plaintext0, ct[0]);
    encryptor.encrypt_symmetric(plaintext1, ct[1]);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    for (auto & i : ct) {
        // Only one ciphertext-plaintext multiplication is needed after this
        evaluator.mod_switch_to_inplace(i, parms_id);
        // All ciphertexts must be in NTT form
        evaluator.transform_to_ntt_inplace(i);
    }
    return serialize_ciphertexts(env, ct);
}

[[maybe_unused]] JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeSender_decryption(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray secret_key_bytes, jbyteArray ciphertext_bytes) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    SecretKey secret_key = deserialize_secret_key(env, secret_key_bytes, context);
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext!");
    }
    Ciphertext ciphertext = deserialize_ciphertext(env, ciphertext_bytes, context);
    if (!is_metadata_valid_for(ciphertext, context)) {
        env->ThrowNew(exception, "invalid ciphertext for this SEALContext!");
    }
    Decryptor decryptor(context, secret_key);
    Plaintext plaintext;
    decryptor.decrypt(ciphertext, plaintext);
    BatchEncoder encoder(context);
    vector<uint64_t> coeffs;
    coeffs.resize(parms.poly_modulus_degree());
    encoder.decode(plaintext, coeffs);
    jlongArray result;
    result = env->NewLongArray((jsize) parms.poly_modulus_degree());
    jlong temp[parms.poly_modulus_degree()];
    for (int i = 0; i < parms.poly_modulus_degree(); i++) {
        temp[i] = (jlong) coeffs[i];
    }
    env->SetLongArrayRegion(result, 0, (jsize) parms.poly_modulus_degree(), temp);
    return result;
}