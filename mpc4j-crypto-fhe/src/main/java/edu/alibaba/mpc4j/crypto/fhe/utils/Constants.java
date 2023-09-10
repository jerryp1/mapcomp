package edu.alibaba.mpc4j.crypto.fhe.utils;

/**
 * @author Qixian Zhou
 * @date 2023/8/3
 */
public class Constants {

    public static final int UINT64_BITS = 64;

    public static final int MOD_BIT_COUNT_MAX = 61;
    public static final int MOD_BIT_COUNT_MIN = 2;

    public static final int MULTIPLY_ACCUMULATE_MOD_MAX = (1 << (128 - (MOD_BIT_COUNT_MAX << 1)));

    // Bounds for number of coefficient moduli (no hard requirement)
    public static final int COEFF_MOD_COUNT_MIN = 1;
    public static final int COEFF_MOD_COUNT_MAX = 64;
    // Bounds for polynomial modulus degree (no hard requirement)
    public static final int POLY_MOD_DEGREE_MAX = 131072;
    public static final int POLY_MOD_DEGREE_MIN = 2;
    // Bit-length of internally used coefficient moduli, e.g., auxiliary base in BFV
    public static final int INTERNAL_MOD_BIT_COUNT = 61;

    // Bounds for bit-length of user-defined coefficient moduli
    public static final int USER_MOD_BIT_COUNT_MAX = 60;
    public static final int USER_MOD_BIT_COUNT_MIN = 2;



}
