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


}
