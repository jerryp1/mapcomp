/*
 * Created by Weiran Liu on 2022/1/5.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * In setTojByteArray and jByteArrayToSet, we need to free the memory (in set) allocated in the interpolation function.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 */

#include "edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly.h"
#include "ntl_zp.h"
#include "ntl_zp_util.h"

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly_nativeInterpolate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jint jnum, jobjectArray jxArray, jobjectArray jyArray) {
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
    // 读取集合x与集合y
    std::vector<uint8_t*> setX;
    jByteArrayToSet(env, jxArray, static_cast<uint64_t>(primeByteLength), setX);
    std::vector<uint8_t*> setY;
    jByteArrayToSet(env, jyArray, static_cast<uint64_t>(primeByteLength), setY);
    // 插值
    std::vector<uint8_t*> polynomial;
    zp_interpolate(primeByteLength, static_cast<uint64_t>(jnum), setX, setY, polynomial);
    freeByteArraySet(setX);
    setX.clear();
    freeByteArraySet(setY);
    setY.clear();
    // 返回结果
    jobjectArray jPolynomial;
    setTojByteArray(env, polynomial, static_cast<uint64_t>(primeByteLength), jnum, jPolynomial);
    freeByteArraySet(polynomial);
    polynomial.clear();

    return jPolynomial;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly_nativeRootInterpolate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jint jnum, jobjectArray jxArray, jbyteArray jy) {
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
    // 读取集合x
    std::vector<uint8_t*> setX;
    jByteArrayToSet(env, jxArray, static_cast<uint64_t>(primeByteLength), setX);
    // 读取y
    jbyte* jyBuffer = (*env).GetByteArrayElements(jy, nullptr);
    uint8_t y[primeByteLength];
    memcpy(y, jyBuffer, primeByteLength);
    reverseBytes(y, primeByteLength);
    (*env).ReleaseByteArrayElements(jy, jyBuffer, 0);
    // 插值
    std::vector<uint8_t*> polynomial;
    zp_root_interpolate(primeByteLength, static_cast<uint64_t>(jnum), setX, y, polynomial);
    freeByteArraySet(setX);
    setX.clear();
    // 返回结果
    jobjectArray jPolynomial;
    setTojByteArray(env, polynomial, static_cast<uint64_t>(primeByteLength), jnum + 1, jPolynomial);
    freeByteArraySet(polynomial);
    polynomial.clear();

    return jPolynomial;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly_nativeSingleEvaluate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jCoeffArray, jbyteArray jx) {
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
    jByteArrayToSet(env, jCoeffArray, static_cast<uint64_t>(primeByteLength), polynomial);
    // 读取x
    jbyte* jxBuffer = (*env).GetByteArrayElements(jx, nullptr);
    uint8_t x[primeByteLength];
    memcpy(x, jxBuffer, primeByteLength);
    reverseBytes(x, primeByteLength);
    (*env).ReleaseByteArrayElements(jx, jxBuffer, 0);
    // 求值
    uint8_t y[primeByteLength];
    zp_evaluate(primeByteLength, polynomial, x, y);
    reverseBytes(y, primeByteLength);
    freeByteArraySet(polynomial);
    polynomial.clear();
    // 返回结果
    jbyteArray jy = (*env).NewByteArray((jsize)primeByteLength);
    (*env).SetByteArrayRegion(jy, 0, primeByteLength, (const jbyte*)y);

    return jy;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly_nativeEvaluate
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
    for (uint64_t index = 0; index < setX.size(); index++) {
        setY[index] = new uint8_t[primeByteLength];
        zp_evaluate(primeByteLength, polynomial, setX[index], setY[index]);
    }
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