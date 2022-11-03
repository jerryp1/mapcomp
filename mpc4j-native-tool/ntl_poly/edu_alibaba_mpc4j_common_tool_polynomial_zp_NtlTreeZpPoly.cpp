//
// Created by Weiran Liu on 2022/11/2.
//
#include "edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly.h"
#include "ntl_tree_zp.h"
#include "ntl_zp_util.h"
#include "ntl_zp.h"

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeRootInterpolate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jint jexpectNum, jobjectArray jxArray, jbyteArray jy) {
    return zp_root_interpolate(env, context, jprimeByteArray, jexpectNum, jxArray, jy);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeTreeInterpolate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jint jexpectNum, jobjectArray jxArray, jobjectArray jyArray) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    uint64_t point_num = (*env).GetArrayLength(jxArray);
    // 读取质数
    uint8_t primeByteArray[primeByteLength];
    zp_byte_array_to_prime(env, primeByteArray, jprimeByteArray, primeByteLength);
    // 设置有限域
    NTL::ZZ prime;
    NTL::ZZFromBytes(prime, primeByteArray, static_cast<long>(primeByteLength));
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取集合x与集合y
    std::vector<uint8_t*> setX;
    jByteArrayToSet(env, jxArray, static_cast<uint64_t>(primeByteLength), setX);
    std::vector<uint8_t*> setY;
    jByteArrayToSet(env, jyArray, static_cast<uint64_t>(primeByteLength), setY);
    // 插值
    std::vector<uint8_t*> polynomial;
    zp_tree_interpolate(primeByteLength, jexpectNum, setX, setY, polynomial);
    freeByteArraySet(setX);
    setX.clear();
    freeByteArraySet(setY);
    setY.clear();
    // 返回结果
    jobjectArray jPolynomial;
    if (point_num == jexpectNum) {
        setTojByteArray(env, polynomial, static_cast<uint64_t>(primeByteLength), jexpectNum + 1, jPolynomial);
    } else {
        setTojByteArray(env, polynomial, static_cast<uint64_t>(primeByteLength), jexpectNum, jPolynomial);
    }

    freeByteArraySet(polynomial);
    polynomial.clear();

    return jPolynomial;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeSingleEvaluate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jCoeffArray, jbyteArray jx) {
    return zp_single_evaluate(env, context, jprimeByteArray, jCoeffArray, jx);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeTreeEvaluate
        (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jPolynomial, jobjectArray jxArray) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    // 读取质数
    uint8_t primeByteArray[primeByteLength];
    zp_byte_array_to_prime(env, primeByteArray, jprimeByteArray, primeByteLength);
    // 设置有限域
    NTL::ZZ prime;
    NTL::ZZFromBytes(prime, primeByteArray, static_cast<long>(primeByteLength));
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取系数
    std::vector<uint8_t *> polynomial;
    jByteArrayToSet(env, jPolynomial, static_cast<uint64_t>(primeByteLength), polynomial);
    // 读取x
    std::vector<uint8_t*> setX;
    jByteArrayToSet(env, jxArray, static_cast<uint64_t>(primeByteLength), setX);
    // 求值
    std::vector<uint8_t*> setY(setX.size());
    zp_tree_evaluate(primeByteLength, polynomial, setX, setY);

    freeByteArraySet(polynomial);
    polynomial.clear();
    freeByteArraySet(setX);
    setX.clear();
    // 返回结果
    jobjectArray jyArray;
    setTojByteArray(env, setY, static_cast<uint64_t>(primeByteLength), static_cast<jint>(setY.size()), jyArray);
    freeByteArraySet(setY);
    setY.clear();

    return jyArray;
}

