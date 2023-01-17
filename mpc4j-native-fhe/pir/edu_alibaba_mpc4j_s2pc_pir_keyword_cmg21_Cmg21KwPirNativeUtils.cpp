//
// Created by Liqiang Peng on 2022/11/4.
//
#include "edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeUtils.h"
#include "../apsi.h"
#include "../serialize.h"
#include "../utils.h"
#include "../polynomials.h"

using namespace seal;
using namespace std;

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeUtils_genEncryptionParameters(
    JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits) {
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* coeff_ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int32_t> bit_sizes(coeff_ptr, coeff_ptr + coeff_size);
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus,
                                                                CoeffModulus::Create(poly_modulus_degree, std::move(bit_sizes)));
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    PublicKey public_key;
    key_gen.create_public_key(public_key);
    RelinKeys relin_keys;
    key_gen.create_relin_keys(relin_keys);
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray parms_bytes = serialize_encryption_parms(env, parms);
    jbyteArray pk_bytes = serialize_public_key(env, public_key);
    jbyteArray relin_keys_bytes = serialize_relin_keys(env, relin_keys);
    jbyteArray sk_bytes = serialize_secret_key(env, secret_key);
    env->CallBooleanMethod(list_obj, list_add, parms_bytes);
    env->CallBooleanMethod(list_obj, list_add, relin_keys_bytes);
    env->CallBooleanMethod(list_obj, list_add, pk_bytes);
    env->CallBooleanMethod(list_obj, list_add, sk_bytes);
    return list_obj;
}

