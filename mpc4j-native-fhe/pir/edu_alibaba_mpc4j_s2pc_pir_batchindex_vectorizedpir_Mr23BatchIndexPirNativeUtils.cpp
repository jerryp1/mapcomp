//
// Created by pengliqiang on 2023/3/10.
//

#include "edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../utils.h"

using namespace std;
using namespace seal;

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateSealContext(
        JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size, jintArray coeff_modulus_bits) {
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* coeff_ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int32_t> bit_sizes(coeff_ptr, coeff_ptr + coeff_size);
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(PlainModulus::Batching(poly_modulus_degree, plain_modulus_size));
    parms.set_coeff_modulus(CoeffModulus::Create(poly_modulus_degree, bit_sizes));
    //parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree, sec_level_type::tc128));
    SEALContext context = SEALContext(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!context.parameters_set()) {
        env->ThrowNew(exception, "SEAL parameters not valid.");
        return nullptr;
    }
    if (!context.first_context_data()->qualifiers().using_batching) {
        env->ThrowNew(exception, "SEAL parameters do not support batching.");
        return nullptr;
    }
    if (!context.using_keyswitching()) {
        env->ThrowNew(exception, "SEAL parameters do not support key switching.");
        return nullptr;
    }
    return serialize_encryption_parms(env, parms);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jint dimension_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    Serializable<RelinKeys> relin_keys = key_gen.create_relin_keys();
    vector<int32_t> steps;
    auto g = (int32_t) ((parms.poly_modulus_degree() / 2) / dimension_size);
    for (int32_t i = 0; i < dimension_size; i++){
        steps.push_back(- i * g);
    }
    Serializable<GaloisKeys> galois_keys = key_gen.create_galois_keys(steps);
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray galois_keys_bytes = serialize_galois_keys(env, galois_keys);
    jbyteArray relin_keys_bytes = serialize_relin_keys(env, relin_keys);
    jbyteArray pk_bytes = serialize_public_key(env, public_key);
    jbyteArray sk_bytes = serialize_secret_key(env, secret_key);
    env->CallBooleanMethod(list_obj, list_add, pk_bytes);
    env->CallBooleanMethod(list_obj, list_add, sk_bytes);
    env->CallBooleanMethod(list_obj, list_add, relin_keys_bytes);
    env->CallBooleanMethod(list_obj, list_add, galois_keys_bytes);
    return list_obj;
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_preprocessDatabase(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobjectArray coeffs_list, jint total_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    BatchEncoder batch_encoder(context);
    Evaluator evaluator(context);
    auto pid = context.first_parms_id();
    vector<Plaintext> encoded_db = deserialize_plaintexts(env, coeffs_list, context);
    for (auto & i : encoded_db){
        evaluator.transform_to_ntt_inplace(i, pid);
    }
    uint32_t current_size = encoded_db.size();
    Plaintext zero(parms.poly_modulus_degree());
    vector<uint64_t> vec(parms.poly_modulus_degree(), 0ULL);
    batch_encoder.encode(vec, zero);
    evaluator.transform_to_ntt_inplace(zero, pid);
    for (uint32_t i = current_size; i < total_size; i++) {
        encoded_db.push_back(zero);
    }
    return serialize_plaintexts(env, encoded_db);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jobjectArray query_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, query_array, context);
    vector<Serializable<Ciphertext>> query;
    for (auto & plaintext : plaintexts) {
        query.push_back(encryptor.encrypt_symmetric(plaintext));
    }
    return serialize_ciphertexts(env, query);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_rotateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray galois_keys_bytes, jbyteArray query_bytes, jint first_two_dimension_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    GaloisKeys galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    Ciphertext query = deserialize_ciphertext(env, query_bytes, context);
    BatchEncoder batch_encoder(context);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    auto g = (int32_t) ((degree / 2) / first_two_dimension_size);
    vector<Ciphertext> rotated_ciphertexts(first_two_dimension_size);
    for (int32_t i = 0; i < first_two_dimension_size; i++) {
        evaluator.rotate_rows(query, - (i * g), galois_keys, rotated_ciphertexts[i]);
        evaluator.transform_to_ntt_inplace(rotated_ciphertexts[i]);
    }
    return serialize_ciphertexts(env, rotated_ciphertexts);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject query_list, jobject rotate_query_list, jobject db_list, jbyteArray pk_bytes,
        jbyteArray relin_keys_bytes, jbyteArray galois_keys_bytes, jint first_two_dimension_size, jint third_dimension_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    GaloisKeys galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    Encryptor encryptor(context, public_key);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
    vector<Plaintext> encoded_db = deserialize_plaintexts(env, db_list, context);
    BatchEncoder batch_encoder(context);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    auto g = (int32_t) ((degree / 2) / first_two_dimension_size);
    vector<Ciphertext> rotated_ciphertexts = deserialize_ciphertexts(env, rotate_query_list, context);
    auto low_parms_id = get_parms_id_for_chain_idx(context, 1);
    // first dimension
    auto time_start = std::chrono::high_resolution_clock::now();
    Ciphertext zero;
    encryptor.encrypt_zero(zero);
    evaluator.transform_to_ntt_inplace(zero);
    vector<Ciphertext> db_prime(third_dimension_size);
    for (int i = 0; i < third_dimension_size; i++) {
        db_prime[i] = zero;
        for (int j = 0; j < first_two_dimension_size; j++) {
            if (encoded_db[i * first_two_dimension_size + j].is_zero()) {
                break;
            } else {
                Ciphertext temp;
                evaluator.multiply_plain(rotated_ciphertexts[j], encoded_db[i * first_two_dimension_size + j], temp);
                evaluator.add_inplace(db_prime[i], temp);
            }
        }
        evaluator.transform_from_ntt_inplace(db_prime[i]);
    }
    auto time_end = std::chrono::high_resolution_clock::now();
    auto preprocess_time = (std::chrono::duration_cast<std::chrono::microseconds>(time_end - time_start)).count();
    cerr << "first dimension : " << preprocess_time << "us" << endl;

    time_start = std::chrono::high_resolution_clock::now();
    // second dimension
    Ciphertext second_dimension_cipher;
    encryptor.encrypt_zero(second_dimension_cipher);
    for (int k = 0; k < third_dimension_size; k++) {
        Ciphertext t;
        evaluator.multiply(query[1], db_prime[k], t);
        evaluator.relinearize_inplace(t, relin_keys);
        evaluator.rotate_rows_inplace(t, -k * g, galois_keys);
        evaluator.add_inplace(second_dimension_cipher, t);
    }
    //evaluator.mod_switch_to_next_inplace(second_dimension_cipher);
    time_end = std::chrono::high_resolution_clock::now();
    preprocess_time = (std::chrono::duration_cast<std::chrono::microseconds>(time_end - time_start)).count();
    cerr << "second dimension : " << preprocess_time << "us" << endl;

    // third dimension
    time_start = std::chrono::high_resolution_clock::now();
    evaluator.mod_switch_to_inplace(query[2], second_dimension_cipher.parms_id());
    evaluator.multiply_inplace(second_dimension_cipher, query[2]);
    evaluator.relinearize_inplace(second_dimension_cipher, relin_keys);
    while (second_dimension_cipher.parms_id() != low_parms_id) {
        evaluator.mod_switch_to_next_inplace(second_dimension_cipher);
    }
    time_end = std::chrono::high_resolution_clock::now();
    preprocess_time = (std::chrono::duration_cast<std::chrono::microseconds>(time_end - time_start)).count();
    cerr << "third dimension : " << preprocess_time << "us" << endl;
    return serialize_ciphertext(env, second_dimension_cipher);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    Decryptor decryptor(context, secret_key);
    BatchEncoder batch_encoder(context);
    Plaintext pt;
    decryptor.decrypt(response, pt);
    vector<uint64_t> vec;
    batch_encoder.decode(pt, vec);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    jlongArray jarr = env->NewLongArray((jsize) degree);
    jlong *arr = env->GetLongArrayElements(jarr, JNI_FALSE);
    for(uint32_t i = 0; i < degree; i++){
        arr[i] = (jlong) vec[i];
    }
    env->ReleaseLongArrayElements(jarr, arr, 0);
    return jarr;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_mergeResponse(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray galois_keys_bytes,
        jobject response_list, jint g) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    GaloisKeys galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    Encryptor encryptor(context, public_key);
    BatchEncoder batch_encoder(context);
    Ciphertext merged_response, r_cipher;
    encryptor.encrypt_zero(merged_response);
    vector<Ciphertext> response = deserialize_ciphertexts(env, response_list, context);
    evaluator.mod_switch_to_inplace(merged_response, response[0].parms_id());
    uint32_t row_size = parms.poly_modulus_degree() / 2;
    auto count = (uint32_t) log2(row_size / g);
    for (uint32_t i = 0; i < response.size(); i++) {
        for (uint32_t j = 0; j < count; j++) {
            evaluator.rotate_rows(response[i], - g * (1 << j), galois_keys, r_cipher);
            evaluator.add_inplace(response[i], r_cipher);
        }
        Plaintext pt(parms.poly_modulus_degree());
        pt.set_zero();
        vector<uint64_t> vec(parms.poly_modulus_degree(), 0ULL);
        uint32_t l = i * g;
        for (uint32_t k = l; k < l + g; k++) {
            vec[k] = 1;
            vec[k + row_size] = 1;
        }
        batch_encoder.encode(vec, pt);
        evaluator.multiply_plain_inplace(response[i], pt);
        evaluator.add_inplace(merged_response, response[i]);
    }
    while (merged_response.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(merged_response);
    }
    try_clear_irrelevant_bits(context.last_context_data()->parms(), merged_response);
    return serialize_ciphertext(env, merged_response);
}