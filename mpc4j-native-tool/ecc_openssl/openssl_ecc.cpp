//
// Created by Weiran Liu on 2022/8/21.
//

#include "openssl_ecc.h"
#include "openssl_window_method.hpp"

void CRYPTO_CHECK(bool condition) {
    if (!condition) {
        char buffer[256];
        ERR_error_string_n(ERR_get_error(), buffer, sizeof(buffer));
        std::cerr << std::string(buffer);
    }
}

void init(int curve_id) {
    // 初始化椭圆曲线群
    openssl_ec_group = EC_GROUP_new_by_curve_name(curve_id);
    CRYPTO_CHECK(openssl_ec_group != nullptr);
}

void finalize() {
    EC_GROUP_free(openssl_ec_group);
}

int main() {
    init(NID_secp256k1);
    int bitLength = EC_GROUP_order_bits(openssl_ec_group);
    BIGNUM *order = BN_new();
    EC_GROUP_get_order(openssl_ec_group, order, nullptr);
    const EC_POINT *generator = EC_GROUP_get0_generator(openssl_ec_group);
    auto *windowMethod = new WindowMethod(generator, bitLength, 16);
    for (int i = 0; i < 100; i++) {
        BIGNUM *r = BN_new();
        CRYPTO_CHECK(BN_rand_range(r, order));
        EC_POINT *correct = EC_POINT_new(openssl_ec_group);
        EC_POINT_mul(openssl_ec_group, correct, nullptr, generator, r, nullptr);
        EC_POINT *result = EC_POINT_new(openssl_ec_group);
        windowMethod->multiply(result, r);
        if (EC_POINT_cmp(openssl_ec_group, correct, result, nullptr) == 1) {
            char *correct_str = EC_POINT_point2hex(openssl_ec_group, correct, POINT_CONVERSION_UNCOMPRESSED, nullptr);
            char *result_str = EC_POINT_point2hex(openssl_ec_group, result, POINT_CONVERSION_UNCOMPRESSED, nullptr);
            std::cout << i << ": " << correct_str << " " << result_str << std::endl;
            OPENSSL_free(correct_str);
            OPENSSL_free(result_str);
        }
        BN_free(r);
        EC_POINT_free(correct);
        EC_POINT_free(result);
    }
    delete windowMethod;
    finalize();
}
