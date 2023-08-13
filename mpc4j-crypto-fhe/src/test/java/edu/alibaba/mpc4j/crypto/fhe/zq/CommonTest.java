package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.checkerframework.checker.units.qual.C;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Qixian Zhou
 * @date 2023/8/9
 */
public class CommonTest {


    @Test
    public void reversedBit64() {

        Assert.assertEquals(0L, Common.reverseBits(0L));
        Assert.assertEquals(1L << 63, Common.reverseBits(1L));
        Assert.assertEquals(1L << 32, Common.reverseBits(1L << 31));
        Assert.assertEquals(0xFFFFL << 32, Common.reverseBits(0xFFFFL << 16));
        Assert.assertEquals(0x0000FFFFFFFF0000L, Common.reverseBits(0x0000FFFFFFFF0000L));
        Assert.assertEquals(0x0000FFFF0000FFFFL, Common.reverseBits(0xFFFF0000FFFF0000L));

        Assert.assertEquals(0L, Common.reverseBits(0L, 0));
        Assert.assertEquals(0L, Common.reverseBits(0L, 1));
        Assert.assertEquals(0L, Common.reverseBits(0L, 32));
        Assert.assertEquals(0L, Common.reverseBits(0L, 64));

        Assert.assertEquals(0L, Common.reverseBits(1L, 0));
        Assert.assertEquals(1L, Common.reverseBits(1L, 1));
        Assert.assertEquals(1L << 31, Common.reverseBits(1L, 32));
        Assert.assertEquals(1L << 63, Common.reverseBits(1L, 64));

        Assert.assertEquals(0L, Common.reverseBits(1L << 31, 0));
        Assert.assertEquals(0L, Common.reverseBits(1L << 31, 1));
        Assert.assertEquals(1L, Common.reverseBits(1L << 31, 32));
        Assert.assertEquals(1L << 32, Common.reverseBits(1L << 31, 64));

        Assert.assertEquals(0L, Common.reverseBits(0xFFFFL << 16, 0));
        Assert.assertEquals(0L, Common.reverseBits(0xFFFFL << 16, 1));
        Assert.assertEquals(0xFFFFL, Common.reverseBits(0xFFFFL << 16, 32));
        Assert.assertEquals(0xFFFFL << 32, Common.reverseBits(0xFFFFL << 16, 64));

        Assert.assertEquals(0L, Common.reverseBits(0x0000FFFFFFFF0000L, 0));
        Assert.assertEquals(0L, Common.reverseBits(0x0000FFFFFFFF0000L, 1));
        Assert.assertEquals(0xFFFFL, Common.reverseBits(0x0000FFFFFFFF0000L, 32));
        Assert.assertEquals(0x0000FFFFFFFF0000L, Common.reverseBits(0x0000FFFFFFFF0000L, 64));

        Assert.assertEquals(0L, Common.reverseBits(0xFFFF0000FFFF0000L, 0));
        Assert.assertEquals(0L, Common.reverseBits(0xFFFF0000FFFF0000L, 1));
        Assert.assertEquals(0xFFFFL, Common.reverseBits(0xFFFF0000FFFF0000L, 32));
        Assert.assertEquals(0x0000FFFF0000FFFFL, Common.reverseBits(0xFFFF0000FFFF0000L, 64));
    }

