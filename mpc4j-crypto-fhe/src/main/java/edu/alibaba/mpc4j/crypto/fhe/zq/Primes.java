package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.math.BigInteger;

/**
 * @author Qixian Zhou
 * @date 2023/7/27
 */
public class Primes {


    /**
     *  Judging whether the given modulus p supports the optimization algorithm,
     *  the judging method refers to paper: <https://hal.archives-ouvertes.fr/hal-01242273/document>.
     *  Equation (1) in Page 7.
     * @param p modulus
     * @return
     */
    public static boolean supportsOptimize(long p) {
        if (Long.numberOfLeadingZeros(p) < 1) {
            return false;
        }
        // Let's multiply the inequality by (2^s0+1)*2^(3s0):
        // we want to output true when
        //    (2^(3s0)+1) * 2^64 < 2^(3s0) * (2^s0+1) * p
        int s0 = Long.numberOfLeadingZeros(p);
        BigInteger middle = BigInteger.ONE.setBit(3 * s0); // 2^{3*s0}
        BigInteger left = middle.add(BigInteger.ONE).shiftLeft(64); // (2^(3s0)+1) * 2^64
        // 2^{3*s0} * (2^s0 + 1 )
        middle = middle.multiply(  BigInteger.ONE.setBit(s0).add(BigInteger.ONE) );
        middle = middle.multiply(BigInteger.valueOf(p));
        //  x.compareTo(y) --> 1 --> x > y
        //                     0 ----> x = y
        //                     -1 ---> x < y
        return left.compareTo(middle) < 0;
    }


    public static boolean isPrime(long a) {
        if (a == 2) {
            return true;
        }
        return BigInteger.valueOf(a).isProbablePrime(CommonConstants.STATS_BIT_LENGTH);
    }


}
