package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;


/**
 * @author Qixian Zhou
 * @date 2023/7/27
 */
public class ModulusBigIntegerTest {



    private static final int MAX_ROUND_NUM = 100;

    private static final long[] SMALL_P_ARRAY = new long[]{2,  3, 17, 1987, 4611686018326724609L};

    private final ModulusBigInteger[] modulusBigIntegerArray;

    public ModulusBigIntegerTest() {
        // these prime moduli come from related test of fhe.rs
        long[] pArray = new long[]{ 2, 3, 17, 1987, 4611686018326724609L,
                4611686018309947393L,
                4611686018282684417L,
                4611686018257518593L,
                4611686018232352769L,
                4611686018171535361L,
                4611686018106523649L,
                4611686018058289153L,
                4611686018051997697L,
                4611686017974403073L,
                4611686017812922369L,
                4611686017781465089L,
                4611686017773076481L,
                4611686017678704641L,
                4611686017666121729L,
                4611686017647247361L,
                4611686017590624257L,
                4611686017554972673L,
                4611686017529806849L,
                4611686017517223937L};
        modulusBigIntegerArray = new ModulusBigInteger[pArray.length];
        for (int i = 0; i < pArray.length; i++) {
            modulusBigIntegerArray[i] = new ModulusBigInteger(pArray[i]);
        }
    }


    /**
     * 验证基于 BigInteger 的 barretHi, barrettLo 的计算是否符合预期
     */
    @Test
    public void barrettLoHiTest(){
        // when p = 2, barretHi: -9223372036854775808, barretLo: 0 in java
        // 从计算的角度来说，这个是对的，核心问题是 java 的 long 的范围无法表示 2^{63}
        long[] pArray = new long[]{ 2,  3, 17, 1987, 4611686018326724609L};
        // ground truth comes form  related test in fhe.rs
        BigInteger p2BarrettLo = new BigInteger("9223372036854775808");
        long[] hiArray = new long[]{ 0, 6148914691236517205L, 1085102592571150095L, 9283716192103448L, 4};
        long[] loArray = new long[]{ 0, 6148914691236517205L,  1085102592571150095L,  4084835124525517217L, 1610612720};

        for (int i = 0; i < pArray.length; i++) {
            ModulusBigInteger modulusBigInteger = new ModulusBigInteger(pArray[i]);
            if (modulusBigInteger.getP() == 2) {
                assert modulusBigInteger.barrettHi.compareTo(p2BarrettLo) == 0;
            }else {
                assert modulusBigInteger.barrettHi.compareTo(BigInteger.valueOf(hiArray[i])) == 0;
            }
            assert modulusBigInteger.barrettLo.compareTo(BigInteger.valueOf(loArray[i])) == 0;
        }
    }


