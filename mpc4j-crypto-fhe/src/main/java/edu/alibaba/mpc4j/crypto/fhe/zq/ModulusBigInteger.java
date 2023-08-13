package edu.alibaba.mpc4j.crypto.fhe.zq;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.LongStream;


/**
 * A data structure used to represent Zq, q is an integer up to 62 bits.
 *
 * @author Qixian Zhou
 * @date 2023/7/27
 */
public class ModulusBigInteger {

    public long p;

    public int leadingZeros;

    public int nBits;
    // may be a 64 bits number, however long can not totally correctly represent a 64-bit number, so use BigIntegr
    public BigInteger barrettHi;
    //
    public BigInteger barrettLo;

    public boolean supportsOpt;



//    public final static BigInteger HIGH_64_BITS_MASK = new BigInteger("FFFFFFFFFFFFFFFF0000000000000000", 16);
    public final static BigInteger LOW_64_BITS_MASK = new BigInteger("0000000000000000FFFFFFFFFFFFFFFF", 16);
    // max bit-count of modulus q
    public final static int MODULUS_BIT_COUNT_MAX = 62;
    public final static int MODULUS_BIT_COUNT_MIN = 2;


//    private final static int MAX



//    public UniformIntegerDistribution distribution;

    public ModulusBigInteger(long p) {
        // p = 2 is a specific case, when p = 2, the result of barrettHi is a negative number, because in java long can not represent 2^63
        // so we reject p = 2 for securely use Barrett Reduce
        // 其实如果 barrettHi/Lo 用 BigInteger 来表示，就不存在这个问题了，即 p < 2 也行
        if (p < 2  || (p >> MODULUS_BIT_COUNT_MAX) != 0) {
            throw new IllegalArgumentException("Invalid modulus: modulus " +  p +  " should be between 2 and (1 << 62) - 1.");
        }
        this.p = p;
        leadingZeros = Long.numberOfLeadingZeros(p);
        // floor(2^k / p)  , k = 128 , k > log(p), p = 2^{62}
        BigInteger barrett = BigInteger.ONE.setBit(128).divide(BigInteger.valueOf(p));
        barrettHi = barrett.shiftRight(64);
        // this & 0x0000000000000000FFFFFFFFFFFFFFFF, get the lower 64 bits
//        barrettLo = barrett.or(BigInteger.ONE.setBit(64).subtract(BigInteger.ONE)).longValue();
        // this directly take the low 64 bits, would to take negative value, because primitive type long
        // can not represent a 64 bits positive number, so we use BigInteger to represent barrettLo
//        barrettLo = barrett.longValue();
        barrettLo = barrett.and(LOW_64_BITS_MASK);
        supportsOpt = Primes.supportsOptimize(p);
    }


    /**
     *
     * @return p
     */
    public long getP() {
        return p;
    }

    /**
     * @param a a
     * @param b b
     * @return (a + b) % p
     */
    public long add(long a, long b) {
        assert a < p && b < p;
        return ModulusBigInteger.reduce1(a + b, p);
    }

    /**
     *
     * @param a a
     * @param b b
     * @return
     */
    public long sub(long a, long b) {
        assert a < p && b < p;
        return ModulusBigInteger.reduce1(a + p - b, p);
    }

