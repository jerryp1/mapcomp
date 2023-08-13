package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Test;

/**
 * @author Qixian Zhou
 * @date 2023/7/27
 */
public class PrimesTest {



    @Test
    public void supportOptimizeTest(){
        long[] pArray = new long[]{ 3, 17, 1987, 4611686018326724609L};
        // ground truth comes form  related test in fhe.rs
        boolean[] res = new boolean[] {false, false, false, true};

        for (int i = 0; i < pArray.length; i++) {
            assert Primes.supportsOptimize(pArray[i]) == res[i];
        }
    }
}