    @Test
    public void powTest() {
        Random random = new Random();

        long[] pArray = new long[]{2,  3, 17, 1987, 4611686018326724609L};

        for (long p: pArray) {
            ModulusBigInteger modulusBigInteger = new ModulusBigInteger(p);

            Assert.assertEquals(modulusBigInteger.pow(p - 1, 0), 1);
            Assert.assertEquals(modulusBigInteger.pow(p - 1, 1), p - 1);
            // (p-1)^2 mod p = 1, (p-1)^{-1} = p - 1 (mod p)
            // 一个简单的证明，求 x^2 = 1 mod p , x < p
            // x^2 - 1 = 0 mod p
            // p | x^2 - 1  ---> p | (x-1)(x+1)
            // 只有两种情况： p | x -1  or p | x + 1
            // p | x - 1 , x < p, x 是无解的
            // p | x + 1 , x = p - 1, so (p-1)^2 = 1
            Assert.assertEquals(modulusBigInteger.pow(p - 1, 2 % p), 1);
            Assert.assertEquals(modulusBigInteger.pow(1, p - 2), 1);
            Assert.assertEquals(modulusBigInteger.pow(1, p - 1), 1);

            Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.pow(p, 1));
            Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.pow(p << 1, 1));
            Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.pow(0, p));
            Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.pow(0, p << 1));

            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, p)
                long a = sampleUniform(modulusBigInteger.getP(), random);
                // the aim of % 1000 is that control while loop times
                long b = sampleUniform(modulusBigInteger.getP(), random) % 1000;
                long c = b % 1000;
                long r = 1;
                // a * a ... * a = a^b mod p
                while (c > 0) {
                    r = modulusBigInteger.mul(r, a);
                    c -= 1;
                }
                // directly compute a^b mod p
                Assert.assertEquals(modulusBigInteger.pow(a, b), r);
                // use BigInteger to get right result
                long truth = BigInteger.valueOf(a).modPow(BigInteger.valueOf(b), BigInteger.valueOf(modulusBigInteger.getP())).longValue();
                Assert.assertEquals(modulusBigInteger.pow(a, b), truth);
            }
        }
    }

    @Test
    public void invTest() {
        Random random = new Random();
        for (long p: SMALL_P_ARRAY) {
            ModulusBigInteger modulusBigInteger = new ModulusBigInteger(p);
            Assert.assertEquals(modulusBigInteger.inv(1), 1);
            Assert.assertEquals(modulusBigInteger.inv(p - 1), p - 1);

            Assert.assertThrows(IllegalArgumentException.class, () -> modulusBigInteger.inv(0));
            Assert.assertThrows(Error.class, () -> modulusBigInteger.inv(p << 1));
            Assert.assertThrows(Error.class, () -> modulusBigInteger.inv(p));
            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, p)
                long a = 0;
                while (a == 0) {
                    a = sampleUniform(modulusBigInteger.getP(), random);
                }
                long aInv = modulusBigInteger.inv(a);
                Assert.assertEquals(modulusBigInteger.mul(a, aInv), 1);

                long truth = BigInteger.valueOf(a).modInverse(BigInteger.valueOf(p)).longValue();
                Assert.assertEquals(aInv, truth);
            }
        }
    }

    @Test
    public void reduceTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, 2p)
                long a = sampleUniform(2 * modulusBigInteger.getP(), random);
                assert modulusBigInteger.reduce(a) == a % modulusBigInteger.getP();
                if (modulusBigInteger.supportsOpt) {
                    Assert.assertEquals(modulusBigInteger.reduceOptimize(a), a % modulusBigInteger.getP());
                }
            }
        }
    }


    @Test
    public void reduceU128Test() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, 2p)
                BigInteger a = new BigInteger(128, random);
                Assert.assertEquals(modulusBigInteger.reduceU128(a), a.mod(BigInteger.valueOf(modulusBigInteger.getP())).longValue());
                if (modulusBigInteger.supportsOpt) {
                    // ensure a < p^2
                    a = a.mod(BigInteger.valueOf(modulusBigInteger.getP()).pow(2));
                    Assert.assertEquals(
                            modulusBigInteger.reduceOptimizeU128(a),
                            a.mod(BigInteger.valueOf(modulusBigInteger.getP())).longValue());
                }
            }
        }
    }

    @Test
    public void reduceArrayTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 1; i < MAX_ROUND_NUM; i++) {
                long[] a = modulusBigInteger.randomUniformArray(i, random);
                long[] b = modulusBigInteger.reduceArray(a, false);
                long[] b1 = modulusBigInteger.reduceArray(a, true);
                long[] b2 = new long[i];
                for (int j = 0; j < a.length; j++) {
                    b2[j] = modulusBigInteger.reduce(a[j]);
                }
                Assert.assertArrayEquals(b, b1);
                Assert.assertArrayEquals(b1, b2);

            }
        }
    }

    @Test
    public void lazyReduceTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, 2p)
                long a = sampleUniform(2 * modulusBigInteger.getP(), random);
                assert modulusBigInteger.lazyReduce(a) < 2 * modulusBigInteger.getP();
                assert modulusBigInteger.lazyReduce(a) % modulusBigInteger.getP() == modulusBigInteger.reduce(a);

                if (modulusBigInteger.supportsOpt) {
                    Assert.assertEquals(modulusBigInteger.lazyReduce(a), modulusBigInteger.lazyReduceOptimize(a));
                    Assert.assertEquals(modulusBigInteger.lazyReduceOptimize(a), a % modulusBigInteger.getP());
                }
            }
        }
    }

    @Test
    public void lazyReduceArrayTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 1; i < MAX_ROUND_NUM; i++) {
                long[] a = modulusBigInteger.randomUniformArray(i, random);
                long[] b = modulusBigInteger.lazyReduceArray(a, false);
                long[] b1 = modulusBigInteger.lazyReduceArray(a, true);
                long[] b2 = new long[i];
                for (int j = 0; j < a.length; j++) {
                    b2[j] = modulusBigInteger.lazyReduce(a[j]);
                }
                Assert.assertArrayEquals(b, b1);
                Assert.assertArrayEquals(b1, b2);

            }
        }
    }


    @Test
    public void negTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, p)
                long a = sampleUniform(modulusBigInteger.getP(), random);
                assert modulusBigInteger.neg(a) == (modulusBigInteger.getP() - a) % modulusBigInteger.getP();
            }
        }
    }
    @Test
    public void negArrayTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 1; i < MAX_ROUND_NUM; i++) {
                long[] a = modulusBigInteger.randomUniformArray(i, random);
                long[] aNeg = modulusBigInteger.negArray(a, false);
                long[] aNeg2 = modulusBigInteger.negArray(a, true);
                Assert.assertArrayEquals(aNeg, aNeg2);

                long[] aNeg3 = new long[a.length];
                for (int j = 0; j < a.length; j++) {
                    aNeg3[j] = modulusBigInteger.neg(a[j]);
                }
                Assert.assertArrayEquals(aNeg, aNeg3);
            }
        }
    }




    @Test
    public void addTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, p)
                long a = sampleUniform(modulusBigInteger.getP(), random);
                long b = sampleUniform(modulusBigInteger.getP(), random);

                assert modulusBigInteger.add(a, b) == (a + b) % modulusBigInteger.getP();
            }
        }
    }

    @Test
    public void subTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, p)
                long a = sampleUniform(modulusBigInteger.getP(), random);
                long b = sampleUniform(modulusBigInteger.getP(), random);

                assert modulusBigInteger.sub(a, b) == (a + modulusBigInteger.getP() -  b) % modulusBigInteger.getP();
            }
        }
    }

    @Test
    public void mulTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, p)
                long a = sampleUniform(modulusBigInteger.getP(), random);
                long b = sampleUniform(modulusBigInteger.getP(), random);

                long truth = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).mod(BigInteger.valueOf(modulusBigInteger.getP())).longValue();
                Assert.assertEquals(modulusBigInteger.mul(a, b), truth);

                long q = modulusBigInteger.getP() + 1; // q > p
                Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.mul(a, q));
                Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.mul(q, a));

            }
        }
    }

    @Test
    public void mulArrayTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 1; i < MAX_ROUND_NUM; i++) {
                long[] a = modulusBigInteger.randomUniformArray(i, random);
                long[] b = modulusBigInteger.randomUniformArray(i, random);

                long[] c = modulusBigInteger.mulArray(a, b, false);
                long[] c1 = modulusBigInteger.mulArray(a, b, true);
                long[] c2 = new long[i];
                for (int j = 0; j < a.length; j++) {
                    c2[j] = modulusBigInteger.mul(a[j], b[j]);
                }
                Assert.assertArrayEquals(c, c1);
                Assert.assertArrayEquals(c1, c2);

            }
        }
    }

    @Test
    public void subArrayTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 1; i < MAX_ROUND_NUM; i++) {
                long[] a = modulusBigInteger.randomUniformArray(i, random);
                long[] b = modulusBigInteger.randomUniformArray(i, random);

                long[] c = modulusBigInteger.subArray(a, b, false);
                long[] c1 = modulusBigInteger.subArray(a, b, true);
                long[] c2 = new long[i];
                for (int j = 0; j < a.length; j++) {
                    c2[j] = modulusBigInteger.sub(a[j], b[j]);
                }
                Assert.assertArrayEquals(c, c1);
                Assert.assertArrayEquals(c1, c2);

            }
        }
    }

    @Test
    public void addArrayTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 1; i < MAX_ROUND_NUM; i++) {
                long[] a = modulusBigInteger.randomUniformArray(i, random);
                long[] b = modulusBigInteger.randomUniformArray(i, random);

                long[] c = modulusBigInteger.addArray(a, b, false);
                long[] c1 = modulusBigInteger.addArray(a, b, true);
                long[] c2 = new long[i];
                for (int j = 0; j < a.length; j++) {
                    c2[j] = modulusBigInteger.add(a[j], b[j]);
                }
                Assert.assertArrayEquals(c, c1);
                Assert.assertArrayEquals(c1, c2);

            }
        }
    }



    @Test
    public void randomUniformArrayTest() {
        Random random = new Random();
        for (ModulusBigInteger modulusBigInteger : modulusBigIntegerArray) {
            for (int i = 1; i < MAX_ROUND_NUM; i++) {
                long[] v = modulusBigInteger.randomUniformArray(i, random);
                long[] w = modulusBigInteger.randomUniformArray(i, random);

                Assert.assertEquals(v.length, i);
                Assert.assertEquals(w.length, i);
                // false
                if (Long.numberOfLeadingZeros(modulusBigInteger.getP()) <= 30) {
                    Assert.assertFalse(Arrays.equals(v, w));
                }
            }
        }

    }

    @Test
    public void mulOptimizeTest() {
        Random random = new Random();
        long[] pArray = new long[]{4611686018326724609L};


        for (long p: pArray) {
            ModulusBigInteger modulusBigInteger = new ModulusBigInteger(p);
            assert Primes.supportsOptimize(modulusBigInteger.getP());

            assert modulusBigInteger.mulOptimize(0, 1) == 0;
            assert modulusBigInteger.mulOptimize(1, 1) == 1;
            assert modulusBigInteger.mulOptimize(2 % p, 3 % p) == 6 % p;
            assert modulusBigInteger.mulOptimize(p - 1, 1) == p - 1;
            assert modulusBigInteger.mulOptimize(p - 1, 2 % p) == p - 2;
            // test assert code is right, just when ground truth is false, the code will assert fail
            Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.mulOptimize(p, 1));
            Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.mulOptimize( p << 1, 1));
            Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.mulOptimize( 0, p));
            Assert.assertThrows(AssertionError.class, () -> modulusBigInteger.mulOptimize( 0, p << 1));


            for (int i = 0; i < MAX_ROUND_NUM; i++) {
                // a \in [0, p)
                long a = sampleUniform(modulusBigInteger.getP(), random);
                long b = sampleUniform(modulusBigInteger.getP(), random);

                long truth = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).mod(BigInteger.valueOf(modulusBigInteger.getP())).longValue();

                Assert.assertEquals(
                        modulusBigInteger.mulOptimize(a, b),
                        truth
                );

            }
        }
    }


    @Test
    public void reduceU128SingleTest() {
        BigInteger a = new BigInteger("1665428626110938167134328771729463553");
        long p = 4611686018309947393L;
        ModulusBigInteger modulusBigInteger = new ModulusBigInteger(p);

        long res = modulusBigInteger.lazyReduceU128(a);

    }

