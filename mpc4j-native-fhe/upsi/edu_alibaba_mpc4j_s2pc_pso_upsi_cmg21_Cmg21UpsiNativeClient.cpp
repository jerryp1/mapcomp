//
// Created by Liqiang Peng on 2022/7/14.
//
#include "edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeClient.h"
#include "../apsi.h"

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeClient_genEncryptionParameters(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits) {
    return genEncryptionParameters(env, poly_modulus_degree, plain_modulus, coeff_modulus_bits);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeClient_generateQuery(
        JNIEnv *env, jclass, jobjectArray jenc_arrays, jbyteArray params_bytes, jbyteArray pk_bytes,
        jbyteArray sk_bytes) {
    return generateQuery(env, jenc_arrays, params_bytes, pk_bytes, sk_bytes);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeClient_decodeReply(
        JNIEnv *env, jclass, jbyteArray response, jbyteArray params_bytes, jbyteArray sk_bytes) {
    return decodeReply(env, response, params_bytes, sk_bytes);
}