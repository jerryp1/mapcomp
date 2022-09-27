//
// Created by pengliqiang on 2022/7/14.
//

#include "edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeServer.h"
#include "../apsi.h"
#include "../serialize.h"
#include "../utils.h"

using namespace seal;
using namespace std;

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeServer_computeEncryptedPowers(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray relin_keys_bytes, jobject query_list,
        jobjectArray jparent_powers, jintArray jsource_power_index, jint ps_low_power) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(parms);
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
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
    for (int i = 0; i < env->GetArrayLength(jsource_power_index); i++) {
        source_power_index.push_back(index_ptr[i]);
    }
    uint32_t target_power_size = env->GetArrayLength(jparent_powers);
    vector<vector<uint32_t>> parent_powers(target_power_size);
    for (int i = 0; i < target_power_size; i++) {
        parent_powers[i].reserve(2);
        auto rows = (jintArray) env->GetObjectArrayElement(jparent_powers, i);
        jint* cols = env->GetIntArrayElements(rows, JNI_FALSE);
        parent_powers[i].push_back(cols[0]);
        parent_powers[i].push_back(cols[1]);
    }
    vector<Ciphertext> encrypted_powers = compute_encrypted_powers(parms, query, parent_powers, source_power_index, ps_low_power, relin_keys);
    return serialize_ciphertexts(env, encrypted_powers);
}

[[maybe_unused]] JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeServer_computeMatches(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray relin_keys_bytes, jobjectArray database_coeffs,
        jobject query_list, jint ps_low_power) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
    if (!is_metadata_valid_for(relin_keys, context)) {
        env->ThrowNew(exception, "invalid relinearization key for this SEALContext");
    }
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    int ps_high_degree = ps_low_power + 1;
    for (int i = 0; i < plaintexts.size(); i++) {
        if ((i % ps_high_degree) != 0) {
            evaluator.transform_to_ntt_inplace(plaintexts[i], low_powers_parms_id);
        }
    }
    Ciphertext f_evaluated = polynomial_evaluation(parms, query_powers, plaintexts, ps_low_power, relin_keys);
    return serialize_ciphertext(env, f_evaluated);
}

[[maybe_unused]] JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeServer_computeMatchesNaiveMethod(
        JNIEnv *env, jclass, jbyteArray params_bytes, jobjectArray database_coeffs, jobject query_list) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    for (int i = 1; i < plaintexts.size(); i++) {
        evaluator.transform_to_ntt_inplace(plaintexts[i], parms_id);
    }
    Ciphertext f_evaluated = polynomial_evaluation(parms, query_powers, plaintexts);
    return serialize_ciphertext(env, f_evaluated);
}