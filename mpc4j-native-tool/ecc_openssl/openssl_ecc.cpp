//
// Created by Weiran Liu on 2022/8/21.
//

#include "openssl_ecc.h"
#include "openssl_window_method.hpp"

void pointFromString(const std::string &pointString, EC_POINT *point, BN_CTX *ctx) {
    auto *buffer = new char[pointString.length()];
    strcpy(buffer, pointString.c_str());
    EC_POINT_hex2point(openssl_ec_group, buffer, point, ctx);
    delete[] buffer;
}

std::string pointToString(const EC_POINT *point, BN_CTX *ctx) {
    std::stringstream ss;
    ss << EC_POINT_point2hex(openssl_ec_group, point, POINT_CONVERSION_UNCOMPRESSED, ctx);
    return ss.str();
}

void bnFromString(const std::string &bnString, BIGNUM *bn) {
    BN_hex2bn(&bn, bnString.c_str());
}

void CRYPTO_CHECK(bool condition) {
    if (!condition) {
        char buffer[256];
        ERR_error_string_n(ERR_get_error(), buffer, sizeof(buffer));
        std::cerr << std::string(buffer);
    }
}

void openssl_init(int curveType) {
    openssl_ec_group = EC_GROUP_new_by_curve_name(curveType);
    CRYPTO_CHECK(openssl_ec_group != nullptr);
    openssl_point_bit_length = EC_GROUP_order_bits(openssl_ec_group);
}

jobject openssl_precompute(JNIEnv *env, jstring jPointString) {
    BN_CTX *ctx = BN_CTX_new();
    // 读取椭圆曲线点
    const char *jPointStringHandler = (*env).GetStringUTFChars(jPointString, JNI_FALSE);
    const std::string pointString = std::string(jPointStringHandler);
    (*env).ReleaseStringUTFChars(jPointString, jPointStringHandler);
    EC_POINT *point = EC_POINT_new(openssl_ec_group);
    pointFromString(pointString, point, ctx);
    // 预计算
    auto *windowHandler = new WindowMethod(point, openssl_point_bit_length, OPENSSL_WIN_SIZE);
    EC_POINT_free(point);
    BN_CTX_free(ctx);
    return (*env).NewDirectByteBuffer(windowHandler, 0);
}

void openssl_destroy_precompute(JNIEnv *env, jobject jWindowHandler) {
    delete (WindowMethod *)(*env).GetDirectBufferAddress(jWindowHandler);
}

jstring openssl_precompute_multiply(JNIEnv *env, jobject jWindowHandler, jstring jBnString) {
    BN_CTX *ctx = BN_CTX_new();
    // 读取幂指数
    const char* jBnStringHandler = (*env).GetStringUTFChars(jBnString, JNI_FALSE);
    std::string bnString = std::string(jBnStringHandler);
    (*env).ReleaseStringUTFChars(jBnString, jBnStringHandler);
    BIGNUM *bn = BN_new();
    bnFromString(bnString, bn);
    // 定点计算
    auto *windowHandler = (WindowMethod *)(*env).GetDirectBufferAddress(jWindowHandler);
    EC_POINT *point = EC_POINT_new(openssl_ec_group);
    (*windowHandler).multiply(point, bn);
    BN_free(bn);
    // 返回结果
    std::string pointString = pointToString(point, ctx);
    EC_POINT_free(point);
    BN_CTX_free(ctx);
    return (*env).NewStringUTF(pointString.data());
}

jstring openssl_multiply(JNIEnv *env, jstring jPointString, jstring jBnString) {
    BN_CTX *ctx = BN_CTX_new();
    // 读取幂指数
    const char* jBnStringHandler = (*env).GetStringUTFChars(jBnString, JNI_FALSE);
    std::string bnString = std::string(jBnStringHandler);
    (*env).ReleaseStringUTFChars(jBnString, jBnStringHandler);
    BIGNUM *bn = BN_new();
    bnFromString(bnString, bn);
    // 读取椭圆曲线点
    const char* jPointStringHandler = (*env).GetStringUTFChars(jPointString, JNI_FALSE);
    const std::string pointString = std::string(jPointStringHandler);
    (*env).ReleaseStringUTFChars(jPointString, jPointStringHandler);
    EC_POINT *point = EC_POINT_new(openssl_ec_group);
    pointFromString(pointString, point, ctx);
    // 计算乘法
    EC_POINT *mulPoint = EC_POINT_new(openssl_ec_group);
    EC_POINT_mul(openssl_ec_group, mulPoint, nullptr, point, bn, ctx);
    BN_free(bn);
    EC_POINT_free(point);
    // 返回结果
    std::string mulPointString = pointToString(mulPoint, ctx);
    EC_POINT_free(mulPoint);
    BN_CTX_free(ctx);
    return (*env).NewStringUTF(mulPointString.data());
}

void openssl_reset() {
    EC_GROUP_free(openssl_ec_group);
}