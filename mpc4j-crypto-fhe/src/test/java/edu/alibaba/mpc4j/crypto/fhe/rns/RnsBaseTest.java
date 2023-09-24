package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/17
 */
public class RnsBaseTest {


    @Test
    public void create() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{0}));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{0, 3}));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{2, 2}));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{2, 3, 4}));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{3, 4, 5, 6}));

        new RnsBase(new long[]{3, 4, 5, 7});
        new RnsBase(new long[]{2});
        new RnsBase(new long[]{3});
        new RnsBase(new long[]{4});
    }

    @Test
    public void arrayAccess() {

        RnsBase rnsBase = new RnsBase(new long[]{2});
        Assert.assertEquals(1, rnsBase.getSize());
        Assert.assertEquals(new Modulus(2), rnsBase.getBase(0));
        RnsBase finalRnsBase = rnsBase;
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> finalRnsBase.getBase(1));


        rnsBase = new RnsBase(new long[]{2, 3, 5});
        Assert.assertEquals(3, rnsBase.getSize());
        Assert.assertEquals(new Modulus(2), rnsBase.getBase(0));
        Assert.assertEquals(new Modulus(3), rnsBase.getBase(1));
        Assert.assertEquals(new Modulus(5), rnsBase.getBase(2));
        RnsBase finalRnsBase1 = rnsBase;
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> finalRnsBase1.getBase(3));
    }


    @Test
    public void copy() {

        RnsBase rnsBase = new RnsBase(new long[]{3, 4});

        RnsBase rnsBase1 = new RnsBase(rnsBase);
        Assert.assertEquals(rnsBase.getSize(), rnsBase1.getSize());
        Assert.assertEquals(rnsBase.getBase(0), rnsBase1.getBase(0));
        Assert.assertEquals(rnsBase.getBase(1), rnsBase1.getBase(1));

        Assert.assertArrayEquals(rnsBase.getBaseProd(), rnsBase1.getBaseProd());
        Assert.assertArrayEquals(rnsBase.getInvPuncturedProdModBaseArray(), rnsBase1.getInvPuncturedProdModBaseArray());
        Assert.assertTrue(Arrays.deepEquals(rnsBase.getPuncturedProdArray(), rnsBase1.getPuncturedProdArray()));
    }

    @Test
    public void contains() {

        RnsBase rnsBase = new RnsBase(new long[]{2, 3, 5, 13});
        Assert.assertTrue(rnsBase.contains(2));
        Assert.assertTrue(rnsBase.contains(3));
        Assert.assertTrue(rnsBase.contains(5));
        Assert.assertTrue(rnsBase.contains(13));

        Assert.assertFalse(rnsBase.contains(7));
        Assert.assertFalse(rnsBase.contains(4));
        Assert.assertFalse(rnsBase.contains(0));
    }

    @Test
    public void isSubBaseOf() {

        RnsBase base = new RnsBase(new long[]{2});
        RnsBase base2 = new RnsBase(new long[]{2});
        Assert.assertTrue(base.isSubBaseOf(base2));
        Assert.assertTrue(base2.isSubBaseOf(base));
        Assert.assertTrue(base2.isSuperBaseOf(base));
        Assert.assertTrue(base.isSuperBaseOf(base2));

        base = new RnsBase(new long[]{2});
        base2 = new RnsBase(new long[]{2, 3});
        Assert.assertTrue(base.isSubBaseOf(base2));
        Assert.assertTrue(base2.isSuperBaseOf(base));
        Assert.assertFalse(base.isSuperBaseOf(base2));
        Assert.assertFalse(base2.isSubBaseOf(base));

        base = new RnsBase(new long[]{3, 13, 7});
        base2 = new RnsBase(new long[]{2, 3, 5, 7, 13, 19});
        Assert.assertTrue(base.isSubBaseOf(base2));
        Assert.assertTrue(base2.isSuperBaseOf(base));
        Assert.assertFalse(base.isSuperBaseOf(base2));
        Assert.assertFalse(base2.isSubBaseOf(base));

        base = new RnsBase(new long[]{3, 13, 7, 23});
        base2 = new RnsBase(new long[]{2, 3, 5, 7, 13, 19});
        Assert.assertFalse(base.isSubBaseOf(base2));
        Assert.assertFalse(base2.isSuperBaseOf(base));
        Assert.assertFalse(base.isSuperBaseOf(base2));
        Assert.assertFalse(base2.isSubBaseOf(base));

    }


    @Test
    public void extend() {

        RnsBase base = new RnsBase(new long[]{3});
        RnsBase base2 = base.extend(5);
        Assert.assertEquals(2, base2.getSize());
        Assert.assertEquals(base.getBase(0), base2.getBase(0));
        Assert.assertEquals(new Modulus(5), base2.getBase(1));

        RnsBase base3 = base2.extend(7);
        Assert.assertEquals(3, base3.getSize());
        Assert.assertEquals(base2.getBase(0), base3.getBase(0));
        Assert.assertEquals(base2.getBase(1), base3.getBase(1));
        Assert.assertEquals(new Modulus(7), base3.getBase(2));

        Assert.assertThrows(IllegalArgumentException.class, () -> base3.extend(0));
        Assert.assertThrows(IllegalArgumentException.class, () -> base3.extend(14));

        RnsBase base4 = new RnsBase(new long[]{3, 4, 5});
        RnsBase base5 = new RnsBase(new long[]{7, 11, 13, 17});
        RnsBase base6 = base4.extend(base5);

        Assert.assertEquals(7, base6.getSize());
        Assert.assertEquals(new Modulus(3), base6.getBase(0));
        Assert.assertEquals(new Modulus(4), base6.getBase(1));
        Assert.assertEquals(new Modulus(5), base6.getBase(2));
        Assert.assertEquals(new Modulus(7), base6.getBase(3));
        Assert.assertEquals(new Modulus(11), base6.getBase(4));
        Assert.assertEquals(new Modulus(13), base6.getBase(5));
        Assert.assertEquals(new Modulus(17), base6.getBase(6));

        Assert.assertThrows(IllegalArgumentException.class, () -> base4.extend(new RnsBase(new long[]{7, 10, 11})));
    }


    @Test
    public void drop() {

        RnsBase base = new RnsBase(new long[]{3, 5, 7, 11});

        RnsBase base2 = base.drop();
        Assert.assertEquals(3, base2.getSize());
        for (int i = 0; i < base2.getSize(); i++) {
            Assert.assertEquals(base.getBase(i), base2.getBase(i));
        }

        RnsBase base3 = base2.drop().drop();
        Assert.assertEquals(1, base3.getSize());
        Assert.assertEquals(base.getBase(0), base3.getBase(0));
        // cannot drop size = 1 's RnsBase
        Assert.assertThrows(RuntimeException.class, base3::drop);
        Assert.assertThrows(RuntimeException.class, () -> base3.drop(3));
        Assert.assertThrows(RuntimeException.class, () -> base3.drop(5));

        RnsBase base4 = base.drop(5);
        Assert.assertEquals(3, base4.getSize());
        Assert.assertEquals(base.getBase(0), base4.getBase(0));
        Assert.assertEquals(base.getBase(2), base4.getBase(1));
        Assert.assertEquals(base.getBase(3), base4.getBase(2));

        Assert.assertThrows(IllegalArgumentException.class, () -> base4.drop(13));
        Assert.assertThrows(IllegalArgumentException.class, () -> base4.drop(0));
        base4.drop(7).drop(11);
        Assert.assertThrows(RuntimeException.class, () -> base4.drop(7).drop(11).drop(3));

    }


    private void rnsTest1(RnsBase base, long[] in, long[] out) {
        long[] inCopy = Arrays.copyOf(in, in.length);
        base.decompose(inCopy);
        Assert.assertArrayEquals(inCopy, out);

        base.compose(inCopy);
        Assert.assertArrayEquals(inCopy, in);
    }

    private void rnsTest2(RnsBase base, int count, long[] in, long[] out) {
        long[] inCopy = Arrays.copyOf(in, in.length);
        base.decomposeArray(inCopy, count);
        Assert.assertArrayEquals(inCopy, out);

        base.composeArray(inCopy, count);
        Assert.assertArrayEquals(inCopy, in);
    }


    @Test
    public void composeDecomposeArray() {

        {
            RnsBase base = new RnsBase(new long[]{2});
            rnsTest2(base, 1, new long[]{0}, new long[]{0});
            rnsTest2(base, 1, new long[]{1}, new long[]{1});
        }

        {
            RnsBase base = new RnsBase(new long[]{5});
            rnsTest2(base, 3, new long[]{0, 1, 2}, new long[]{0, 1, 2});
        }

        {
            RnsBase base = new RnsBase(new long[]{3, 5});
            rnsTest2(base, 1, new long[]{0, 0}, new long[]{0, 0});
            rnsTest2(base, 1, new long[]{2, 0}, new long[]{2, 2});
            rnsTest2(base, 1, new long[]{7, 0}, new long[]{1, 2});

            rnsTest2(base, 2, new long[]{0, 0, 0, 0}, new long[]{0, 0, 0, 0});
            rnsTest2(base, 2, new long[]{1, 0, 2, 0}, new long[]{1, 2, 1, 2});
            rnsTest2(base, 2, new long[]{7, 0, 8, 0}, new long[]{1, 2, 2, 3});
        }

        {
            RnsBase base = new RnsBase(new long[]{3, 5, 7});
            rnsTest2(base, 1, new long[]{0, 0, 0}, new long[]{0, 0, 0});
            rnsTest2(base, 1, new long[]{2, 0, 0}, new long[]{2, 2, 2});
            rnsTest2(base, 1, new long[]{7, 0, 0}, new long[]{1, 2, 0});
            rnsTest2(base, 2, new long[]{0, 0, 0, 0, 0, 0}, new long[]{0, 0, 0, 0, 0, 0});
            rnsTest2(base, 2, new long[]{1, 0, 0, 2, 0, 0}, new long[]{1, 2, 1, 2, 1, 2});
            rnsTest2(base, 2, new long[]{7, 0, 0, 8, 0, 0}, new long[]{1, 2, 2, 3, 0, 1});
            rnsTest2(base, 3, new long[]{7, 0, 0, 8, 0, 0, 9, 0, 0}, new long[]{1, 2, 0, 2, 3, 4, 0, 1, 2});
        }

        {
            // large number
            Modulus[] primes = Numth.getPrimes(1024 * 2, 60, 2);
            long[] inValues = new long[]{0xAAAAAAAAAAAL, 0xBBBBBBBBBBL,
                    0xCCCCCCCCCCL, 0xDDDDDDDDDDL,
                    0xEEEEEEEEEEL, 0xFFFFFFFFFFL};

            long[][] invaluesT = new long[][]{
                    {0xAAAAAAAAAAAL, 0xBBBBBBBBBBL},
                    {0xCCCCCCCCCCL, 0xDDDDDDDDDDL},
                    {0xEEEEEEEEEEL, 0xFFFFFFFFFFL},
            };
            // 注意 moduloUint 的函数签名， 这种情况必须使用辅助数组 把每一个 base-2^64 的值 给抠出来
            RnsBase base = new RnsBase(primes);
            rnsTest2(base, 3, inValues, new long[]{
                            UintArithmeticSmallMod.moduloUint(invaluesT[0], 2, primes[0]),
                            UintArithmeticSmallMod.moduloUint(invaluesT[1], 2, primes[0]),
                            UintArithmeticSmallMod.moduloUint(invaluesT[2], 2, primes[0]),

                            UintArithmeticSmallMod.moduloUint(invaluesT[0], 2, primes[1]),
                            UintArithmeticSmallMod.moduloUint(invaluesT[1], 2, primes[1]),
                            UintArithmeticSmallMod.moduloUint(invaluesT[2], 2, primes[1]),
                    }
            );
        }
        {
            // large number2
            Modulus[] primes = Numth.getPrimes(1024 * 2, 60, 2);
            long[] inValues = new long[]{0xAAAAAAAAAAAL, 0xBBBBBBBBBBL,
                    0xCCCCCCCCCCL, 0xDDDDDDDDDDL,
                    0xEEEEEEEEEEL, 0xFFFFFFFFFFL};

            RnsBase base = new RnsBase(primes);
            // 注意 moduloUint 的函数签名， 不使用辅助数组，直接取 原数组给定区间 [startIndex, startIndex + valueUint64Count)
            // 这种方法可以避免 重新 new 数组，这样底层的运算都是那一个 1维数组，但是在真正计算的时候，我们只取 对应区间的元素作为我们的 计算数据
            // 在这里我也突然明白了，为什么在 SEAL 的实现中，特别是在 UintArithmetic 的实现中，方法的参数里 都存在一个 uint64Count
            // 就是为了取给定数组的特定区间，在 C++ 里，可以非常方便的取数组的任一元素的地址 作为新的起始地址
            // 但是在 Java里，给定数组 long[] values = new long[10]，我们无法直接获取 values[2]的地址，所以我们增加一个字段 startIndex
            // 来解决这个问题

            rnsTest2(base, 3, inValues, new long[]{
                            UintArithmeticSmallMod.moduloUint(inValues, 0, 2, primes[0]),
                            UintArithmeticSmallMod.moduloUint(inValues, 2, 2, primes[0]),
                            UintArithmeticSmallMod.moduloUint(inValues, 4, 2, primes[0]),

                            UintArithmeticSmallMod.moduloUint(inValues, 0, 2, primes[1]),
                            UintArithmeticSmallMod.moduloUint(inValues, 2, 2, primes[1]),
                            UintArithmeticSmallMod.moduloUint(inValues, 4, 2, primes[1]),
                    }
            );
        }


    }


    @Test
    public void composeDecompose() {

        RnsBase base = new RnsBase(new long[]{2});
        rnsTest1(base, new long[]{0}, new long[]{0});
        rnsTest1(base, new long[]{1}, new long[]{1});


        base = new RnsBase(new long[]{5});
        rnsTest1(base, new long[]{0}, new long[]{0});
        rnsTest1(base, new long[]{1}, new long[]{1});
        rnsTest1(base, new long[]{2}, new long[]{2});
        rnsTest1(base, new long[]{3}, new long[]{3});
        rnsTest1(base, new long[]{4}, new long[]{4});

        base = new RnsBase(new long[]{3, 5});
        rnsTest1(base, new long[]{0, 0}, new long[]{0, 0});
        rnsTest1(base, new long[]{1, 0}, new long[]{1, 1});
        rnsTest1(base, new long[]{2, 0}, new long[]{2, 2});
        rnsTest1(base, new long[]{3, 0}, new long[]{0, 3});
        rnsTest1(base, new long[]{4, 0}, new long[]{1, 4});
        rnsTest1(base, new long[]{5, 0}, new long[]{2, 0});
        rnsTest1(base, new long[]{8, 0}, new long[]{2, 3});
        rnsTest1(base, new long[]{12, 0}, new long[]{0, 2});
        rnsTest1(base, new long[]{14, 0}, new long[]{2, 4});

        base = new RnsBase(new long[]{2, 3, 5});
        rnsTest1(base, new long[]{0, 0, 0}, new long[]{0, 0, 0});
        rnsTest1(base, new long[]{1, 0, 0}, new long[]{1, 1, 1});
        rnsTest1(base, new long[]{2, 0, 0}, new long[]{0, 2, 2});
        rnsTest1(base, new long[]{3, 0, 0}, new long[]{1, 0, 3});
        rnsTest1(base, new long[]{4, 0, 0}, new long[]{0, 1, 4});
        rnsTest1(base, new long[]{5, 0, 0}, new long[]{1, 2, 0});
        rnsTest1(base, new long[]{10, 0, 0}, new long[]{0, 1, 0});
        rnsTest1(base, new long[]{11, 0, 0}, new long[]{1, 2, 1});
        rnsTest1(base, new long[]{16, 0, 0}, new long[]{0, 1, 1});
        rnsTest1(base, new long[]{27, 0, 0}, new long[]{1, 0, 2});
        rnsTest1(base, new long[]{29, 0, 0}, new long[]{1, 2, 4});

        base = new RnsBase(new long[]{13, 37, 53, 97});
        rnsTest1(base, new long[]{0, 0, 0, 0}, new long[]{0, 0, 0, 0});
        rnsTest1(base, new long[]{1, 0, 0, 0}, new long[]{1, 1, 1, 1});
        rnsTest1(base, new long[]{2, 0, 0, 0}, new long[]{2, 2, 2, 2});
        rnsTest1(base, new long[]{12, 0, 0, 0}, new long[]{12, 12, 12, 12});
        rnsTest1(base, new long[]{321, 0, 0, 0}, new long[]{9, 25, 3, 30});

        // large number
        Modulus[] primes = Numth.getPrimes(1024 * 2, 60, 4);
        long[] inValues = new long[]{0xAAAAAAAAAAAL, 0xBBBBBBBBBBL, 0xCCCCCCCCCCL, 0xDDDDDDDDDDL};
        RnsBase base1 = new RnsBase(primes);

        rnsTest1(base1, inValues, new long[]{
                UintArithmeticSmallMod.moduloUint(inValues, inValues.length, primes[0]),
                UintArithmeticSmallMod.moduloUint(inValues, inValues.length, primes[1]),
                UintArithmeticSmallMod.moduloUint(inValues, inValues.length, primes[2]),
                UintArithmeticSmallMod.moduloUint(inValues, inValues.length, primes[3]),
        });
    }


}
