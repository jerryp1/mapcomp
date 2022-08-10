//
// Created by pengliqiang on 2022/8/9.
//

#include "edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeParamsChecker.h"
#include "../apsi.h"

JNIEXPORT jboolean JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeParamsChecker_checkSealParams
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