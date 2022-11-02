//
// Created by Weiran Liu on 2022/11/2.
//
#include <jni.h>
#include "defines.h"

#ifndef MPC4J_NATIVE_TOOL_NTL_ZP_UTIL_H
#define MPC4J_NATIVE_TOOL_NTL_ZP_UTIL_H

void zp_byte_array_to_prime(JNIEnv *env, uint8_t* primeByteArray, jbyteArray jprimeByteArray, int primeByteLength);

#endif //MPC4J_NATIVE_TOOL_NTL_ZP_UTIL_H