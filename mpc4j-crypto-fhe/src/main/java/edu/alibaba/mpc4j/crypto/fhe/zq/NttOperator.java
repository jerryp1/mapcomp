package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.utils.NttContext;

import java.util.stream.IntStream;

/**
 * NTT Operator under Zq
 *
 * @author Qixian Zhou
 * @date 2023/8/11
 */
public class NttOperator {


    public Modulus mod;

    public long degree;

    public long degreeInv;
    // 2*degree-root of unity mod modulus, g_{2n}^i, i = 0, ..., n-1
    public long[] omegas;
    // (g_{2n}^{-1})^i, i = 0, ..., n-1
    public long[] omegasInv;
    // index:      0 1 2 ... 7
    // reversed:   0 4 2 6 1 5 3 7
    public int[] reversedIndex;



    // default true
    private boolean parallel;

    public NttOperator(Modulus mod, long degree) {
        if (!supportNtt(mod.getValue(), degree)) {
            throw new IllegalArgumentException("modulus and degree do not support NTT");
        }
        parallel = true;
        this.mod = mod;
        this.degree = degree;
        degreeInv = mod.inv(degree);
        long[] tmp = new long[1];
        if (!Numth.tryPrimitiveRoot(degree << 1, mod, tmp)) {
            throw new IllegalArgumentException("No 2n-root of unity under current modulus");
        }
        long omega = tmp[0];
        // compute g_{2n}^i , i = 0,...,n-1
        IntStream intStream = IntStream.range(0, (int) degree);
        intStream = parallel ? intStream.parallel() : intStream;
        this.omegas = intStream.mapToLong(i-> mod.exponent(omega, i)).toArray();
        // compute (g_{2n}^{-1})^i , i = 0,...,n-1
        long omegaInv = mod.inv(omega);
        this.omegasInv = intStream.mapToLong(i-> mod.exponent(omegaInv, i)).toArray();



    }

    /**
     * modulus must be prime number
     * degree must be power of 2
     * modulus % 2n == 1
     *
     * @param modulus
     * @param degree
     * @return
     */
    public static boolean supportNtt(long modulus, long degree) {
        assert degree >= 8;
        return UintCore.getPowerOfTwo(degree) > 0 && Primes.isPrime(modulus) && (modulus % (degree << 1) == 1);
    }

}
