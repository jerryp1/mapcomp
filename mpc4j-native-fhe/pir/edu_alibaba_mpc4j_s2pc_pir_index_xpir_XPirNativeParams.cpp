//
// Created by pengliqiang on 2022/9/13.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeParams.h"
#include "seal/seal.h"
#include "../index_pir.h"
#include "../serialize.h"

using namespace seal;
using namespace std;

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeParams_genEncryptionParameters(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus_size) {
    EncryptionParameters params = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus_size);
    return serialize_encryption_params(env, params);
}
