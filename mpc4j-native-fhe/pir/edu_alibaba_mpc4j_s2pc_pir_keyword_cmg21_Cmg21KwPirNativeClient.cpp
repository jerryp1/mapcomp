//
// Created by pengliqiang on 2022/7/14.
//

#include "edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeClient.h"
#include "seal/seal.h"
#include "../apsi.h"
#include "../utils.h"
#include "../serialize.h"

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeClient_genEncryptionParameters(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits) {
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* coeff_ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int> bit_sizes(coeff_ptr, coeff_ptr + coeff_size);
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus, bit_sizes);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable relin_keys = key_gen.create_relin_keys();
    Serializable public_key = key_gen.create_public_key();
    return serialize_relin_public_secret_keys(env, parms, relin_keys, public_key, secret_key);
}

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeClient_generateQuery(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray pk_bytes,jbyteArray sk_bytes, jobjectArray coeffs_array) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    if (!is_metadata_valid_for(public_key, context) || !is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid public key or secret key for this SEALContext");
    }
    vector<Plaintext> plain_query = deserialize_plaintexts_from_coeff(env, coeffs_array, context);
    for (auto & plaintext : plain_query) {
        if (!is_metadata_valid_for(plaintext, context)) {
            env->ThrowNew(exception, "invalid plaintext for this SEALContext");
        }
    }
    BatchEncoder encoder(context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    vector<Serializable<Ciphertext>> query;
    query.reserve(plain_query.size());
    for (auto & i : plain_query) {
        Serializable ciphertext = encryptor.encrypt_symmetric(i);
        query.push_back(ciphertext);
    }
    return serialize_ciphertexts(env, query);
}

[[maybe_unused]] JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeClient_decodeReply(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray sk_bytes, jbyteArray response_byte) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext");
    }
    BatchEncoder encoder(context);
    Decryptor decryptor(context, secret_key);
    uint32_t slot_count = encoder.slot_count();
    vector<uint64_t> result;
    Ciphertext response = deserialize_ciphertext(env, response_byte, context);
    if (!is_metadata_valid_for(response, context)) {
        env->ThrowNew(exception, "invalid ciphertext for this SEALContext");
    }
    Plaintext decrypted;
    vector<uint64_t> dec_vec(slot_count);
    decryptor.decrypt(response, decrypted);
    encoder.decode(decrypted, dec_vec);
    jlongArray coeffs;
    coeffs = env->NewLongArray((jsize) dec_vec.size());
    jlong fill[dec_vec.size()];
    for (int i = 0; i < dec_vec.size(); i++) {
        fill[i] = (jlong) dec_vec[i];
    }
    env->SetLongArrayRegion(coeffs, 0, (jsize) dec_vec.size(), fill);
    return coeffs;
}