//    @Test
//    public void loHiTest() {
//
//        Random random = new Random();
//        for (int i = 0; i < 1; i++) {
//            BigInteger r = new BigInteger(128, random);
//            BigInteger rHi = r.shiftRight(64);
//            BigInteger rLo = r.and(ModulusBigInteger.LOW_64_BITS_MASK);
//
//            assert rHi.compareTo(ModulusBigInteger.LOW_64_BITS_MASK) < 0;
//            assert rLo.compareTo(ModulusBigInteger.LOW_64_BITS_MASK) < 0;
//
//
//
//            System.out.println(r);
//            BigInteger rr = rHi.shiftLeft(64).or(rLo);
//            System.out.println(rr);
//            assert r.equals(rr);
//
//            Assert.assertEquals( rLo.add(rHi.multiply(BigInteger.ONE.setBit(64))), r);
//        }
//
//
//    }




    @Test
    public void bigIntegerLo64Test() {
        Random random = new Random();
        // how to get a bigInteger lower 64-bit?
        for (int i = 0; i < MAX_ROUND_NUM; i++) {
            BigInteger a = new BigInteger(128, random);
            long aLo = a.or(BigInteger.ONE.setBit(64).subtract(BigInteger.ONE)).longValue();
            assert a.longValue() == aLo;
        }

    }


    private long sampleUniform(long minVal, long maxVal, Random random) {
        return minVal + Math.abs(random.nextLong()) % (maxVal - minVal);
    }

    private long sampleUniform(long maxVal, Random random) {
        return Math.abs(random.nextLong()) % maxVal;
    }



}