JNIEXPORT jboolean JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeUtils_checkSealParams(
    JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits,
    jobjectArray jparent_powers, jintArray jsource_power_index, jint ps_low_power, jint max_bin_size) {
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int32_t> bit_sizes(ptr, ptr + coeff_size);
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus,
                                                                CoeffModulus::Create(poly_modulus_degree, std::move(bit_sizes)));
    SEALContext context = SEALContext(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!context.parameters_set()) {
        return env->ThrowNew(exception, "SEAL parameters not valid.");
    }
    if (!context.first_context_data()->qualifiers().using_batching) {
        return env->ThrowNew(exception, "SEAL parameters do not support batching.");
    }
    if (!context.using_keyswitching()) {
        return env->ThrowNew(exception, "SEAL parameters do not support key switching.");
    }
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    RelinKeys relin_keys;
    key_gen.create_relin_keys(relin_keys);
    PublicKey public_key;
    key_gen.create_public_key(public_key);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    encryptor.set_secret_key(secret_key);
    Decryptor decryptor(context, secret_key);
    auto random_factory = UniformRandomGeneratorFactory::DefaultFactory();
    auto random = random_factory->create();
    uint32_t slot_count = encoder.slot_count();
    vector<Plaintext> database(max_bin_size+1);
    for (uint32_t i = 0 ; i <= max_bin_size; i++) {
        vector<uint64_t> database_coeffs(slot_count);
        for (uint32_t j = 0; j < slot_count; j++) {
            database_coeffs[j] = random->generate() % plain_modulus;
        }
        encoder.encode(database_coeffs, database[i]);
    }
    vector<uint64_t> plaintext_coeffs(slot_count);
    jint* index_ptr = env->GetIntArrayElements(jsource_power_index, JNI_FALSE);
    vector<uint32_t> source_power_index;
    source_power_index.reserve(env->GetArrayLength(jsource_power_index));
    for (uint32_t i = 0; i < env->GetArrayLength(jsource_power_index); i++) {
        source_power_index.push_back(index_ptr[i]);
    }
    for (uint32_t i = 0; i < slot_count; i++) {
        plaintext_coeffs[i] = random->generate() % plain_modulus;
    }
    bool is_small_modulus = plain_modulus < (1L << 32);
    vector<Ciphertext> query_powers(source_power_index.size());
    for (uint32_t i = 0; i < source_power_index.size(); i++) {
        Plaintext plaintext_power;
        vector<uint64_t> power_coeffs(slot_count);
        for (uint32_t j = 0; j < slot_count; j++) {
            power_coeffs[i] = mod_exp(plaintext_coeffs[j], source_power_index[i], plain_modulus, is_small_modulus);
        }
        encoder.encode(power_coeffs, plaintext_power);
        encryptor.encrypt_symmetric(plaintext_power, query_powers[i]);
    }
    uint32_t target_power_size = env->GetArrayLength(jparent_powers);
    vector<vector<uint32_t>> parent_powers(target_power_size);
    for (uint32_t i = 0; i < target_power_size; i++) {
        parent_powers[i].reserve(2);
        auto rows = (jintArray) env->GetObjectArrayElement(jparent_powers, i);
        jint* cols = env->GetIntArrayElements(rows, JNI_FALSE);
        parent_powers[i].push_back(cols[0]);
        parent_powers[i].push_back(cols[1]);
    }
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    vector<Ciphertext> encrypted_powers = compute_encrypted_powers(parms, query_powers, parent_powers, source_power_index, ps_low_power, relin_keys);
    encrypted_powers.resize(target_power_size);
    Ciphertext f_evaluated;
    if (ps_low_power > 0) {
        // Paterson-Stockmeyer algorithm
        for (uint32_t i = 0; i < database.size(); i++) {
            if ((i % (ps_low_power + 1)) != 0) {
                evaluator.transform_to_ntt_inplace(database[i], low_powers_parms_id);
            }
        }
        f_evaluated = polynomial_evaluation(parms, encrypted_powers, database, ps_low_power, relin_keys, public_key);
    } else {
        for (uint32_t i = 1; i < database.size(); i++) {
            evaluator.transform_to_ntt_inplace(database[i], high_powers_parms_id);
        }
        f_evaluated = polynomial_evaluation(parms, encrypted_powers, database, public_key);
    }
    return decryptor.invariant_noise_budget(f_evaluated) > 0;
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeUtils_computeEncryptedPowers(
    JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray relin_keys_bytes, jobject query_list,
    jobjectArray jparent_powers, jintArray jsource_power_index, jint ps_low_power) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context = SEALContext(parms);
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!is_metadata_valid_for(relin_keys, context)) {
        env->ThrowNew(exception, "invalid relinearization key for this SEALContext");
    }
    Evaluator evaluator(context);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
    // compute all the powers of the receiver's input.
    jint* index_ptr = env->GetIntArrayElements(jsource_power_index, JNI_FALSE);
    vector<uint32_t> source_power_index;
    source_power_index.reserve(env->GetArrayLength(jsource_power_index));
    for (uint32_t i = 0; i < env->GetArrayLength(jsource_power_index); i++) {
        source_power_index.push_back(index_ptr[i]);
    }
    uint32_t target_power_size = env->GetArrayLength(jparent_powers);
    vector<vector<uint32_t>> parent_powers(target_power_size);
    for (uint32_t i = 0; i < target_power_size; i++) {
        parent_powers[i].reserve(2);
        auto rows = (jintArray) env->GetObjectArrayElement(jparent_powers, (jint) i);
        jint* cols = env->GetIntArrayElements(rows, JNI_FALSE);
        parent_powers[i].push_back(cols[0]);
        parent_powers[i].push_back(cols[1]);
    }
    vector<Ciphertext> encrypted_powers = compute_encrypted_powers(parms, query, parent_powers, source_power_index, ps_low_power, relin_keys);
    return serialize_ciphertexts(env, encrypted_powers);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeUtils_optComputeMatches(
    JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray relin_keys_bytes,
    jobjectArray database_coeffs, jobject query_list, jint ps_low_power) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    if (!is_metadata_valid_for(public_key, context)) {
        env->ThrowNew(exception, "invalid public key for this SEALContext");
    }
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    if (!is_metadata_valid_for(relin_keys, context)) {
        env->ThrowNew(exception, "invalid relinearization key for this SEALContext");
    }
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    uint32_t ps_high_degree = ps_low_power + 1;
    for (uint32_t i = 0; i < plaintexts.size(); i++) {
        if ((i % ps_high_degree) != 0) {
            evaluator.transform_to_ntt_inplace(plaintexts[i], low_powers_parms_id);
        }
    }
    Ciphertext f_evaluated = polynomial_evaluation(parms, query_powers, plaintexts, ps_low_power, relin_keys, public_key);
    return serialize_ciphertext(env, f_evaluated);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeUtils_naiveComputeMatches(
    JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jobjectArray database_coeffs, jobject query_list) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    if (!is_metadata_valid_for(public_key, context)) {
        env->ThrowNew(exception, "invalid public key for this SEALContext");
    }
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    for (uint32_t i = 1; i < plaintexts.size(); i++) {
        evaluator.transform_to_ntt_inplace(plaintexts[i], parms_id);
    }
    Ciphertext f_evaluated = polynomial_evaluation(parms, query_powers, plaintexts, public_key);
    return serialize_ciphertext(env, f_evaluated);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeUtils_generateQuery(
    JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jobjectArray coeffs_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
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
    vector<Ciphertext> query;
    query.reserve(plain_query.size());
    for (auto & i : plain_query) {
        Ciphertext ciphertext;
        encryptor.encrypt_symmetric(i, ciphertext);
        query.push_back(ciphertext);
    }
    return serialize_ciphertexts(env, query);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeUtils_decodeReply(
    JNIEnv *env, jclass, jbyteArray paras_bytes, jbyteArray sk_bytes, jbyteArray response_byte) {
    EncryptionParameters parms = deserialize_encryption_parms(env, paras_bytes);
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
    jlongArray coeffs = env->NewLongArray((jsize) dec_vec.size());
    jlong fill[dec_vec.size()];
    for (uint32_t i = 0; i < dec_vec.size(); i++) {
        fill[i] = (jlong) dec_vec[i];
    }
    env->SetLongArrayRegion(coeffs, 0, (jsize) dec_vec.size(), fill);
    return coeffs;
}