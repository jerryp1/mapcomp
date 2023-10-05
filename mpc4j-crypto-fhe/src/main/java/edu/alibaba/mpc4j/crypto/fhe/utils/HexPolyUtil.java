package edu.alibaba.mpc4j.crypto.fhe.utils;

/**
 * @author Qixian Zhou
 * @date 2023/9/25
 */
public class HexPolyUtil {


    public static boolean isDecimalChar(char c) {
        return c >= '0' && c <= '9';
    }

    public static int getDecimalValue(char c) {
        return c - '0';
    }




}
