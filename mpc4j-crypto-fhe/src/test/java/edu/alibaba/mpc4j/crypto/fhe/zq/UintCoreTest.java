package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * @author Qixian Zhou
 * @date 2023/8/4
 */
public class UintCoreTest {


    private final static int MAX_LOOP_NUM = 10000;

    @Test
    public void getPowerOfTwoTest() {

        Assert.assertEquals(-1, UintCore.getPowerOfTwo(0));
        Assert.assertEquals(0, UintCore.getPowerOfTwo(1));
        Assert.assertEquals(1, UintCore.getPowerOfTwo(2));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(3));
        Assert.assertEquals(2, UintCore.getPowerOfTwo(4));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(5));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(6));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(7));
        Assert.assertEquals(3, UintCore.getPowerOfTwo(8));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(15));
        Assert.assertEquals(4, UintCore.getPowerOfTwo(16));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(17));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(255));
        Assert.assertEquals(8, UintCore.getPowerOfTwo(256));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(257));
        Assert.assertEquals(10, UintCore.getPowerOfTwo(1 << 10));
        Assert.assertEquals(30, UintCore.getPowerOfTwo(1 << 30));
        Assert.assertEquals(32, UintCore.getPowerOfTwo(1L << 32));
        Assert.assertEquals(62, UintCore.getPowerOfTwo(1L << 62));
        Assert.assertEquals(63, UintCore.getPowerOfTwo(1L << 63));
    }


    @Test
    public void isBitSetUintTest() {

        long[] ptr = new long[2];
        for (int i = 0; i < 128; i++) {
            Assert.assertFalse(UintCore.isBitSetUint(ptr, 2, i));
        }
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        for (int i = 0; i < 128; i++) {
            Assert.assertTrue(UintCore.isBitSetUint(ptr, 2, i));
        }

        ptr[0] = 0x0000000000000001L;
        ptr[1] = 0x8000000000000000L;
        for (int i = 0; i < 128; i++) {
            if (i == 0 || i == 127) {
                Assert.assertTrue(UintCore.isBitSetUint(ptr,2, i));
            }else {
                Assert.assertFalse(UintCore.isBitSetUint(ptr, 2, i));
            }
        }
    }

    @Test
    public void isZeroUintTest() {

        long[] ptr = new long[1];
        ptr[0] = 1;
        Assert.assertFalse(UintCore.isZeroUint(ptr, 1));
        ptr[0] = 0;
        Assert.assertTrue(UintCore.isZeroUint(ptr, 1));

        ptr = new long[2];
        ptr[0] = 0x8000000000000000L;
        ptr[1] = 0x8000000000000000L;
        Assert.assertFalse(UintCore.isZeroUint(ptr, 2));
        ptr[0] = 0;
        ptr[1] = 0x8000000000000000L;
        Assert.assertFalse(UintCore.isZeroUint(ptr, 2));
        ptr[0] = 0x8000000000000000L;
        ptr[1] = 0;
        Assert.assertFalse(UintCore.isZeroUint(ptr, 2));
        ptr[0] = 0;
        ptr[1] = 0;
        Assert.assertTrue(UintCore.isZeroUint(ptr, 2));
    }


    @Test
    public void isEqualUintTest() {
        long[] ptr = new long[1];

        ptr[0] = 1;
        Assert.assertTrue(UintCore.isEqualUint(ptr, 1, 1));
        Assert.assertFalse(UintCore.isEqualUint(ptr, 1, 0));
        Assert.assertFalse(UintCore.isEqualUint(ptr, 1, 2));

        ptr = new long[2];
        ptr[0] = 1;
        ptr[1] = 1;
        Assert.assertFalse(UintCore.isEqualUint(ptr, 2, 1));

        ptr[0] = 1;
        ptr[1] = 0;
        Assert.assertTrue(UintCore.isEqualUint(ptr, 2, 1));

        ptr[0] = 0x1234567887654321L;
        ptr[1] = 0;
        Assert.assertTrue(UintCore.isEqualUint(ptr, 2, 0x1234567887654321L));
        Assert.assertFalse(UintCore.isEqualUint(ptr, 2, 0x2234567887654321L));
    }


    @Test
    public void compareUintTest() {

        long[] ptr1 = new long[2];
        long[] ptr2 = new long[2];
        Assert.assertEquals(0, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 0x1234567887654321L;
        ptr1[1] = 0x8765432112345678L;
        ptr2[0] = 0x1234567887654321L;
        ptr2[1] = 0x8765432112345678L;
        Assert.assertEquals(0, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 1;
        ptr1[1] = 0;
        ptr2[0] = 2;
        ptr2[1] = 0;
        Assert.assertEquals(-1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 1;
        ptr1[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 2;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        Assert.assertEquals(-1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 2;
        ptr1[1] = 0;
        ptr2[0] = 1;
        ptr2[1] = 0;
        Assert.assertEquals(1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 2;
        ptr1[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 1;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        Assert.assertEquals(1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr1[1] = 0x0000000000000003L;
        ptr2[0] = 0x0000000000000000;
        ptr2[1] = 0x0000000000000002L;
        UintCore.compareUint(ptr1, ptr2, 2);
        Assert.assertEquals(1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));
    }


    @Test
    public void setUintTest() {

        long[] ptr = new long[1];

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(1 , 1, ptr);
        Assert.assertEquals(1, ptr[0]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(0x1234567812345678L, 1, ptr);
        Assert.assertEquals(0x1234567812345678L, ptr[0]);

        ptr = new long[2];
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(1 , 2, ptr);
        Assert.assertEquals(1, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(0x1234567812345678L, 2, ptr);
        Assert.assertEquals(0x1234567812345678L, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
    }

    @Test
    public void setUintTest2() {
        long[] ptr1 = new long[1];
        long[] ptr2 = new long[1];

        ptr1[0] = 0x1234567887654321L;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1 , 1, ptr2);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);

        ptr1[0] = 0x1231231231231231L;
        UintCore.setUint(ptr1 , 1, ptr1);
        Assert.assertEquals(0x1231231231231231L, ptr1[0]);

        ptr1 = new long[2];
        ptr2 = new long[2];
        ptr1[0] = 0x1234567887654321L;
        ptr1[1] = 0x8765432112345678L;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1, 2, ptr2);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);
        Assert.assertEquals(0x8765432112345678L, ptr2[1]);

        ptr1[0] = 0x1231231231231321L;
        ptr1[1] = 0x3213213213213211L;
        UintCore.setUint(ptr1, 2, ptr2);
        Assert.assertEquals(0x1231231231231321L, ptr2[0]);
        Assert.assertEquals(0x3213213213213211L, ptr2[1]);
    }


    @Test
    public void setUintTest3() {

        long[] ptr1 = new long[1];
        ptr1[0] = 0x1234567887654321L;
        long[] ptr2 = new long[1];
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;

        UintCore.setUint(ptr1, 1, ptr2, 1);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);

        ptr1[0] = 0x1231231231231231L;
        UintCore.setUint(ptr1, 1, ptr2, 1);
        Assert.assertEquals(0x1231231231231231L, ptr2[0]);

        ptr1 = new long[2];
        ptr2 = new long[2];
        ptr1[0] = 0x1234567887654321L;
        ptr1[1] = 0x8765432112345678L;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1, 1, ptr2,  2);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1, 2, ptr2,  2);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);
        Assert.assertEquals(0x8765432112345678L, ptr2[1]);

        ptr1[0] = 0x1231231231231321L;
        ptr1[1] = 0x3213213213213211L;
        UintCore.setUint(ptr1, 2, ptr2,  2);
        Assert.assertEquals(0x1231231231231321L, ptr2[0]);
        Assert.assertEquals(0x3213213213213211L, ptr2[1]);

        UintCore.setUint(ptr1, 1, ptr2,  2);
        Assert.assertEquals(0x1231231231231321L, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
    }



    @Test
    public void getSignificantUint64CountUintTest() {

        long[] values = new long[]{1, 0};
        int res;
        res = UintCore.getSignificantUint64CountUint(values,2);
        Assert.assertEquals(1, res);

        values[0] = 0;
        values[1] = 0;
        UintCore.getSignificantUint64CountUint(values, 2);
        Assert.assertEquals(0, UintCore.getSignificantUint64CountUint(values, 2));

        values[0] = 2;
        values[1] = 0;
        Assert.assertEquals(1, UintCore.getSignificantUint64CountUint(values, 2));

        values[0] = 0xFFFFFFFFFFFFFFFFL;
        values[1] = 0;
        Assert.assertEquals(1, UintCore.getSignificantUint64CountUint(values, 2));

        values[0] = 0;
        values[1] = 1;
        Assert.assertEquals(2, UintCore.getSignificantUint64CountUint(values, 2));

        values[0] = 0xFFFFFFFFFFFFFFFFL;
        values[1] = 1;
        Assert.assertEquals(2, UintCore.getSignificantUint64CountUint(values, 2));

        values[0] = 0xFFFFFFFFFFFFFFFFL;
        values[1] = 0x8000000000000000L;
        Assert.assertEquals(2, UintCore.getSignificantUint64CountUint(values, 2));

        values[0] = 0xFFFFFFFFFFFFFFFFL;
        values[1] = 0xFFFFFFFFFFFFFFFFL;
        Assert.assertEquals(2, UintCore.getSignificantUint64CountUint(values, 2));

    }

    @Test
    public void getSignificantBitCountUintTest() {

        long[] values = new long[]{0, 0};
        Assert.assertEquals(0, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{1, 0};
        Assert.assertEquals(1, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{2, 0};
        Assert.assertEquals(2, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{3, 0};
        Assert.assertEquals(2, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{29, 0};
        Assert.assertEquals(5, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{4, 0};
        Assert.assertEquals(3, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{(1L << 63) - 1, 0};
        Assert.assertEquals(63, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0, 1};
        Assert.assertEquals(65, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0xFFFFFFFFFFFFFFFFL, 1};
        Assert.assertEquals(65, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0xFFFFFFFFFFFFFFFFL, 0x7000000000000000L};
        Assert.assertEquals(127, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L};
        Assert.assertEquals(128, UintCore.getSignificantBitCountUint(values, 2));

    }


    @Test
    public void getBitCountUint() {
        long[] a = new long[] {2424242424242424L, 6868686868686868L, 3434343434343434L};

        int b = UintCore.getSignificantBitCountUint(a, 3);
        System.out.println(b);
    }


    @Test
    public void someEasy() {
        long a = -2;
        long b = -1;
        long c = a - b;
        System.out.println(c);
    }


    @Test
    public void bitsTest() {
        Random random = new Random();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            long a = -Math.abs(random.nextLong());
            assert UintCore.getSignificantBitCount(a) == 64;
        }

    }
}