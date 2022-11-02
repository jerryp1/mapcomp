//
// Created by Weiran Liu on 2022/11/2.
//
#include "ntl_zp_util.h"

void zp_byte_array_to_prime(JNIEnv *env, uint8_t* primeByteArray, jbyteArray jprimeByteArray, int primeByteLength) {
    // 读取质数的字节长度，读取质数
    jbyte* jprimeByteBuffer = (*env).GetByteArrayElements(jprimeByteArray, nullptr);
    memcpy(primeByteArray, jprimeByteBuffer, static_cast<size_t>(primeByteLength));
    reverseBytes(primeByteArray, static_cast<uint64_t>(primeByteLength));
    (*env).ReleaseByteArrayElements(jprimeByteArray, jprimeByteBuffer, 0);
}