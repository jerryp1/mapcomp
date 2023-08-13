package edu.alibaba.mpc4j.crypto.fhe.utils;

import org.junit.Test;

import java.math.BigInteger;

/**
 * @author Qixian Zhou
 * @date 2023/7/13
 */
public class CrtContextTest {


    @Test
    public void testGeneratePrimes() {
        int numPrimes = 4;
        int primeSize = 9;
        int polyModulusDegree = 256;

        CrtContext crt = new CrtContext(numPrimes, primeSize, polyModulusDegree);
        for(long prime: crt.primes) {
            assert prime > (1 << primeSize);
            assert prime % (2 * polyModulusDegree) == 1;
        }
    }

    @Test
    public void testInverse() {

        int numPrimes = 4;
        int primeSize = 9;
        int polyModulusDegree = 256;
        CrtContext crt = new CrtContext(numPrimes, primeSize, polyModulusDegree);

        long origin = 178;
        long[] values = crt.crt(origin);
        long reverse = crt.reconstruct(values).longValue();
        assert origin == reverse;
    }

}