    /**
     *
     * @param a a
     * @param b b
     * @return  a * b % p
     */
    public long mul(long a, long b) {
        assert a < p && b < p;
        return reduceU128(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
    }

    /**
     *
     * @param a a
     * @param b b
     * @return  a * b % p
     */
    public long mulOptimize(long a, long b) {
        assert a < p && b < p;
        return reduceOptimizeU128(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
    }


    public long neg(long a) {
        assert a < p;
        return ModulusBigInteger.reduce1(p - a, p);
    }

    public long[] negArray(long[] as, boolean parallel) {
        LongStream longStream = Arrays.stream(as);
        longStream = parallel ? longStream.parallel() : longStream;
        return longStream.map(this::neg).toArray();
    }


    public long[] addArray(long[] as, long[] bs, boolean parallel) {

        assert as.length == bs.length;
        IntStream indexIntStream = IntStream.range(0, as.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
                .mapToLong(i -> add(as[i], bs[i]))
                .toArray();
    }

    public long[] subArray(long[] as, long[] bs, boolean parallel) {

        assert as.length == bs.length;
        IntStream indexIntStream = IntStream.range(0, as.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
                .mapToLong(i -> sub(as[i], bs[i]))
                .toArray();
    }


    public long[] mulArray(long[] as, long[] bs, boolean parallel) {
        assert as.length == bs.length;
        IntStream indexIntStream = IntStream.range(0, as.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;

        if (supportsOpt) {
            return indexIntStream
                    .mapToLong(i -> mulOptimize(as[i], bs[i]))
                    .toArray();
        }else {
            return indexIntStream
                    .mapToLong(i -> mul(as[i], bs[i]))
                    .toArray();
        }
    }


    public long[] reduceArray(long[] as, boolean parallel) {
        LongStream longStream = Arrays.stream(as);
        longStream = parallel ? longStream.parallel() : longStream;
        return longStream.map(this::reduce).toArray();
    }

    /**
     * convert a \in [0, p) to a \in [-p/2, p/2)
     * @param a a
     * @return a\in [-p/2, p/2)
     */
    public long center(long a) {
        assert a < p;
        // a >= p/2 ---> a - p , when a = p/2 , res = p/2 - p = -p/2
        // So the range is [-p/2, p/2)
        return a >= (p >> 1) ? a - p : a;
    }
    /**
     *
     * @param as long array
     * @param parallel parallel
     * @return
     */
    public long[] centerArray(long[] as, boolean parallel) {
        LongStream longStream = Arrays.stream(as);
        return longStream.map(this::center).toArray();
    }

    /**
     *
     * @param a base
     * @param n exponent
     * @return a^n mod p
     */
    public long pow(long a, long n) {
        assert a < p && n < p;
        if (n == 0) {
            return 1;
        }else if (n == 1) {
            return a;
        }else {
            // fast power mod, ref: https://oi-wiki.org/math/binary-exponentiation/
            long r = a;
            // bit-count of n
            int i = (MODULUS_BIT_COUNT_MAX - Long.numberOfLeadingZeros(n));
            while (i >= 0) {
                // a^n   ---> n = 1001
                // r * r mod p
                r = mul(r, r);
                if (((n >> i) & 1) == 1) {
                    // mul mod
                    r = mul(r, a);
                }
                i -= 1;
            }
            return r;
            // why below implementation version so slow?
//            long r = 1;
//            while (n >= 0) {
//                if ((n & 1) == 1){
//                    r = mul(r, a);
//                }
//                a = mul(a, a);
//                n = n >> 1;
//            }
        }
    }

    /**
     *  fermat small theory:
     *  p is a prime number, gcd(a, p) = 1, then: a^{p-1} = 1 mod p
     *  so a^{p-2} * a = 1 mod p , so a^{-1} = a^{p-2} mod p
     * @param a a
     * @return a^{-1} mod p, satisfy a*a^{-1} = 1 mod p
     */
    public long inv(long a) {
        if (!Primes.isPrime(p) || a == 0) {
            throw new IllegalArgumentException("modulus p: " + p +  " is not a prime number or a(" + a + ") = 0");
        }
        long r = pow(a, p - 2);
        assert mul(a, r) == 1;
        return r;
    }

    /**
     *  Barrett reduce
     * @param a a
     * @return a % p
     */
    public long reduce(long a) {
        return ModulusBigInteger.reduce1(lazyReduce(a), p);
    }


    /**
     *  Barrett reduce
     * @param a a
     * @return a % p
     */
    public long reduceOptimize(long a) {
        return ModulusBigInteger.reduce1(lazyReduceOptimize(a), p);
    }

    /**
     * reduce a to [0, 2p)
     * @param a up to 62-bits
     * @return
     */
    public long lazyReduce(long a) {
        // m = floor(2^k/p)
        // split a * m into hi and lo two parts, m has been precomputed, just barrettLo and barrettHi
        // q0 = (m * (a >> 64) ) >> 64
        // q1 =
        BigInteger pLoLo = BigInteger.valueOf(a).multiply(barrettLo).shiftRight(64);
        BigInteger pLoHi = BigInteger.valueOf(a).multiply(barrettHi);

        BigInteger q = pLoLo.add(pLoHi).shiftRight(64);
        // a - q*p
        long r = BigInteger.valueOf(a).subtract(q.multiply(BigInteger.valueOf(p))).longValue();
        // r < 2*p, because we have limited the p max is 62 bits, just the 2^62 - 1, so tha max value of 2*p is 2^63 - 2 , which is a valid long value
        assert r < 2 * p;
        // correctness verify
        assert r % p == a % p;
        return r;
    }

    public long[] lazyReduceArray(long[] as, boolean parallel) {

        LongStream longStream = Arrays.stream(as);
        longStream = parallel ? longStream.parallel() : longStream;
        if (supportsOpt) {
            return longStream.map(this::lazyReduceOptimize).toArray();
        }else {
            return longStream.map(this::lazyReduce).toArray();
        }
    }


    /**
     * reduce a to [0, 2p)
     * @param a up to 62-bits
     * @return
     */
    public long lazyReduceOptimize(long a) {

        long q = a >> (64 - leadingZeros);
        long r = BigInteger.valueOf(a).subtract(BigInteger.valueOf(q).multiply(BigInteger.valueOf(p))).longValue();

        assert r < 2 * p;
        assert r % p == a % p;
        return r;
    }

    /**
     *
     * @param a 128-bit value
     * @return a long value in [0, 2p), just the x * floor(2^k/p) \approx floor (x/p), k = 128
     */
    public long lazyReduceU128(BigInteger a) {
        // 这里有一个大问题就是， 一个接近128-bit的BigInteger, 其 低64-bit, long 是装不下的
        // 这样就有可能出现负数， 所以不能转回long, 后续的计算其实也不需要long，直接全部在 BigInteger 下计算吧
        BigInteger aLo = a.and(LOW_64_BITS_MASK);
        BigInteger aHi = a.shiftRight(64);

        BigInteger pLoLo = aLo.multiply(barrettLo).shiftRight(64);
        BigInteger pHiLo = aHi.multiply(barrettLo);
        BigInteger pLoHi = aLo.multiply(barrettHi);

        BigInteger q = pLoHi.add(pHiLo).add(pLoLo).shiftRight(64).add(aHi.multiply(barrettHi));
        long r = a.subtract(q.multiply(BigInteger.valueOf(p))).longValue();
        // because we have limited the p max is 62 bits, just the 2^62 - 1, so tha max value of 2*p is 2^63 - 2 , which is a valid long value
        assert r < 2 * p;
        assert r % p == a.mod(BigInteger.valueOf(p)).longValue(): "r % p: " + r % p + ", a % p: " + a.mod(BigInteger.valueOf(p));

        return r;
    }


    public long lazyReduceOptimizeU128(BigInteger a) {
        // a < p*p
        assert a.compareTo(BigInteger.valueOf(p).multiply(BigInteger.valueOf(p))) < 0;

        BigInteger q = (barrettLo.multiply(a.shiftRight(64)).add(a.shiftLeft(leadingZeros))).shiftRight(64);

        long r = a.subtract(q.multiply(BigInteger.valueOf(p))).longValue();

        assert r < 2 * p;
        assert r % p == a.mod(BigInteger.valueOf(p)).longValue();

        return r;
    }

    /**
     *
     * @param a a 128-bit integer
     * @return
     */
    public long reduceU128(BigInteger a) {
        return ModulusBigInteger.reduce1(lazyReduceU128(a), p);
    }

    public long reduceOptimizeU128(BigInteger a) {
        return ModulusBigInteger.reduce1(lazyReduceOptimizeU128(a), p);
    }




    /**
     * In java, we do not consider Side Channel Attack, so we do not need to provide non-variable time implementations.
     * So we remove the "VariableTime" in fhe.rs. Default all the implementation is variable time.
     * @param x x \in [0, 2p) , only when x in [0, 2p) we can use this reduce1.
     * @param p modulus
     * @return
     */
    public static long reduce1(long x, long p) {
        // 对于一个数，我们说 它有 n 比特，指的是这个数的范围是
        // [2^{n-1}, 2^n - 1] , 这里我们只考虑了 无符号数
        // 我们现在考虑 有符号数，例如 long 是 64 个比特，其中 1个比特拿来表示符号，剩余 63 个比特来表示数
        // 那么我们可以认为 long 是一个 63-bit 的无符号数
        // 那么 一个 63 比特的数，其范围数 [2^62, 2^63 - 1) ，注意这里的右区间是 开区间
        // 那么 任意一个 63 比特的数，右移 63 位，结果一定是 0
        // 那么下面的判断，就是限制这里的 p 为 62-bit，也就是：
        // [2^61, 2^{62} - 1] , 注意这里又是比区间了，为什么要这样限制呢？
        // 因为 两个 62比特的数相加，最多也就 63-bit
        // 例如 a = 2^62 - 1 , b = 2^62 - 1
        //    a + b = 2^63 - 2, 这一定是 63-bit 的数，那么 在使用long 的时候，就不会溢出变成负数
        assert p >> 62 == 0;
        // only x satisfy this condition, then we can correctly reduce
        assert x < 2 * p;

//        if (x >= p) {
//            return x - p;
//        }
//        return x;

        return x >= p ? x - p : x;
    }

    /**
     *
     * @param length array length
     * @param random random object
     * @return a long array with random uniform elements in [0, p)
     */
    public long[] randomUniformArray(int length, Random random) {
        long[] res = new long[length];
        for (int i = 0; i < length; i++) {
            res[i] = Math.abs(random.nextLong()) % p;
        }
        return res;
    }



}