    @Test
    public void reversedBit32() {

        Assert.assertEquals((0), Common.reverseBits((0)));
        Assert.assertEquals((0x80000000), Common.reverseBits((1)));
        Assert.assertEquals((0x40000000), Common.reverseBits((2)));
        Assert.assertEquals((0xC0000000), Common.reverseBits((3)));
        Assert.assertEquals((0x00010000), Common.reverseBits((0x00008000)));
        Assert.assertEquals((0xFFFF0000), Common.reverseBits((0x0000FFFF)));
        Assert.assertEquals((0x0000FFFF), Common.reverseBits((0xFFFF0000)));
        Assert.assertEquals((0x00008000), Common.reverseBits((0x00010000)));
        Assert.assertEquals((3), Common.reverseBits((0xC0000000)));
        Assert.assertEquals((2), Common.reverseBits((0x40000000)));
        Assert.assertEquals((1), Common.reverseBits((0x80000000)));
        Assert.assertEquals((0xFFFFFFFF), Common.reverseBits((0xFFFFFFFF)));
        // Reversing a 0-bit item should return 0
        Assert.assertEquals((0), Common.reverseBits((0xFFFFFFFF), 0));

        // Reversing a 32-bit item returns is same as normal reverse
        Assert.assertEquals((0), Common.reverseBits((0), 32));
        Assert.assertEquals((0x80000000), Common.reverseBits((1), 32));
        Assert.assertEquals((0x40000000), Common.reverseBits((2), 32));
        Assert.assertEquals((0xC0000000), Common.reverseBits((3), 32));
        Assert.assertEquals((0x00010000), Common.reverseBits((0x00008000), 32));
        Assert.assertEquals((0xFFFF0000), Common.reverseBits((0x0000FFFF), 32));
        Assert.assertEquals((0x0000FFFF), Common.reverseBits((0xFFFF0000), 32));
        Assert.assertEquals((0x00008000), Common.reverseBits((0x00010000), 32));
        Assert.assertEquals((3), Common.reverseBits((0xC0000000), 32));
        Assert.assertEquals((2), Common.reverseBits((0x40000000), 32));
        Assert.assertEquals((1), Common.reverseBits((0x80000000), 32));
        Assert.assertEquals((0xFFFFFFFF), Common.reverseBits((0xFFFFFFFF), 32));

        // 16-bit reversal
        Assert.assertEquals((0), Common.reverseBits((0), 16));
        Assert.assertEquals((0x00008000), Common.reverseBits((1), 16));
        Assert.assertEquals((0x00004000), Common.reverseBits((2), 16));
        Assert.assertEquals((0x0000C000), Common.reverseBits((3), 16));
        Assert.assertEquals((0x00000001), Common.reverseBits((0x00008000), 16));
        Assert.assertEquals((0x0000FFFF), Common.reverseBits((0x0000FFFF), 16));
        Assert.assertEquals((0x00000000), Common.reverseBits((0xFFFF0000), 16));
        Assert.assertEquals((0x00000000), Common.reverseBits((0x00010000), 16));
        Assert.assertEquals((3), Common.reverseBits((0x0000C000), 16));
        Assert.assertEquals((2), Common.reverseBits((0x00004000), 16));
        Assert.assertEquals((1), Common.reverseBits((0x00008000), 16));
        Assert.assertEquals((0x0000FFFF), Common.reverseBits((0xFFFFFFFF), 16));
    }


    @Test
    public void getMsbIndexTest() {

        long result;
        result = Common.getMsbIndex(1);
        Assert.assertEquals(0, result);
        result = Common.getMsbIndex(2);
        Assert.assertEquals(1, result);
        result = Common.getMsbIndex(3);
        Assert.assertEquals(1, result);
        result = Common.getMsbIndex(4);
        Assert.assertEquals(2, result);
        result = Common.getMsbIndex(16);
        Assert.assertEquals(4, result);
        result = Common.getMsbIndex(0xFFFFFFFFL);
        Assert.assertEquals(31, result);
        result = Common.getMsbIndex(0x100000000L);
        Assert.assertEquals(32, result);
        result = Common.getMsbIndex(0xFFFFFFFFFFFFFFFFL);
        Assert.assertEquals(63, result);
    }

    @Test
    public void safeArithmetic() {


        int posI32 = 5;
        int negI32 = -5;
        int posU32 = 6;

        long posU64Max = 0xFFFFFFFFFFFFFFFFL;
        long negI64 = -1;


        Assert.assertEquals(25, Common.mulSafe(posI32, posI32, false));
        Assert.assertEquals(25, Common.mulSafe(negI32, negI32, false));

        Assert.assertEquals(10, Common.addSafe(posI32, posI32, false));
        Assert.assertEquals(-10, Common.addSafe(negI32, negI32, false));
        Assert.assertEquals(0, Common.addSafe(posI32, negI32, false));

        Assert.assertEquals(10, Common.subSafe(posI32, negI32, false));
        Assert.assertEquals(-10, Common.subSafe(negI32, posI32, false));

        Assert.assertEquals(0, Common.subSafe(posU32, posU32, true));

        Assert.assertThrows(ArithmeticException.class, () -> Common.subSafe(0, posU32, true));
        Assert.assertThrows(ArithmeticException.class, () -> Common.subSafe(4, posU32, true));

        Assert.assertThrows(ArithmeticException.class, () -> Common.addSafe(posU64Max, 1, true));

        Assert.assertEquals(posU64Max, Common.addSafe(posU64Max, 0, true));
        Assert.assertThrows(ArithmeticException.class, () -> Common.mulSafe(posU64Max, posU64Max, true));

        Assert.assertEquals(0, Common.mulSafe(0, posU64Max, true));
        Assert.assertEquals(1, Common.mulSafe(negI64, negI64, false));

        // multi numbers
        Assert.assertEquals(15, Common.addSafe(posI32, -posI32, false, posI32, posI32, posI32));
        Assert.assertEquals(6, Common.addSafe(0, -posI32, false, posI32, 1, posI32));
        Assert.assertEquals(0, Common.mulSafe(posI32, posU32, false, posI32, 0, posI32));
        Assert.assertEquals(625, Common.mulSafe(posI32, posI32, false, posI32, posI32));

        Assert.assertThrows(ArithmeticException.class, () -> Common.mulSafe(posI32, posI32, false,
                posI32, posI32,posI32, posI32,posI32, posI32,posI32, posI32,posI32, posI32,posI32, posI32
                ));

    }

}
