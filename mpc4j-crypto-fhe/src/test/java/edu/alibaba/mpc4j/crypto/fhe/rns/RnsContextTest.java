package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * @author Qixian Zhou
 * @date 2023/8/15
 */
public class RnsContextTest {

    private final static int MAX_LOOP_NUM = 1000;

    @Test
    public void constructor() {

        long[] moduli = new long[]{2};
        RnsContext rns = new RnsContext(moduli);

        moduli = new long[]{2, 3};
        rns = new RnsContext(moduli);

        moduli = new long[]{4, 15, 1153};
        rns = new RnsContext(moduli);

        moduli = new long[]{};
        long[] finalModuli = moduli;
        Assert.assertThrows(AssertionError.class, () -> new RnsContext(finalModuli));

        moduli = new long[]{2, 4};
        long[] finalModuli1 = moduli;
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsContext(finalModuli1));

        moduli = new long[]{2, 3, 5, 30};
        long[] finalModuli2 = moduli;
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsContext(finalModuli2));
    }


    @Test
    public void garner() {
        long[] moduli = new long[]{4, 15, 1153};
        RnsContext rns = new RnsContext(moduli);
        for (int i = 0; i < 3; i++) {
            // 51885 59956 26520
            long[] gi = rns.getGarner(i);
            // g^* * (g^*)^{-1} = 1 mod qi
            Assert.assertEquals(UintArithmeticSmallMod.moduloUint(gi, gi.length, rns.moduli[i]), 1);
        }

        // random test
        int size = 5;
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            Modulus[] modulus = Numth.getPrimes(8, 61, size);
            rns = new RnsContext(modulus);
            // test garner
            for (int j = 0; j < 3; j++) {
                // 51885 59956 26520
                long[] gi = rns.getGarner(j);
                // g^* * (g^*)^{-1} = 1 mod qi
                Assert.assertEquals(UintArithmeticSmallMod.moduloUint(gi, gi.length, rns.moduli[j]), 1);
            }
        }
    }


    @Test
    public void modulus() {

        long[] moduli = new long[]{2};
        RnsContext rns = new RnsContext(moduli);
        Assert.assertEquals(convertUintToBigInteger(rns.getBaseProd()), BigInteger.valueOf(2));

        moduli = new long[]{2, 5};
        rns = new RnsContext(moduli);
        Assert.assertEquals(convertUintToBigInteger(rns.getBaseProd()), BigInteger.valueOf(2 * 5));


        moduli = new long[]{4, 15, 1153};
        rns = new RnsContext(moduli);
        Assert.assertEquals(convertUintToBigInteger(rns.getBaseProd()), BigInteger.valueOf(4 * 15 * 1153));

        // random test
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            Modulus[] modulus = Numth.getPrimes(8, 61, 5);
            rns = new RnsContext(modulus);
            BigInteger expected = mulManyModuli(rns.base);
            BigInteger real = convertUintToBigInteger(rns.getBaseProd());
            Assert.assertEquals(expected, real);
        }
    }


    @Test
    public void deconstructLiftRandom() {
        int size = 5;
        Random random = new Random();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            Modulus[] modulus = Numth.getPrimes(8, 61, size);
            long[] moduli = new long[modulus.length];
            for (int j = 0; j < modulus.length; j++) {
                moduli[j] = modulus[j].getValue();
            }
            RnsContext rns = new RnsContext(moduli);
            for (int j = 0; j < MAX_LOOP_NUM; j++) {
                long[] value = new long[size];
                Arrays.fill(value, Math.abs(random.nextInt()));
                long[] rests = rns.decompose(value);
                long[] recover = rns.compose(rests);
                Assert.assertArrayEquals(recover, value);
            }
        }
    }


    @Test
    public void deconstructLift() {


        long[] moduli = new long[]{4, 15, 1153};
        long product = 4L * 15 * 1153;
        long[] zeros = new long[3];

        RnsContext rns = new RnsContext(moduli);
        long[] value = new long[1];
        long[] rests = rns.decompose(value);
        Assert.assertArrayEquals(zeros, rests);
        long[] liftRes = rns.compose(rests);
        Assert.assertEquals(convertUintToBigInteger(value), convertUintToBigInteger(liftRes));

        value = new long[]{4};
        rests = rns.decompose(value);
        Assert.assertArrayEquals(new long[]{0, 4, 4}, rests);
        liftRes = rns.compose(rests);
        Assert.assertEquals(convertUintToBigInteger(value), convertUintToBigInteger(liftRes));

        value = new long[]{15};
        rests = rns.decompose(value);
        Assert.assertArrayEquals(new long[]{3, 0, 15}, rests);
        liftRes = rns.compose(rests);
        Assert.assertEquals(convertUintToBigInteger(value), convertUintToBigInteger(liftRes));

        value = new long[]{1153};
        rests = rns.decompose(value);
        Assert.assertArrayEquals(new long[]{1, 13, 0}, rests);
        liftRes = rns.compose(rests);
        Assert.assertEquals(convertUintToBigInteger(value), convertUintToBigInteger(liftRes));

        value = new long[]{product - 1};
        rests = rns.decompose(value);
        Assert.assertArrayEquals(new long[]{3, 14, 1152}, rests);
        liftRes = rns.compose(rests);
        Assert.assertEquals(convertUintToBigInteger(value), convertUintToBigInteger(liftRes));

    }


    private BigInteger mulManyModuli(long[] moduli) {

        BigInteger res = BigInteger.ONE;

        for (long m : moduli) {
            res = res.multiply(BigInteger.valueOf(m));
        }
        return res;
    }


    private BigInteger convertUintToBigInteger(long[] values) {

        BigInteger base = BigInteger.valueOf(2).pow(64);
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < values.length; i++) {

            String hexVal = Long.toHexString(values[i]);
            BigInteger cur = new BigInteger(hexVal, 16);

            sum = sum.add(base.pow(i).multiply(cur));
        }

        return sum;
    }


}
