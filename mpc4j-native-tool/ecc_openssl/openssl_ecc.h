//
// Created by Weiran Liu on 2022/8/21.
//
#include <openssl/ec.h>
#include <openssl/err.h>
#include "stdc++.h"
#include <openssl/obj_mac.h>

#ifndef MPC4J_NATIVE_TOOL_OPENSSL_ECC_H
#define MPC4J_NATIVE_TOOL_OPENSSL_ECC_H

/**
 * 预计算窗口大小
 */
const size_t WIN_SIZE = 16;
/**
 * 群元素
 */
static EC_GROUP *openssl_ec_group;

/**
 * 返回OpenSSL报告的错误信息。
 *
 * @param condition 是否满足给定的条件。
 */
void CRYPTO_CHECK(bool condition);

/**
 * 初始化椭圆曲线群。
 *
 * @param curve_id 椭圆曲线ID。
 */
void init(int curve_id);

/**
 * 清除椭圆曲线群。
 */
void finalize();

#endif //MPC4J_NATIVE_TOOL_OPENSSL_ECC_H
