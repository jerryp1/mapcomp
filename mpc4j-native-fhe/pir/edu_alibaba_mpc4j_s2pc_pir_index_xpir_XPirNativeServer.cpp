//
// Created by pengliqiang on 2022/9/13.
//

#include <iomanip>
#include "edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeServer.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../index_pir.h"


using namespace std;
using namespace seal;

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeServer_transformToNttForm(
        JNIEnv *env, jclass, jbyteArray params_bytes, jobject plaintext_list) {
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context(params);
    Evaluator evaluator(context);
    vector<Plaintext> plaintexts = deserialize_plaintext_from_coefficients(env, plaintext_list, context,
                                                                           params.poly_modulus_degree());
    // Transform plaintext to NTT.
    for (auto & plaintext : plaintexts) {
        evaluator.transform_to_ntt_inplace(plaintext, context.first_parms_id());
    }
    return serialize_plaintexts(env, plaintexts);
}

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeServer_generateReply(
        JNIEnv *env, jclass, jbyteArray params_bytes, jobject ciphertext_list_bytes, jobject plaintext_list_bytes,
        jintArray nvec_array) {
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context(params);
    Evaluator evaluator(context);
    auto exception = env->FindClass("java/lang/Exception");
    vector<Plaintext> database = deserialize_plaintexts_from_byte(env, plaintext_list_bytes, context);
    vector<Ciphertext> query = deserialize_ciphertexts(env, ciphertext_list_bytes, context);
    jint *ptr = env->GetIntArrayElements(nvec_array, JNI_FALSE);
    uint32_t d = env->GetArrayLength(nvec_array);
    vector<uint64_t> nvec(ptr, ptr + d);
    vector<vector<Ciphertext>> query_list(d);
    int flag = 0;
    for (int i = 0; i < d; i++) {
        query_list[i].reserve(nvec[i]);
        for (int j = 0; j < nvec[i]; j++) {
            query_list[i].push_back(query[flag++]);
        }
    }
    uint64_t product = 1;
    for (unsigned long long i : nvec) {
        product *= i;
    }
    vector<Plaintext> *cur = &database;
    vector<Plaintext> intermediate_plain; // decompose....
    auto pool = MemoryManager::GetPool();
    uint32_t expansion_ratio = compute_expansion_ratio(params);
    for (uint32_t i = 0; i < nvec.size(); i++) {
#ifdef DEBUG
        cout << "Server: " << i + 1 << "-th recursion level started " << endl;
        cout << "Server: n_i = " << nvec[i] << endl;
#endif
        if (query_list[i].size() != nvec[i]) {
            cout << " size mismatch!!! " << query_list[i].size() << ", " << nvec[i] << endl;
            env->ThrowNew(exception, "size mismatch!");
        }
        // Transform expanded query to NTT, and ...
        for (auto & jj : query_list[i]) {
            evaluator.transform_to_ntt_inplace(jj);
        }
        if (i > 0) {
            for (auto & jj : *cur) {
                evaluator.transform_to_ntt_inplace(jj,context.first_parms_id());
            }
        }
#ifdef DEBUG
        for (uint64_t k = 0; k < product; k++) {
            if ((*cur)[k].is_zero()) {
                cout << k + 1 << "/ " << product << "-th ptxt = 0 " << endl;
            }
        }
#endif
        product /= nvec[i];
        vector<Ciphertext> intermediateCtxts(product);
        Ciphertext temp;
        for (uint64_t k = 0; k < product; k++) {
            evaluator.multiply_plain(query_list[i][0], (*cur)[k],intermediateCtxts[k]);
            for (uint64_t j = 1; j < nvec[i]; j++) {
                evaluator.multiply_plain(query_list[i][j], (*cur)[k + j * product], temp);
                evaluator.add_inplace(intermediateCtxts[k], temp); // Adds to first component.
            }
        }
        for (auto & intermediateCtxt : intermediateCtxts) {
            evaluator.transform_from_ntt_inplace(intermediateCtxt);
        }
        if (i == nvec.size() - 1) {
            return serialize_ciphertexts(env, intermediateCtxts);
        } else {
            intermediate_plain.clear();
            intermediate_plain.reserve(expansion_ratio * product);
            cur = &intermediate_plain;
            for (uint64_t rr = 0; rr < product; rr++) {
                EncryptionParameters parms;
                evaluator.mod_switch_to_inplace(intermediateCtxts[rr],context.last_parms_id());
                parms = context.last_context_data()->parms();
                vector<Plaintext> plains = decompose_to_plaintexts(parms, intermediateCtxts[rr]);
                for (auto & plain : plains) {
                    intermediate_plain.emplace_back(plain);
                }
            }
            product = intermediate_plain.size(); // multiply by expansion rate.
        }
#ifdef DEBUG
        cout << "Server: " << i + 1 << "-th recursion level finished " << endl;
#endif
    }
    // This should never get here
    env->ThrowNew(exception, "generate response failed!");
    return nullptr;
}