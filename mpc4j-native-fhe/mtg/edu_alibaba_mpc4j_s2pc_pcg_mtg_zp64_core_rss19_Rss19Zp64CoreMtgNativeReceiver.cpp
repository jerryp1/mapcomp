//
// Created by pengliqiang on 2022/9/7.
//

#include "edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeReceiver.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../utils.h"

using namespace std;
using namespace seal;

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeReceiver_computeResponse(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray cipher1_bytes, jbyteArray cipher2_bytes,
        jlongArray plain1, jlongArray plain2, jlongArray r) {
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context(params);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    vector<Ciphertext> ct;
    ct.resize(2);
    ct[0] = deserialize_ciphertext(env, cipher1_bytes, context);
    ct[1] = deserialize_ciphertext(env, cipher2_bytes, context);
    int size = env->GetArrayLength(plain1);
    long *ptr1 = env->GetLongArrayElements(plain1, JNI_FALSE);
    long *ptr2 = env->GetLongArrayElements(plain2, JNI_FALSE);
    long *ptr_r = env->GetLongArrayElements(r, JNI_FALSE);
    vector<uint64_t> vec4(ptr1, ptr1 + size);
    vector<uint64_t> vec1(ptr1, ptr1 + size) ,vec2(ptr2, ptr2 + size), r_vec(ptr_r, ptr_r + size);
    Plaintext plaintext_a1, plaintext_b1, random_mask;
    encoder.encode(vec1, plaintext_a1);
    encoder.encode(vec2, plaintext_b1);
    encoder.encode(r_vec, random_mask);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    evaluator.transform_to_ntt_inplace(plaintext_a1, parms_id);
    evaluator.transform_to_ntt_inplace(plaintext_b1, parms_id);
    evaluator.multiply_plain_inplace(ct[0], plaintext_b1);
    evaluator.multiply_plain_inplace(ct[1], plaintext_a1);
    // add random mask
    evaluator.add_inplace(ct[0], ct[1]);
    evaluator.transform_from_ntt_inplace(ct[0]);
    Ciphertext ct_d;
    evaluator.add_plain(ct[0], random_mask, ct_d);
    while (ct_d.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(ct_d);
    }
    return serialize_ciphertext(env, ct_d);
}