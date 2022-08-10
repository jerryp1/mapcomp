//
// Created by pengliqiang on 2022/8/9.
//

#include "edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeParamsChecker.h"
#include "../apsi.h"

JNIEXPORT jboolean JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeParamsChecker_checkSealParams
(JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits,
 jobjectArray jparent_powers, jintArray jsource_power_index, jint ps_low_power, jint max_bin_size) {
    int noise_budget = checkSealParams(env, poly_modulus_degree, plain_modulus, coeff_modulus_bits, jparent_powers,
                                       jsource_power_index, ps_low_power, max_bin_size);
    std::cerr << "noise_budget : " << noise_budget << std::endl;
    if (noise_budget > 0) {
        return true;
    } else {
        return false;
    }
}

//jobject JNICALL computeEncryptedPowers(JNIEnv *env, jobject query_list, jbyteArray relin_keys_bytes,
//                                       jbyteArray params_bytes, jobjectArray jparent_powers,
//                                       jintArray jsource_power_index, jint ps_low_power) {
//    // 反序列化
//    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
//    SEALContext context = SEALContext(params);
//    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
//    Evaluator evaluator(context);
//    // 密文查询
//    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
//    // compute all the powers of the receiver's input.
//    jint* index_ptr = env->GetIntArrayElements(jsource_power_index, JNI_FALSE);
//    vector<uint32_t> source_power_index;
//    source_power_index.reserve(env->GetArrayLength(jsource_power_index));
//    for (int i = 0; i < env->GetArrayLength(jsource_power_index); i++) {
//        source_power_index.push_back(index_ptr[i]);
//    }
//    uint32_t target_power_size = env->GetArrayLength(jparent_powers);
//    uint32_t parent_powers[target_power_size][2];
//    for (int i = 0; i < target_power_size; i++) {
//        auto rows = (jintArray) env->GetObjectArrayElement(jparent_powers, i);
//        jint* cols = env->GetIntArrayElements(rows, JNI_FALSE);
//        parent_powers[i][0] = cols[0];
//        parent_powers[i][1] = cols[1];
//    }
//    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
//    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
//    vector<Ciphertext> encrypted_powers;
//    encrypted_powers.resize(target_power_size);
//    if (ps_low_power == 0) {
//        // 根据源密文的阶，对发送方密文排序
//        for (int i = 0; i < query.size(); i++) {
//            encrypted_powers[source_power_index[i] - 1] = query[i];
//        }
//        // 计算指定阶数密文
//        for (int i = 0; i < target_power_size; i++) {
//            if (parent_powers[i][1] != 0) {
//                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
//                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1], encrypted_powers[i]);
//                } else {
//                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1],
//                                       encrypted_powers[parent_powers[i][1] - 1], encrypted_powers[i]);
//                }
//                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
//            }
//        }
//    } else {
//        // Paterson-Stockmeyer algorithm.
//        uint32_t ps_high_degree = ps_low_power + 1;
//        // 根据源密文的阶，对发送方密文排序
//        for (int i = 0; i < query.size(); i++) {
//            if (source_power_index[i] <= ps_low_power) {
//                encrypted_powers[source_power_index[i] - 1] = query[i];
//            } else {
//                encrypted_powers[ps_low_power + (source_power_index[i] / ps_high_degree) - 1] = query[i];
//            }
//        }
//        // 计算指定阶数密文
//        for (int i = 0; i < ps_low_power; i++) {
//            if (parent_powers[i][1] != 0) {
//                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
//                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1], encrypted_powers[i]);
//                } else {
//                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1],
//                                       encrypted_powers[parent_powers[i][1] - 1], encrypted_powers[i]);
//                }
//                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
//            }
//        }
//        for (int i = ps_low_power; i < target_power_size; i++) {
//            if (parent_powers[i][1] != 0) {
//                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
//                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1 + ps_low_power], encrypted_powers[i]);
//                } else {
//                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1 + ps_low_power],
//                                       encrypted_powers[parent_powers[i][1] - 1 + ps_low_power], encrypted_powers[i]);
//                }
//                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
//            }
//        }
//    }
//    return serialize_ciphertexts(env, encrypted_powers);
//}
//
//jbyteArray JNICALL computeMatches(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
//                                  jbyteArray relin_keys_bytes, jbyteArray params_bytes, jint ps_low_power) {
//    // 反序列化
//    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
//    SEALContext context = SEALContext(params);
//    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
//    Evaluator evaluator(context);
//    // encrypted query powers
//    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
//    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
//    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
//    for (int i = 0; i < ps_low_power; i++) {
//        // Low powers must be at a higher level than high powers
//        evaluator.mod_switch_to_inplace(query_powers[i], low_powers_parms_id);
//        // Low powers must be in NTT form
//        evaluator.transform_to_ntt_inplace(query_powers[i]);
//    }
//    for (int i = ps_low_power; i < query_powers.size(); i++) {
//        // High powers are only modulus switched
//        evaluator.mod_switch_to_inplace(query_powers[i], high_powers_parms_id);
//    }
//    // 服务端明文多项式
//    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
//    int ps_high_degree = ps_low_power + 1;
//    for (int i = 0; i < plaintexts.size(); i++) {
//        if ((i % ps_high_degree) != 0) {
//            evaluator.transform_to_ntt_inplace(plaintexts[i], low_powers_parms_id);
//        }
//    }
//    uint32_t degree = plaintexts.size() - 1;
//    Ciphertext f_evaluated, cipher_temp, temp_in;
//    f_evaluated.resize(context, high_powers_parms_id, 3);
//    f_evaluated.is_ntt_form() = false;
//    uint32_t ps_high_degree_powers = degree / ps_high_degree;
//    // Calculate polynomial for i=1,...,ps_high_degree_powers-1
//    for (int i = 1; i < ps_high_degree_powers; i++) {
//        // Evaluate inner polynomial. The free term is left out and added later on.
//        // The evaluation result is stored in temp_in.
//        for (int j = 1; j < ps_high_degree; j++) {
//            evaluator.multiply_plain(query_powers[j - 1], plaintexts[j + i * ps_high_degree], cipher_temp);
//            if (j == 1) {
//                temp_in = cipher_temp;
//            } else {
//                evaluator.add_inplace(temp_in, cipher_temp);
//            }
//        }
//        // Transform inner polynomial to coefficient form
//        evaluator.transform_from_ntt_inplace(temp_in);
//        evaluator.mod_switch_to_inplace(temp_in, high_powers_parms_id);
//        // The high powers are already in coefficient form
//        evaluator.multiply_inplace(temp_in, query_powers[i - 1 + ps_low_power]);
//        evaluator.add_inplace(f_evaluated, temp_in);
//    }
//    // Calculate polynomial for i=ps_high_degree_powers.
//    // Done separately because here the degree of the inner poly is degree % ps_high_degree.
//    // Once again, the free term will only be added later on.
//    if (degree % ps_high_degree > 0 && ps_high_degree_powers > 0) {
//        for (int i = 1; i <= degree % ps_high_degree; i++) {
//            evaluator.multiply_plain(query_powers[i - 1],
//                                     plaintexts[ps_high_degree * ps_high_degree_powers + i],
//                                     cipher_temp);
//            if (i == 1) {
//                temp_in = cipher_temp;
//            } else {
//                evaluator.add_inplace(temp_in, cipher_temp);
//            }
//        }
//        // Transform inner polynomial to coefficient form
//        evaluator.transform_from_ntt_inplace(temp_in);
//        evaluator.mod_switch_to_inplace(temp_in, high_powers_parms_id);
//        // The high powers are already in coefficient form
//        evaluator.multiply_inplace(temp_in, query_powers[ps_high_degree_powers - 1 + ps_low_power]);
//        evaluator.add_inplace(f_evaluated, temp_in);
//    }
//    // Relinearize sum of ciphertext-ciphertext products
//    if (!f_evaluated.is_transparent()) {
//        evaluator.relinearize_inplace(f_evaluated, relin_keys);
//    }
//    // Calculate inner polynomial for i=0.
//    // Done separately since there is no multiplication with a power of high-degree
//    uint32_t length = ps_high_degree_powers == 0 ? degree : ps_low_power;
//    for (size_t j = 1; j <= length; j++) {
//        evaluator.multiply_plain(query_powers[j-1], plaintexts[j], cipher_temp);
//        evaluator.transform_from_ntt_inplace(cipher_temp);
//        evaluator.mod_switch_to_inplace(cipher_temp, high_powers_parms_id);
//        evaluator.add_inplace(f_evaluated, cipher_temp);
//    }
//    // Add the constant coefficients of the inner polynomials multiplied by the respective powers of high-degree
//    for (size_t i = 1; i < ps_high_degree_powers + 1; i++) {
//        evaluator.multiply_plain(query_powers[i - 1 + ps_low_power], plaintexts[ps_high_degree * i], cipher_temp);
//        evaluator.mod_switch_to_inplace(cipher_temp, high_powers_parms_id);
//        evaluator.add_inplace(f_evaluated, cipher_temp);
//    }
//    // Add the constant coefficient
//    evaluator.add_plain_inplace(f_evaluated, plaintexts[0]);
//    while (f_evaluated.parms_id() != context.last_parms_id()) {
//        evaluator.mod_switch_to_next_inplace(f_evaluated);
//    }
//    try_clear_irrelevant_bits(context.last_context_data()->parms(), f_evaluated);
//    return serialize_ciphertext(env, f_evaluated);
//}