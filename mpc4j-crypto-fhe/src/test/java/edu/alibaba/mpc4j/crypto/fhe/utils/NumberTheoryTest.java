package edu.alibaba.mpc4j.crypto.fhe.utils;

/**
 * @author Qixian Zhou
 * @date 2023/7/11
 */


import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;


public class NumberTheoryTest {

    @Test
    public void testModInv() {

        assert NumberTheory.modInv(BigInteger.valueOf(7), BigInteger.valueOf(19)).equals(BigInteger.valueOf(11));
        assert NumberTheory.modInv(BigInteger.valueOf(43), BigInteger.valueOf(103)).equals(BigInteger.valueOf(12));
        assert NumberTheory.modInv(BigInteger.valueOf(94), BigInteger.valueOf(97)).equals(BigInteger.valueOf(32));

    }


    @Test
    public void testModExp() {

        assert NumberTheory.modExp(BigInteger.valueOf(12312), BigInteger.valueOf(53), BigInteger.valueOf(9393333)).equals(BigInteger.valueOf(2678490));
        assert NumberTheory.modExp(BigInteger.valueOf(3880), BigInteger.valueOf(391), BigInteger.valueOf(9000)).equals(BigInteger.valueOf(1000));
        assert NumberTheory.modExp(BigInteger.valueOf(-1), BigInteger.valueOf(432413), BigInteger.valueOf(88)).equals(BigInteger.valueOf(87));

    }


    @Test
    public void testRootOfUnity() {
        assert NumberTheory.rootOfUnity(BigInteger.valueOf(2), BigInteger.valueOf(5)).equals(BigInteger.valueOf(4));
        assert NumberTheory.rootOfUnity(BigInteger.valueOf(3), BigInteger.valueOf(7)).equals(BigInteger.valueOf(2));
        assert NumberTheory.rootOfUnity(BigInteger.valueOf(5), BigInteger.valueOf(11)).equals(BigInteger.valueOf(4));
    }

    @Test
    public void testIsPrime() {

        assert NumberTheory.isPrime(BigInteger.valueOf(2));
        assert NumberTheory.isPrime(BigInteger.valueOf(3));
        assert NumberTheory.isPrime(BigInteger.valueOf(5));
        assert NumberTheory.isPrime(BigInteger.valueOf(7));
        assert NumberTheory.isPrime(BigInteger.valueOf(11));
        assert !NumberTheory.isPrime(BigInteger.valueOf(12));
        assert !NumberTheory.isPrime(BigInteger.valueOf(14));
        assert !NumberTheory.isPrime(BigInteger.valueOf(15));
        assert !NumberTheory.isPrime(BigInteger.valueOf(21));
        assert !NumberTheory.isPrime(BigInteger.valueOf(25));

        assert NumberTheory.isPrime(BigInteger.valueOf(7919));
        assert !NumberTheory.isPrime(BigInteger.valueOf(7921));

    }


    @Test
    public void testFindGenerator() {

        assert NumberTheory.findGenerator(BigInteger.valueOf(5)).equals(BigInteger.valueOf(2));
        assert NumberTheory.findGenerator(BigInteger.valueOf(7)).equals(BigInteger.valueOf(3));
        assert NumberTheory.findGenerator(BigInteger.valueOf(11)).equals(BigInteger.valueOf(2));
        // 这么牛逼，居然都过了，那继续往前走了
        assert NumberTheory.findGenerator(BigInteger.valueOf(262147)).equals(BigInteger.valueOf(2));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262151)).equals(BigInteger.valueOf(13));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262153)).equals(BigInteger.valueOf(10));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262187)).equals(BigInteger.valueOf(2));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262193)).equals(BigInteger.valueOf(3));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262217)).equals(BigInteger.valueOf(3));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262231)).equals(BigInteger.valueOf(12));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262237)).equals(BigInteger.valueOf(7));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262253)).equals(BigInteger.valueOf(2));
        assert NumberTheory.findGenerator(BigInteger.valueOf(262261)).equals(BigInteger.valueOf(2));
    }

}
