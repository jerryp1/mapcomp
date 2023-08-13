package edu.alibaba.mpc4j.crypto.fhe.utils;

import com.sun.tools.internal.xjc.reader.xmlschema.BindGreen;

import javax.swing.*;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Some number theory functions for other functions used.
 *
 * @author Qixian Zhou
 * @date 2023/7/10
 */
public class NumberTheory {

    private static final int PRIME_CERTAINTY = 20;


    /**
     * power mod
     *
     * @param value a
     * @param exp e
     * @param modulus n
     * @return a^e mod n
     */
    public static BigInteger modExp(BigInteger value, BigInteger exp, BigInteger modulus) {
        return value.modPow(exp, modulus);
    }


    public static BigInteger rootOfUnity(BigInteger order, BigInteger modulus) {
        // 2n | modulus - 1
        assert modulus.subtract(BigInteger.ONE).mod(order).equals(BigInteger.ZERO): " Must have order q | m - 1";

        BigInteger generator = findGenerator(modulus);
        assert generator != null;
        // g_{2n} = g^{p-1/2n}
        BigInteger result = modExp(generator, modulus.subtract(BigInteger.ONE).divide(order), modulus  );

        assert !result.equals(BigInteger.ONE): "This it not a 2n-root of unity";

        return result;
    }

    /**
     * Finds an inverse in a given prime modulus.
     *
     * @param value a
     * @param modulus n
     * @return a^{-1} mod n
     */
    public static BigInteger modInv(BigInteger value, BigInteger modulus) {

        assert modulus.isProbablePrime(PRIME_CERTAINTY): "modulus must be prime number.";

        return modExp(value, modulus.subtract(BigInteger.valueOf(2)), modulus);

    }

    /**
     *  Determines whether a number is prime.
     *
     * @param value a value
     * @return True if number is prime, False otherwise.
     */
    public static boolean isPrime(BigInteger value) {

        return value.isProbablePrime(PRIME_CERTAINTY);
    }



    /**
     * find the smallest generator of Z_p^*, modulus must be prime
     *
     * @param modulus Z_p^*
     * @return smallest generator
     */
    public static BigInteger findGenerator(BigInteger modulus) {

//        System.out.println("modulus: " +  modulus );
        assert modulus != null && modulus.isProbablePrime(PRIME_CERTAINTY): "Modulus must be prime number.";

        if (modulus.compareTo(BigInteger.valueOf(4)) <= 0) {
            return modulus.subtract(BigInteger.ONE);
        }

        // Find prime factors of p-1 once
        BigInteger n = modulus.subtract(BigInteger.ONE);
        Set<BigInteger> factors = factorize(n);

        // Try to find the primitive root by starting at random number
        BigInteger g = BigInteger.valueOf(2);
        while (!checkPrimitiveRoot(g, modulus, n, factors)) {
            g = g.add(BigInteger.ONE);
        }
        return g;
    }

    /**
     *
     * @param g candidate generator
     * @param modulus modulus
     * @param n  modulus - 1
     * @param factors n = q1 * q2 * .. qm
     * @return
     */
    private static boolean checkPrimitiveRoot(BigInteger g, BigInteger modulus,
                                              BigInteger n, Set<BigInteger> factors) {
        // Run g^(n / "each factor) mod p
        // If the is 1 mod p then g is not a primitive root
        Iterator<BigInteger> i = factors.iterator();
        while (i.hasNext()) {
            if (g.modPow( n.divide(i.next()), modulus).equals(BigInteger.ONE) ) {
                return false;
            }
        }
        return true;
    }


    private static Set<BigInteger> factorize(BigInteger n) {

        Set<BigInteger> factors = new HashSet<>();
        BigInteger i = BigInteger.valueOf(2);
        for ( ; i.compareTo(n) <= 0; i = i.add(BigInteger.ONE)) {
            // only if n % i == 0, i is one of factors
            while (n.mod(i).equals(BigInteger.ZERO)) {
                factors.add(i);
                n = n.divide(i);
                if (n.isProbablePrime(PRIME_CERTAINTY)) {
                    factors.add(n);
                    return factors;
                }
            }
        }
        return factors;
    }

}
