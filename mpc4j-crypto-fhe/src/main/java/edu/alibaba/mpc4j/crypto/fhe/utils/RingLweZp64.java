package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.RingsZp64;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Mainly provide basic operations under Z_Q/x^n + 1, where the maximum value of Q is 63-bit
 * Q must be a prime number.
 *
 * @author Qixian Zhou
 * @date 2023/7/12
 */
public class RingLweZp64 {

    public RingsZp64 ringsZp64;
    public long modulus;

    public RingLweZp64(EnvType envType, long modulus) {
        this.ringsZp64 = new RingsZp64(envType, modulus);
        this.modulus = modulus;
    }

    public long rootOfUnity(long order) {
        // 2n | modulus - 1
        assert (modulus - 1) % order == 0 : " Must have order 2n | m - 1";
        long generator = findSmallestGenerator();

        // g_{2n} = g^{p-1/2n}
        long result =  modExp(generator, (modulus - 1) / order);
        assert result != 1: "g_{2n} cannot be one, try next generator";
        // todo, should try next genrator
//        if (result == 1) {
//
//        }
        return result;
    }


    public long add(long a, long b) {
        if (a < 0) {
            a = negativeMod(a);
        }
        if( b < 0) {
            b = negativeMod(b);
        }
        return ringsZp64.add(a, b);
    }

    public long sub(long a, long b) {
        if (a < 0) {
            a = negativeMod(a);
        }
        if (b < 0) {
            b = negativeMod(b);
        }
        return ringsZp64.sub(a, b);
    }

    public long mul(long a, long b) {
        if (a < 0) {
           a = negativeMod(a);
        }
        if (b < 0) {
           b = negativeMod(b);
        }
        return ringsZp64.mul(a, b);
    }

    public long modExp(long value, long exp) {

        assert exp >= 0: "exp must be >= 0";
        if (exp == 0) {
            return 1;
        }
        if (value < 0) {
            value = negativeMod(value);
        }
        return ringsZp64.pow(value, exp);
    }


    private long negativeMod(long a) {
        long b = a % modulus;
        b += modulus;
        return b;
    }
    /**
     *
     * @param value value
     * @return
     */
    public long modInv(long value) {
        if (value < 0) {
            value = negativeMod(value);
        }
        return ringsZp64.inv(value);
    }

    /**
     *  find the generator of Z_Q^*
     * @return
     */
    public long findSmallestGenerator() {

        if (modulus <= 4) {
            return modulus - 1;
        }

        Set<Long> factors = factorize(modulus - 1);
        long g = 2;
        while (!checkPrimitiveRoot(g, factors)) {
            g += 1;
        }
        return g;
    }


    private boolean checkPrimitiveRoot(long g, Set<Long> factors) {
        // Run g^(n / "each factor) mod p
        // If the is 1 mod p then g is not a primitive root

        for (long f: factors) {
            if ( ringsZp64.pow(g, ringsZp64.getPrime() / f ) == 1 ) {
                return false;
            }
        }
        return true;
    }

    private static Set<Long> factorize(long n) {

        Set<Long> factors = new HashSet<>();
        long i = 2;
        for ( ; i <= n; i += 1) {
            // only if n % i == 0, i is one of factors
            while (n % i == 0) {
                factors.add(i);
                n = n / i;
                if (BigInteger.valueOf(n).isProbablePrime(CommonConstants.STATS_BIT_LENGTH)) {
                    return factors;
                }
            }
        }
        return factors;
    }


}
