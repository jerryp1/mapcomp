package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * @author Qixian Zhou
 * @date 2023/7/31
 */
public class LongArithmeticTest {


    private static final int MAX_LOOP_NUM = 10000;




    @Test
    public void leftShiftUint189Test1() {

        long[] values = new long[] {3, 0, 0};
        for (int i = 0; i <= 128; i++) {
            long[] result = LongArithmetic.leftShiftUint189(values, i);
            BigInteger truth = LongArithmetic.convertLongArrayToBigInteger(values).shiftLeft(i);
            BigInteger res = LongArithmetic.convertLongArrayToBigInteger(result);
            Assert.assertEquals(truth, res);
//            if (truth.compareTo(res) != 0) {
//                System.out.println("error, i = " + i + ", truth = " + truth + ", res = " + res);
//            }
        }
    }
    // 比较严苛的测试，应该可以证明 base-2^63 移位算法的正确性
    @Test
    public void leftShiftUint189Test2() {
        long[] values = new long[] {0, 0, 0};
        Random random = new Random();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            values[0] = Math.abs(random.nextLong());
            // 大部分情况下，是为了把当前数据左移到 129 bits， 与 2^128 对齐
            int shiftAmount = 129 - LongArithmetic.getUint63ValueBitCount(values[0]);
            // 为了更严格的测试，只要 left shift 后的数据不会超过 189 bits，结果就应该是正确的
            int maxShiftAmont = 189 - LongArithmetic.getUint63ValueBitCount(values[0]);
            for (; shiftAmount <= maxShiftAmont; shiftAmount++) {
                long[] result = LongArithmetic.leftShiftUint189(values, shiftAmount);
                BigInteger truth = LongArithmetic.convertLongArrayToBigInteger(values).shiftLeft(shiftAmount);
                BigInteger res = LongArithmetic.convertLongArrayToBigInteger(result);
                Assert.assertEquals(truth, res);
            }
        }
    }

    // test cases come from LeftShiftUInt192 in native/tests/seal/util/uintarith.cpp of SEAL-4.0
    @Test
    public void leftShiftUint189Test3() {
        long[] zeros = new long[3];
        long[] ptr = new long[3];
        long[] ptr2 = new long[3];

        ptr2 = LongArithmetic.leftShiftUint189(ptr, 0);
        Assert.assertArrayEquals(ptr2, zeros);

        ptr2 = LongArithmetic.leftShiftUint189(ptr, 10);
        Assert.assertArrayEquals(ptr2, zeros);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0x6AAAAAAAAAAAAAAAL; // change the highest bit into 0, avoiding negative number
        ptr[2] = 0x4DCDCDCDCDCDCDCDL;
        ptr2 = LongArithmetic.leftShiftUint189(ptr, 0);
        Assert.assertArrayEquals(ptr, ptr2);


        ptr2 = LongArithmetic.leftShiftUint189(ptr, 63);
        Assert.assertEquals(ptr2[0], 0);
        Assert.assertEquals(ptr2[1], 0x5555555555555555L);
        Assert.assertEquals(ptr2[2], 0x6AAAAAAAAAAAAAAAL);

        ptr2 = LongArithmetic.leftShiftUint189(ptr, 126);
        Assert.assertEquals(ptr2[0], 0);
        Assert.assertEquals(ptr2[1], 0);
        Assert.assertEquals(ptr2[2], 0x5555555555555555L);
        // 注意，移动 189 后还是 低 63 bits 的值，seal 的实现也是移动 192 后，还是 低 64 bits 的值
        ptr2 = LongArithmetic.leftShiftUint189(ptr, 189);
        Assert.assertEquals(ptr2[0], 0);
        Assert.assertEquals(ptr2[1], 0);
        Assert.assertEquals(ptr2[2], 0x5555555555555555L);


        ptr2 = LongArithmetic.leftShiftUint189(ptr, 188);
        Assert.assertEquals(ptr2[0], 0);
        Assert.assertEquals(ptr2[1], 0);
        Assert.assertEquals(ptr2[2], (0x5555555555555555L << 62) & Long.MAX_VALUE);

        ptr2 = LongArithmetic.leftShiftUint189(ptr, 64);
        System.out.println( Long.toHexString(ptr2[0]));
        System.out.println( Long.toHexString(ptr2[1]));
        System.out.println( Long.toHexString(ptr2[2]));
    }


    @Test
    public void leftShiftUint189Test4() {

        long[] diff = new long[] {0, 0, 8388608L};
        long[] numerator = new long[] {0, 0, 2155872256L};

        LongArithmetic.leftShiftUint189(diff, 0, numerator);
        System.out.println(numerator[2]);

        numerator = LongArithmetic.leftShiftUint189(diff, 0);
        System.out.println(numerator[2]);

    }


    @Test
    public void rightShiftUint189Test2() {
        long[] values = new long[] {0, 0, 0};
        Random random = new Random();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            values[0] = Math.abs(random.nextLong());
            values[1] = Math.abs(random.nextLong());
            values[2] = Math.abs(random.nextLong());
            // 为了更严格的测试，只要 left shift 后的数据不会超过 189 bits，结果就应该是正确的
            int maxShiftAmont = 189 - LongArithmetic.getSignificantBitCountUint63(values, 3);
            for (int shiftAmount = 0; shiftAmount <= maxShiftAmont; shiftAmount++) {
                long[] result = LongArithmetic.rightShiftUint189(values, shiftAmount);
                BigInteger truth = LongArithmetic.convertLongArrayToBigInteger(values).shiftRight(shiftAmount);
                BigInteger res = LongArithmetic.convertLongArrayToBigInteger(result);
                Assert.assertEquals(truth, res);
            }
        }
    }

    @Test
    public void rightShiftUint189Test3() {
        long[] zeros = new long[3];
        long[] ptr = new long[3];
        long[] ptr2;

        ptr2 = LongArithmetic.rightShiftUint189(ptr, 0);
        Assert.assertArrayEquals(ptr2, zeros);

        ptr2 = LongArithmetic.rightShiftUint189(ptr, 10);
        Assert.assertArrayEquals(ptr2, zeros);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0x6AAAAAAAAAAAAAAAL; // change the highest bit into 0, avoiding negative number
        ptr[2] = 0x4DCDCDCDCDCDCDCDL;
        ptr2 = LongArithmetic.rightShiftUint189(ptr, 0);
        Assert.assertArrayEquals(ptr2, ptr);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0x6AAAAAAAAAAAAAAAL; // change the highest bit into 0, avoiding negative number
        ptr[2] = 0x4DCDCDCDCDCDCDCDL;
        ptr2 = LongArithmetic.rightShiftUint189(ptr, 1);

        BigInteger truth = LongArithmetic.convertLongArrayToBigInteger(ptr).shiftRight(1);
        BigInteger ptr2Value = LongArithmetic.convertLongArrayToBigInteger(ptr2);
        long[] truthArray = LongArithmetic.convertBigIntegerToLongArray(truth);
        Assert.assertArrayEquals(ptr2, truthArray);
        Assert.assertEquals(ptr2Value, truth);

        ptr2 = LongArithmetic.rightShiftUint189(ptr, 2);
        truth = LongArithmetic.convertLongArrayToBigInteger(ptr).shiftRight(2);
        ptr2Value = LongArithmetic.convertLongArrayToBigInteger(ptr2);
        truthArray = LongArithmetic.convertBigIntegerToLongArray(truth);
        Assert.assertArrayEquals(ptr2, truthArray);
        Assert.assertEquals(ptr2Value, truth);

        ptr2 = LongArithmetic.rightShiftUint189(ptr, 63);
        Assert.assertEquals(0x6AAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0x4DCDCDCDCDCDCDCDL, ptr2[1]);
        Assert.assertEquals(0, ptr2[2]);

        ptr2 = LongArithmetic.rightShiftUint189(ptr, 126);
        Assert.assertEquals(0x4DCDCDCDCDCDCDCDL, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        Assert.assertEquals(0, ptr2[2]);

        ptr2 = LongArithmetic.rightShiftUint189(ptr, 188);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        Assert.assertEquals(0, ptr2[2]);
    }


    @Test
    public void getSignificantBitCountUint63Test() {

        long[] values = new long[] {0, 0};
        Assert.assertEquals(0, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {1, 0};
        Assert.assertEquals(1, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {2, 0};
        Assert.assertEquals(2, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {3, 0};
        Assert.assertEquals(2, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {29, 0};
        Assert.assertEquals(5, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {4, 0};
        Assert.assertEquals(3, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {(1L <<63) - 1, 0};
        Assert.assertEquals(63, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {0, 1};
        Assert.assertEquals(64, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {(1L << 63) - 1, 1};
        Assert.assertEquals(64, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {(1L << 63) - 1, 0x7000000000000000L};
        Assert.assertEquals(126, LongArithmetic.getSignificantBitCountUint63(values, 2));

        values = new long[] {(1L << 63) - 1, (1L << 63) - 1};
        Assert.assertEquals(126, LongArithmetic.getSignificantBitCountUint63(values, 2));



    }
    @Test
    public void divideRoundUpTest() {
        Assert.assertEquals(0, LongArithmetic.divideRoundUp(0, 4));
        Assert.assertEquals(1, LongArithmetic.divideRoundUp(1, 4));
        Assert.assertEquals(1, LongArithmetic.divideRoundUp(2, 4));
        Assert.assertEquals(1, LongArithmetic.divideRoundUp(3, 4));
        Assert.assertEquals(1, LongArithmetic.divideRoundUp(4, 4));
        Assert.assertEquals(2, LongArithmetic.divideRoundUp(5, 4));
        Assert.assertEquals(2, LongArithmetic.divideRoundUp(6, 4));
        Assert.assertEquals(2, LongArithmetic.divideRoundUp(7, 4));
        Assert.assertEquals(2, LongArithmetic.divideRoundUp(8, 4));
        Assert.assertEquals(3, LongArithmetic.divideRoundUp(9, 4));
        Assert.assertEquals(3, LongArithmetic.divideRoundUp(12, 4));
        Assert.assertEquals(4, LongArithmetic.divideRoundUp(13, 4));
    }


    @Test
    public void subUint63Test1() {
        long[] result;

        result = LongArithmetic.subUint63(0, 0, 0);
        Assert.assertEquals(result[0], 0);
        Assert.assertEquals(result[1], 0);// no borrow

        result = LongArithmetic.subUint63(1, 1, 0);
        Assert.assertEquals(result[0], 0);
        Assert.assertEquals(result[1], 0);// no borrow
        // 1 - 0 - 1
        result = LongArithmetic.subUint63(1, 0, 1);
        Assert.assertEquals(result[0], 0);
        Assert.assertEquals(result[1], 0);// no borrow
        // 0 - 1 - 1 = -2 = LongM
        result = LongArithmetic.subUint63(0, 1, 1);
        Assert.assertEquals(result[0], (-2) & Long.MAX_VALUE );
        Assert.assertEquals(result[1], 1);//  borrow = 1

        result = LongArithmetic.subUint63(1, 1, 1);
        Assert.assertEquals(result[0], Long.MAX_VALUE);
        Assert.assertEquals(result[1], 1);//  borrow = 1

        result = LongArithmetic.subUint63(Long.MAX_VALUE, 1, 0);
        Assert.assertEquals(result[0], Long.MAX_VALUE - 1);
        Assert.assertEquals(result[1], 0);

        result = LongArithmetic.subUint63(1, Long.MAX_VALUE, 0);
        Assert.assertEquals(result[0], 2);
        Assert.assertEquals(result[1], 1);//  borrow = 1

        result = LongArithmetic.subUint63(1, Long.MAX_VALUE, 1);
        Assert.assertEquals(result[0], 1);
        Assert.assertEquals(result[1], 1);//  borrow = 1

        result = LongArithmetic.subUint63(2, Long.MAX_VALUE - 1, 0);
        Assert.assertEquals(result[0], 4);
        Assert.assertEquals(result[1], 1);//  borrow = 1

        result = LongArithmetic.subUint63(2, Long.MAX_VALUE - 1, 1);
        Assert.assertEquals(result[0], 3);
        Assert.assertEquals(result[1], 1);//  borrow = 1
    }

    @Test
    public void subUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long[] ptr3;

        ptr3 = LongArithmetic.subUint(ptr, ptr2, 2);
        assert ptr3.length == 2 + 1;
        long borrow = ptr3[ptr3.length - 1];
        Assert.assertEquals(borrow, 0);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);


        ptr[0] = Long.MAX_VALUE;
        ptr[1] = Long.MAX_VALUE;
        Arrays.fill(ptr2, 0);

        ptr3 = LongArithmetic.subUint(ptr, ptr2, 2);
        assert ptr3.length == 2 + 1;
        borrow = ptr3[ptr3.length - 1];
        Assert.assertEquals(borrow, 0);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[0]);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[1]);

        ptr[0] = Long.MAX_VALUE;
        ptr[1] = Long.MAX_VALUE;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.subUint(ptr, ptr2, 2);
        assert ptr3.length == 2 + 1;
        borrow = ptr3[ptr3.length - 1];
        Assert.assertEquals(borrow, 0);
        Assert.assertEquals(Long.MAX_VALUE - 1, ptr3[0]);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[1]);

        ptr[0] = 0;
        ptr[1] = 0;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.subUint(ptr, ptr2, 2);
        assert ptr3.length == 2 + 1;
        borrow = ptr3[ptr3.length - 1];
        Assert.assertEquals(borrow, 1);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[0]);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[1]);

        ptr[0] = Long.MAX_VALUE;
        ptr[1] = Long.MAX_VALUE;
        ptr2[0] = Long.MAX_VALUE;
        ptr2[1] = Long.MAX_VALUE;
        ptr3 = LongArithmetic.subUint(ptr, ptr2, 2);
        assert ptr3.length == 2 + 1;
        borrow = ptr3[ptr3.length - 1];
        Assert.assertEquals(borrow, 0);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        ptr[0] = Long.MAX_VALUE - 1;
        ptr[1] = Long.MAX_VALUE;
        ptr2[0] = Long.MAX_VALUE;
        ptr2[1] = Long.MAX_VALUE;
        ptr3 = LongArithmetic.subUint(ptr, ptr2, 2);
        assert ptr3.length == 2 + 1;
        borrow = ptr3[ptr3.length - 1];
        Assert.assertEquals(borrow, 1);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[0]);// -1
        Assert.assertEquals(Long.MAX_VALUE, ptr3[1]); // -1

        ptr[0] = 0;
        ptr[1] = 1;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.subUint(ptr, ptr2, 2);
        assert ptr3.length == 2 + 1;
        borrow = ptr3[ptr3.length - 1];
        Assert.assertEquals(borrow, 0);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[0]);// -1
        Assert.assertEquals(0, ptr3[1]); // -1

        // 7 parameter
        ptr[0] = 0;
        ptr[1] = 1;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.subUint(ptr, 2, ptr2, 1,  0, 2);
        assert ptr3.length == 2 + 1;
        borrow = ptr3[ptr3.length - 1];
        Assert.assertEquals(borrow, 0);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[0]);// -1
        Assert.assertEquals(0, ptr3[1]); // -1
    }


    @Test
    public void addUint63Test() {
        long[] result;
        long carry;

        result = LongArithmetic.addUint63(0, 0);
        assert result.length == 2;
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, result[0]);

        result = LongArithmetic.addUint63(1, 1);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(2, result[0]);

        result = LongArithmetic.addUint63(1, 1);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(2, result[0]);

        result = LongArithmetic.addUint63(1, 0);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(1, result[0]);

        result = LongArithmetic.addUint63(0, 1);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(1, result[0]);

        result = LongArithmetic.addUint63(Long.MAX_VALUE, 1);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, result[0]);

        result = LongArithmetic.addUint63(1, Long.MAX_VALUE);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, result[0]);

        result = LongArithmetic.addUint63(2, Long.MAX_VALUE - 1);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, result[0]);

        result = LongArithmetic.addUint63(0x7F0F0F0F0F0F0F0FL, 0x00F0F0F0F0F0F0F0L);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(Long.MAX_VALUE, result[0]);
    }

    @Test
    public void addUint63WithCarryTest() {
        long[] result;
        long carry;

        result = LongArithmetic.addUint63(0, 0, 0);
        assert result.length == 2;
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, result[0]);

        result = LongArithmetic.addUint63(1, 1, 0);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(2, result[0]);

        result = LongArithmetic.addUint63(1, 0, 1);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(2, result[0]);

        result = LongArithmetic.addUint63(0, 1, 1);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(2, result[0]);

        result = LongArithmetic.addUint63(1, 1, 1);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(3, result[0]);

        result = LongArithmetic.addUint63(Long.MAX_VALUE, 1, 0);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, result[0]);

        result = LongArithmetic.addUint63(1, Long.MAX_VALUE, 0);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, result[0]);

        result = LongArithmetic.addUint63(1, Long.MAX_VALUE, 1);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(1, result[0]);

        result = LongArithmetic.addUint63(2, Long.MAX_VALUE - 1, 0);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, result[0]);

        result = LongArithmetic.addUint63(2, Long.MAX_VALUE - 1, 1);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(1, result[0]);

        result = LongArithmetic.addUint63(0x7F0F0F0F0F0F0F0FL, 0x00F0F0F0F0F0F0F0L, 0);
        carry = result[1];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(Long.MAX_VALUE, result[0]);

        result = LongArithmetic.addUint63(0x7F0F0F0F0F0F0F0FL, 0x00F0F0F0F0F0F0F0L, 1);
        carry = result[1];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, result[0]);
    }


    @Test
    public void addUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long[] ptr3;

        ptr3 = LongArithmetic.addUint(ptr, ptr2, 2);
        assert ptr3.length == 3;
        long carry = ptr3[2];
        Assert.assertEquals(carry, 0);
        Assert.assertEquals(ptr3[0], 0);
        Assert.assertEquals(ptr3[1], 0);

        ptr[0] = Long.MAX_VALUE;
        ptr[1] = Long.MAX_VALUE;
        ptr2[0] = 0;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.addUint(ptr, ptr2, 2);
        assert ptr3.length == 3;
        carry = ptr3[2];
        Assert.assertEquals(carry, 0);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[0]);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[1]);

        ptr[0] = Long.MAX_VALUE - 1;
        ptr[1] = Long.MAX_VALUE;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.addUint(ptr, ptr2, 2);
        assert ptr3.length == 3;
        carry = ptr3[2];
        Assert.assertEquals(carry, 0);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[0]);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[1]);

        ptr[0] = Long.MAX_VALUE;
        ptr[1] = Long.MAX_VALUE;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.addUint(ptr, ptr2, 2);
        assert ptr3.length == 3;
        carry = ptr3[2];
        Assert.assertEquals(carry, 1);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        ptr[0] = Long.MAX_VALUE;
        ptr[1] = Long.MAX_VALUE;
        ptr2[0] = Long.MAX_VALUE;
        ptr2[1] = Long.MAX_VALUE;
        ptr3 = LongArithmetic.addUint(ptr, ptr2, 2);
        assert ptr3.length == 3;
        carry = ptr3[2];
        Assert.assertEquals(1, carry);
        Assert.assertEquals(Long.MAX_VALUE - 1, ptr3[0]);
        Assert.assertEquals(Long.MAX_VALUE, ptr3[1]);

        ptr[0] = Long.MAX_VALUE;
        ptr[1] = 0;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.addUint(ptr, ptr2, 2);
        assert ptr3.length == 3;
        carry = ptr3[2];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(1, ptr3[1]);

        ptr[0] = Long.MAX_VALUE;
        ptr[1] = 5;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3 = LongArithmetic.addUint(ptr, 2, ptr2, 1, 0, 2);
        assert ptr3.length == 3;
        carry = ptr3[2];
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(6, ptr3[1]);
    }

    @Test
    public void divideUint189Test() {

        // 想了下，是没问题的，我的计算结果是在 base-2^63 下去表示最终的 商
        // 是不冲突的，只不过后续的  barrett 也需要在 base-2^63 下完成计算，不过想了下，问题应该不大
        // 只要计算结果是正确的，只是表示问题而已！
        long denominator = 3;
        long[] quotient  = new long[3];
        long remainder = LongArithmetic.divideUint189(LongArithmetic.TWO_POWER_128, denominator, quotient);

        Assert.assertEquals(1, remainder);
        BigInteger towPower128 = BigInteger.valueOf(2).pow(128);
        BigInteger left = towPower128.divide(BigInteger.valueOf(denominator));
        BigInteger right = LongArithmetic.convertLongArrayToBigInteger(quotient);
        Assert.assertEquals(left, right);

    }



    @Test
    public void divideUint189Test1() {
        long[] input = new long[3];
        long[] quotient = new long[3];
        long remainder;
        long denominator;
        Random random = new Random();

        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            input[0] = Math.abs(random.nextLong());
            input[1] = Math.abs(random.nextLong());
            input[2] = Math.abs(random.nextLong());
            denominator = Math.abs(random.nextLong());
            Arrays.fill(quotient, 0);

            remainder = LongArithmetic.divideUint189(Arrays.copyOf(input, input.length), denominator, quotient);
            BigInteger res = LongArithmetic.convertLongArrayToBigInteger(quotient);
            BigInteger[] divdeAndRemainder = LongArithmetic.convertLongArrayToBigInteger(input).divideAndRemainder(BigInteger.valueOf(denominator));
            Assert.assertEquals(res, divdeAndRemainder[0]);
            Assert.assertEquals(remainder, divdeAndRemainder[1].longValue());
        }
    }


    /**
     * come from tests/seal/util/uintarith.cpp DivideUInt192UInt64
     */
    @Test
    public void divideUint189Test2() {
       long[] input = new long[3];
       long[] quotient = new long[3];
       long[] zeros = new long[3];
       long remainder;
        // 0 / 1
       remainder = LongArithmetic.divideUint189(input, 1L, quotient);
       Assert.assertEquals(remainder, 0);
       Assert.assertArrayEquals(quotient, zeros);
        // 1/1
       input[0] = 1;
       Arrays.fill(quotient, 0);
       remainder = LongArithmetic.divideUint189(input, 1L, quotient);
       Assert.assertEquals(remainder, 0);
       Assert.assertEquals(quotient[0], 1);

        input[0] = 0x10101010L;
        input[1] = 0x2B2B2B2BL;
        input[2] = 0xF1F1F1F1L;
        Arrays.fill(quotient, 0);
        // because input will be used to assert, so we pass the cloned input, not the original
        remainder = LongArithmetic.divideUint189(Arrays.copyOf(input, input.length), 0x1000L, quotient);
        Assert.assertEquals(0x10L, remainder);
        BigInteger res = LongArithmetic.convertLongArrayToBigInteger(quotient);
        BigInteger truth = LongArithmetic.convertLongArrayToBigInteger(input).divide(BigInteger.valueOf(0x1000L));
        Assert.assertEquals(res, truth);

        input[0] = 1212121212121212L;
        input[1] = 3434343434343434L;
        input[2] = 5656565656565656L;
        Arrays.fill(quotient, 0);
        remainder = LongArithmetic.divideUint189(Arrays.copyOf(input, input.length), 7878787878787878L, quotient);

        BigInteger[] divedeAndRemainder = LongArithmetic.convertLongArrayToBigInteger(input).divideAndRemainder(BigInteger.valueOf(7878787878787878L));
        Assert.assertEquals(remainder, divedeAndRemainder[1].longValue());
        res = LongArithmetic.convertLongArrayToBigInteger(quotient);
        Assert.assertEquals(res, divedeAndRemainder[0]);
    }

    public boolean longArrayConvertBigIntegerThenCompare(long[] as, long[] bs) {

        assert as.length == bs.length;
        BigInteger left = LongArithmetic.convertLongArrayToBigInteger(as);
        BigInteger right = LongArithmetic.convertLongArrayToBigInteger(bs);
        return left.compareTo(right) == 0;
    }

}
