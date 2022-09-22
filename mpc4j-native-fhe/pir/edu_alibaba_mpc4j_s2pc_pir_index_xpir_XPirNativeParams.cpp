//
// Created by pengliqiang on 2022/9/13.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeParams.h"
#include "seal/seal.h"
#include "../utils.h"
#include "../serialize.h"

using namespace seal;
using namespace std;

[[maybe_unused]] JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeParams_genEncryptionParameters(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus);
    return serialize_encryption_parms(env, parms);
}
