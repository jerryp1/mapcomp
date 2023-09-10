package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * @author Qixian Zhou
 * @date 2023/8/3
 */
public class UintArithmeticTest {



    @Test
    public void exponentUintTest() {

        Assert.assertEquals(0, UintArithmetic.exponentUint(0, 1));
        Assert.assertEquals(1, UintArithmetic.exponentUint(1, 0));
        Assert.assertEquals(0, UintArithmetic.exponentUint(0, 0xFFFFFFFFFFFFFFFFL));
        Assert.assertEquals(1, UintArithmetic.exponentUint( 0xFFFFFFFFFFFFFFFFL, 0));
        Assert.assertEquals(25, UintArithmetic.exponentUint( 5, 2));
        Assert.assertEquals(32, UintArithmetic.exponentUint( 2, 5));
        Assert.assertEquals(0x1000000000000000L, UintArithmetic.exponentUint( 0x10, 15));
        Assert.assertEquals(0, UintArithmetic.exponentUint( 0x10, 16));
        // 12389286314587456613
        Assert.assertEquals(0XABEF964309980465L, UintArithmetic.exponentUint( 123456789L, 13));



    }



    @Test
    public void multiplyManyUint64Except() {

        long[] in = new long[3];
        long[] out = new long[3];
        long[] expected = new long[3];
        UintArithmetic.multiplyManyUint64Except(in, 2, 0, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64Except(in, 2, 1, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64Except(in, 3, 0, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64Except(in, 3, 1, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64Except(in, 3, 2, out);
        Assert.assertArrayEquals(expected, out);


        in = new long[]{ 2, 3, 5 };
        out = new long[]{ 0, 0, 0 };
        expected = new long[]{ 3, 0, 0 };
        UintArithmetic.multiplyManyUint64Except(in, 2, 0, out);
        Assert.assertArrayEquals(expected, out);
        expected = new long[]{ 2, 0, 0 };
        UintArithmetic.multiplyManyUint64Except(in, 2, 1, out);
        Assert.assertArrayEquals(expected, out);
        expected = new long[]{ 15, 0, 0 };
        UintArithmetic.multiplyManyUint64Except(in, 3, 0, out);
        Assert.assertArrayEquals(expected, out);
        expected = new long[]{ 10, 0, 0 };
        UintArithmetic.multiplyManyUint64Except(in, 3, 1, out);
        Assert.assertArrayEquals(expected, out);
        expected = new long[]{ 6, 0, 0 };
        UintArithmetic.multiplyManyUint64Except(in, 3, 2, out);
        Assert.assertArrayEquals(expected, out);

        in = new long[]{ 0xF0F0F0F0F0F0F0L, 0xBABABABABABABAL, 0xCECECECECECECEL};
        out = new long[]{ 0, 0, 0 };
        expected = new long[]{ 0x0c6a88a6c4e30120L, 0xc2a486684a2cL, 0};
        UintArithmetic.multiplyManyUint64Except(in, 3, 1, out);
        Assert.assertArrayEquals(expected, out);
    }



    @Test
    public void multiplyManyUint64() {

        long[] in = new long[1];
        long[] out = new long[1];
        long[] expected = new long[1];
        UintArithmetic.multiplyManyUint64(in, 1, out);
        Assert.assertArrayEquals(expected, out);

        in[0] = 1;
        out[0] = 0;
        expected[0] = 1;
        UintArithmetic.multiplyManyUint64(in, 1, out);
        Assert.assertArrayEquals(expected, out);

        in = new long[3];
        out = new long[3];
        expected = new long[3];
        UintArithmetic.multiplyManyUint64(in, 1, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64(in, 2, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64(in, 3, out);
        Assert.assertArrayEquals(expected, out);

        in = new long[]{ 1, 1, 1 };
        out = new long[]{ 0, 0, 0 };
        expected = new long[]{ 1, 0, 0 };
        UintArithmetic.multiplyManyUint64(in, 1, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64(in, 2, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64(in, 3, out);
        Assert.assertArrayEquals(expected, out);

        in = new long[]{ 10, 20, 40 };
        out = new long[]{ 0, 0, 0 };
        expected = new long[]{ 10, 0, 0 };

        UintArithmetic.multiplyManyUint64(in, 1, out);
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64(in, 2, out);
        expected = new long[]{ 200, 0, 0 };
        Assert.assertArrayEquals(expected, out);
        UintArithmetic.multiplyManyUint64(in, 3, out);
        expected = new long[]{ 8000, 0, 0 };
        Assert.assertArrayEquals(expected, out);

        in = new long[]{ 0xF0F0F0F0F0F0F0L, 0xBABABABABABABAL, 0xCECECECECECECEL};
        out = new long[]{ 0, 0, 0 };
        expected = new long[]{ 0xade881380d001140L, 0xd4d54d49088bd2ddL, 0x8df9832af0L};
        UintArithmetic.multiplyManyUint64(in, 3, out);
        Assert.assertArrayEquals(expected, out);
    }


    @Test
    public void multiplyUintUint64() {

        long[] ptr = new long[3];
        long[] result = new long[4];

        UintArithmetic.multiplyUint(ptr, 3, 0, 4, result);
        Assert.assertEquals(0, result[0]);
        Assert.assertEquals(0, result[1]);
        Assert.assertEquals(0, result[2]);
        Assert.assertEquals(0, result[3]);

        ptr[0] = 0xFFFFFFFFFL;
        ptr[1] = 0xAAAAAAAAAL;
        ptr[2] = 0x111111111L;
        UintArithmetic.multiplyUint(ptr, 3, 0, 4, result);
        Assert.assertEquals(0, result[0]);
        Assert.assertEquals(0, result[1]);
        Assert.assertEquals(0, result[2]);
        Assert.assertEquals(0, result[3]);

        ptr[0] = 0xFFFFFFFFFL;
        ptr[1] = 0xAAAAAAAAAL;
        ptr[2] = 0x111111111L;
        UintArithmetic.multiplyUint(ptr, 3, 1, 4, result);
        Assert.assertEquals(0xFFFFFFFFFL, result[0]);
        Assert.assertEquals(0xAAAAAAAAAL, result[1]);
        Assert.assertEquals(0x111111111L, result[2]);
        Assert.assertEquals(0, result[3]);

        ptr[0] = 0xFFFFFFFFFL;
        ptr[1] = 0xAAAAAAAAAL;
        ptr[2] = 0x111111111L;
        UintArithmetic.multiplyUint(ptr, 3, 0x10000L, 4, result);
        Assert.assertEquals(0xFFFFFFFFF0000L, result[0]);
        Assert.assertEquals(0xAAAAAAAAA0000L, result[1]);
        Assert.assertEquals(0x1111111110000L, result[2]);
        Assert.assertEquals(0, result[3]);

        ptr[0] = 0xFFFFFFFFFL;
        ptr[1] = 0xAAAAAAAAAL;
        ptr[2] = 0x111111111L;
        UintArithmetic.multiplyUint(ptr, 3, 0x100000000L, 4, result);
        Assert.assertEquals(0xFFFFFFFF00000000L, result[0]);
        Assert.assertEquals(0xAAAAAAAA0000000FL, result[1]);
        Assert.assertEquals(0x111111110000000AL, result[2]);
        Assert.assertEquals(1, result[3]);

        ptr[0] = 5656565656565656L;
        ptr[1] = 3434343434343434L;
        ptr[2] = 1212121212121212L;
        UintArithmetic.multiplyUint(ptr, 3, 7878787878787878L, 4, result);
        Assert.assertEquals(8891370032116156560L, result[0]);
        Assert.assertEquals(127835914414679452L, result[1]);
        Assert.assertEquals(0x8827D32D6D811F8EL, result[2]);  //9811042505314082702L
        Assert.assertEquals(517709026347L, result[3]);
    }

    @Test
    public void multiplyUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long[] ptr3 = new long[4];
        // 0 * 0 = 0
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[2] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[3] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.multiplyUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);
        Assert.assertEquals(0, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);
        // any * 0 = 0
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0;
        ptr2[1] = 0;
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[2] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[3] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.multiplyUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);
        Assert.assertEquals(0, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        ptr3[2] = 0;
        ptr3[3] = 0;
        UintArithmetic.multiplyUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);
        Assert.assertEquals(0, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);

        //      max max
        //      1  0
        //-------------
        //      0   0
        //max  max
        //max  max 0 ---> [0, max, max, 0]
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0;
        ptr2[1] = 1;
        ptr3[0] = 0;
        ptr3[1] = 0;
        ptr3[2] = 0;
        ptr3[3] = 0;
        UintArithmetic.multiplyUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);

        ptr[0] = 0x87664DA2ED1ABDA6L; // 9756571004902751654L
        ptr[1] = 731952007397389984L;
        ptr2[0] = 701538366196406307L;
        ptr2[1] = 1699883529753102283L;
        ptr3[0] = 0;
        ptr3[1] = 0;
        ptr3[2] = 0;
        ptr3[3] = 0;
        UintArithmetic.multiplyUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0x850717BF66F1FDB2L, ptr3[0]); //9585656442714717618L
        Assert.assertEquals(1817697005049051848L, ptr3[1]);
        Assert.assertEquals(0XC87F88F385299344L, ptr3[2]); // 14447416709120365380L
        Assert.assertEquals(67450014862939159L, ptr3[3]);



        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0;
        ptr3[1] = 0;
        ptr3[2] = 0;
        ptr3[3] = 0;
        UintArithmetic.multiplyUint(ptr,2,  ptr2, 1, 2, ptr3);
        Assert.assertEquals(1, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);
        Assert.assertEquals(0, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0;
        ptr3[1] = 0;
        ptr3[2] = 0;
        ptr3[3] = 0;
        UintArithmetic.multiplyUint(ptr,2,  ptr2, 1, 3, ptr3);
        Assert.assertEquals(1, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);

//        System.out.println(ptr3[0]);
//        System.out.println(ptr3[1]);
//        System.out.println(ptr3[2]);
//        System.out.println(ptr3[3]);


        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0;
        ptr3[1] = 0;
        ptr3[2] = 0;
        ptr3[3] = 0;
        UintArithmetic.multiplyTruncateUint(ptr,  ptr2, 2, ptr3);
        Assert.assertEquals(1, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);
        Assert.assertEquals(0, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);
    }




    @Test
    public void xorUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long[] ptr3 = new long[2];

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0;
        ptr2[0] = 0;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0;
        ptr3[1] = 0;
        UintArithmetic.xorUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);

        ptr[0] = 0xFFFFFFFF00000000L;
        ptr[1] = 0xFFFF0000FFFF0000L;
        ptr2[0] = 0x0000FFFF0000FFFFL;
        ptr2[1] = 0xFF00FF00FF00FF00L;
        ptr3[0] = 0;
        ptr3[1] = 0;
        UintArithmetic.xorUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0xFFFF00000000FFFFL, ptr3[0]);
        Assert.assertEquals(0x00FFFF0000FFFF00L, ptr3[1]);
        UintArithmetic.xorUint(ptr, ptr2, 2, ptr);
        Assert.assertEquals(0xFFFF00000000FFFFL, ptr[0]);
        Assert.assertEquals(0x00FFFF0000FFFF00L, ptr[1]);
    }


    @Test
    public void orUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long[] ptr3 = new long[2];

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0;
        ptr2[0] = 0;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0;
        ptr3[1] = 0;
        UintArithmetic.orUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);

        ptr[0] = 0xFFFFFFFF00000000L;
        ptr[1] = 0xFFFF0000FFFF0000L;
        ptr2[0] = 0x0000FFFF0000FFFFL;
        ptr2[1] = 0xFF00FF00FF00FF00L;
        ptr3[0] = 0;
        ptr3[1] = 0;
        UintArithmetic.orUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0xFFFFFFFF0000FFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFF00FFFFFF00L, ptr3[1]);
        UintArithmetic.orUint(ptr, ptr2, 2, ptr);
        Assert.assertEquals(0xFFFFFFFF0000FFFFL, ptr[0]);
        Assert.assertEquals(0xFFFFFF00FFFFFF00L, ptr[1]);
    }


    @Test
    public void andUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long[] ptr3 = new long[2];

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0;
        ptr2[0] = 0;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.andUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        ptr[0] = 0xFFFFFFFF00000000L;
        ptr[1] = 0xFFFF0000FFFF0000L;
        ptr2[0] = 0x0000FFFF0000FFFFL;
        ptr2[1] = 0xFF00FF00FF00FF00L;
        ptr3[0] = 0;
        ptr3[1] = 0;
        UintArithmetic.andUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0x0000FFFF00000000L, ptr3[0]);
        Assert.assertEquals(0xFF000000FF000000L, ptr3[1]);
        UintArithmetic.andUint(ptr, ptr2, 2, ptr);
        Assert.assertEquals(0x0000FFFF00000000L, ptr[0]);
        Assert.assertEquals(0xFF000000FF000000L, ptr[1]);
    }


    @Test
    public void notUintTest() {

        long[] ptr = new long[2];
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0;
        UintArithmetic.notUint(ptr, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr[1]);

        ptr[0] = 0xFFFFFFFF00000000L;
        ptr[1] = 0xFFFF0000FFFF0000L;
        UintArithmetic.notUint(ptr, 2, ptr);
        Assert.assertEquals(0x00000000FFFFFFFFL, ptr[0]);
        Assert.assertEquals(0x0000FFFF0000FFFFL, ptr[1]);
    }



    @Test
    public void halfRoundUpUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 1;
        ptr[1] = 0;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr2);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr);
        Assert.assertEquals(1, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 2;
        ptr[1] = 0;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr2);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr);
        Assert.assertEquals(1, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 3;
        ptr[1] = 0;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr2);
        Assert.assertEquals(2, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr[0] = 4;
        ptr[1] = 0;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr2);
        Assert.assertEquals(2, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.halfRoundUpUint(ptr, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0x8000000000000000L, ptr2[1]);

        UintArithmetic.halfRoundUpUint(ptr, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0x8000000000000000L, ptr[1]);
    }



    @Test
    public void addUint128Test() {

        long[] as = new long[2];
        long[] bs = new long[2];
        long[] res = new long[] {0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL};
        long carry;

        carry = UintArithmetic.addUint128(as, bs, res);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, res[0] | res[1]);

        as[0] = 1;
        as[1] = 1;
        bs[0] = 1;
        bs[1] = 1;
        carry = UintArithmetic.addUint128(as, bs, res);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(2, res[0]);
        Assert.assertEquals(2, res[1]);

        as[0] = 0xFFFFFFFFFFFFFFFFL;
        as[1] = 0;
        bs[0] = 1;
        bs[1] = 0;
        carry = UintArithmetic.addUint128(as, bs, res);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(1, res[1]);

        as[0] = 0xFFFFFFFFFFFFFFFFL;
        as[1] = 0xFFFFFFFFFFFFFFFFL;
        bs[0] = 1;
        bs[1] = 0;

        carry = UintArithmetic.addUint128(as, bs, res);
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(0, res[1]);
    }



    @Test
    public void multiplyUint64Hw64Test() {

        long res;

        res = UintArithmetic.multiplyUint64Hw64(0, 0);
        Assert.assertEquals(0, res);

        res = UintArithmetic.multiplyUint64Hw64(0, 1);
        Assert.assertEquals(0, res);

        res = UintArithmetic.multiplyUint64Hw64(1, 0);
        Assert.assertEquals(0, res);
        // why 1 * 1 = 0 ?, because the res is the high 64 bits
        res = UintArithmetic.multiplyUint64Hw64(1, 1);
        Assert.assertEquals(0, res);

        res = UintArithmetic.multiplyUint64Hw64(0x100000000L, 0xFAFABABAL);
        Assert.assertEquals(0, res);

        res = UintArithmetic.multiplyUint64Hw64(0x1000000000L, 0xFAFABABAL);
        Assert.assertEquals(0xFL, res);

        res = UintArithmetic.multiplyUint64Hw64(1111222233334444L, 5555666677778888L);
        Assert.assertEquals(334670460471L, res);
    }

    @Test
    public void multiplyUint64Test() {

        long[] res = new long[2];

        UintArithmetic.multiplyUint64(0, 0, res);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(0, res[1]);

        UintArithmetic.multiplyUint64(0, 1, res);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(0, res[1]);

        UintArithmetic.multiplyUint64(1, 0, res);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(0, res[1]);

        UintArithmetic.multiplyUint64(1, 1, res);
        Assert.assertEquals(1, res[0]);
        Assert.assertEquals(0, res[1]);

        UintArithmetic.multiplyUint64(0x100000000L, 0xFAFABABAL, res);
        Assert.assertEquals(0xFAFABABA00000000L, res[0]);
        Assert.assertEquals(0, res[1]);

        UintArithmetic.multiplyUint64(0x1000000000L, 0xFAFABABAL, res);
        Assert.assertEquals(0xAFABABA000000000L, res[0]);
        Assert.assertEquals(0xF, res[1]);

        UintArithmetic.multiplyUint64(1111222233334444L, 5555666677778888L, res);
        Assert.assertEquals(4140785562324247136L, res[0]);
        Assert.assertEquals(334670460471L, res[1]);

    }



    @Test
    public void divideUint192InplaceTest2() {

        long[] input = new long[3];
        long[] quotient = new long[3];
        long[] zeros = new long[3];

        input[2] = 1;

        UintArithmetic.divideUint192Inplace(input, 3L, quotient);
        System.out.println(input[0]);
        System.out.println(quotient[0]);
        System.out.println(quotient[1]);
        System.out.println(quotient[2]);

    }

    @Test
    public void divideUintInplaceTest2() {


        long[] a = new long[] {0, 0, 1};
        long[] b = new long[] {2, 0, 0};

        long[] res = new long[a.length];
        UintArithmetic.divideUintInplace(a, b, 3, res);
        System.out.println(Arrays.toString(res));

    }

    @Test
    public void divideUintInplaceTest() {

        long[] ptr = new long[4];
        long[] ptr2 = new long[4];
        long[] ptr3 = new long[4];
        long[] ptr4 = new long[4];
        ptr[0] = 0;
        ptr[1] = 0;
        ptr2[0] = 0;
        ptr2[1] = 1;
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.divideUintInplace(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        ptr[0] = 0;
        ptr[1] = 0;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.divideUintInplace(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);
        // small / big , quotient = 0, remainder = numerator
        ptr[0] = 0xFFFFFFFFFFFFFFFEL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.divideUintInplace(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr[1]);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);
        // max / max = 1, remainder = 0
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.divideUintInplace(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
        Assert.assertEquals(1, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        ptr[0] = 0x850717BF66F1FDB4L; // 9585656442714717620L
        ptr[1] = 1817697005049051848L;
        ptr[2] = 0xC87F88F385299344L; // 14447416709120365380L
        ptr[3] = 67450014862939159L;
        ptr2[0] = 701538366196406307L;
        ptr2[1] = 1699883529753102283L;
        ptr2[2] = 0;
        ptr2[3] = 0;
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[2] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[3] = 0xFFFFFFFFFFFFFFFFL;
        ptr4[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr4[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr4[2] = 0xFFFFFFFFFFFFFFFFL;
        ptr4[3] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.divideUint(ptr, ptr2, 4, ptr3, ptr4);
        // remainder
        Assert.assertEquals(2, ptr4[0]);
        Assert.assertEquals(0, ptr4[1]);
        Assert.assertEquals(0, ptr4[2]);
        Assert.assertEquals(0, ptr4[3]);
        // quotient
        Assert.assertEquals(0x87664DA2ED1ABDA6L, ptr3[0]); // 9756571004902751654L
        Assert.assertEquals(731952007397389984L, ptr3[1]);
        Assert.assertEquals(0, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);


        UintArithmetic.divideUintInplace(ptr, ptr2, 4, ptr3);
        Assert.assertEquals(2, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
        Assert.assertEquals(0, ptr[2]);
        Assert.assertEquals(0, ptr[3]);
        // quotient
        Assert.assertEquals(0x87664DA2ED1ABDA6L, ptr3[0]); // 9756571004902751654L
        Assert.assertEquals(731952007397389984L, ptr3[1]);
        Assert.assertEquals(0, ptr3[2]);
        Assert.assertEquals(0, ptr3[3]);

    }

    @Test
    public void divideUint128InplaceTest() {

        long[] input = new long[2];
        long[] quotient = new long[2];
        long[] zeros = new long[2];

        UintArithmetic.divideUint128Inplace(input, 1, quotient);
        Assert.assertArrayEquals(zeros, input);
        Assert.assertArrayEquals(zeros, quotient);

        input[0] = 1;
        input[1] = 0;
        UintArithmetic.divideUint128Inplace(input, 1, quotient);
        Assert.assertArrayEquals(zeros, input);
        Assert.assertEquals(1, quotient[0]);
        Assert.assertEquals(0, quotient[1]);


        input[0] = 0x10101010L;
        input[1] = 0x2B2B2B2BL;
        UintArithmetic.divideUint128Inplace(input, 0x1000L, quotient);
        Assert.assertEquals(0x10L, input[0]);
        Assert.assertEquals(0, input[1]);
        Assert.assertEquals(0xB2B0000000010101L, quotient[0]);
        Assert.assertEquals(0x2B2B2L, quotient[1]);

        input[0] = 1212121212121212L;
        input[1] = 3434343434343434L;
        UintArithmetic.divideUint128Inplace(input, 5656565656565656L, quotient);
        Assert.assertEquals(5252525252525252L, input[0]);
        Assert.assertEquals(0, input[1]);
        Assert.assertEquals(0x9B6DB6DB6DB6DB6DL, quotient[0]); // 11199808901895084909L
        Assert.assertEquals(0, quotient[1]);

    }

    @Test
    public void divideUint192InplaceTest() {

        long[] input = new long[3];
        long[] quotient = new long[3];
        long[] zeros = new long[3];

        UintArithmetic.divideUint192Inplace(input, 1, quotient);
        Assert.assertArrayEquals(zeros, input);
        Assert.assertArrayEquals(zeros, quotient);

        input[0] = 1;
        input[1] = 0;
        input[2] = 0;
        UintArithmetic.divideUint192Inplace(input, 1, quotient);
        Assert.assertArrayEquals(zeros, input);
        Assert.assertEquals(1, quotient[0]);
        Assert.assertEquals(0, quotient[1]);
        Assert.assertEquals(0, quotient[2]);

        input[0] = 0x10101010L;
        input[1] = 0x2B2B2B2BL;
        input[2] = 0xF1F1F1F1L;
        UintArithmetic.divideUint192Inplace(input, 0x1000L, quotient);
        Assert.assertEquals(0x10L, input[0]);
        Assert.assertEquals(0, input[1]);
        Assert.assertEquals(0, input[2]);
        Assert.assertEquals(0xB2B0000000010101L, quotient[0]);
        Assert.assertEquals(0x1F1000000002B2B2L, quotient[1]);
        Assert.assertEquals(0xF1F1FL, quotient[2]);

        input[0] = 1212121212121212L;
        input[1] = 3434343434343434L;
        input[2] = 5656565656565656L;
        UintArithmetic.divideUint192Inplace(input, 7878787878787878L, quotient);
        Assert.assertEquals(7272727272727272L, input[0]);
        Assert.assertEquals(0, input[1]);
        Assert.assertEquals(0, input[2]);
        Assert.assertEquals(0XEC4EC4EC4EC4EC4EL, quotient[0]); // 17027763760347278414L
        Assert.assertEquals(0XB7CB7CB7CB7CB7CBL, quotient[1]); // 13243816258047883211
        Assert.assertEquals(0, quotient[2]);
    }

    @Test
    public void leftShift128Test() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.leftShiftUint128(ptr, 0, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.leftShiftUint128(ptr, 10, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.leftShiftUint128(ptr, 10, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0xAAAAAAAAAAAAAAAAL;
        UintArithmetic.leftShiftUint128(ptr, 0, ptr2);
        Assert.assertEquals(0x5555555555555555L, ptr2[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[1]);
        UintArithmetic.leftShiftUint128(ptr, 0, ptr);
        Assert.assertEquals(0x5555555555555555L, ptr[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr[1]);
        UintArithmetic.leftShiftUint128(ptr, 1, ptr2);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0x5555555555555554L, ptr2[1]);
        UintArithmetic.leftShiftUint128(ptr, 2, ptr2);
        Assert.assertEquals(0x5555555555555554L, ptr2[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAA9L, ptr2[1]);
        UintArithmetic.leftShiftUint128(ptr, 64, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0x5555555555555555L, ptr2[1]);
        UintArithmetic.leftShiftUint128(ptr, 65, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[1]);
        UintArithmetic.leftShiftUint128(ptr, 127, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0x8000000000000000L, ptr2[1]);


        UintArithmetic.leftShiftUint128(ptr, 2, ptr);
        Assert.assertEquals(0x5555555555555554L, ptr[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAA9L, ptr[1]);
        UintArithmetic.leftShiftUint128(ptr, 64, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0x5555555555555554L, ptr[1]);

    }


    @Test
    public void rightShiftUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.rightShiftUint(ptr, 0, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.rightShiftUint(ptr, 10, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.rightShiftUint(ptr, 10, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0xAAAAAAAAAAAAAAAAL;
        UintArithmetic.rightShiftUint(ptr, 0, 2, ptr2);
        Assert.assertEquals(0x5555555555555555L, ptr2[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[1]);
        UintArithmetic.rightShiftUint(ptr, 0, 2, ptr);
        Assert.assertEquals(0x5555555555555555L, ptr[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr[1]);
        UintArithmetic.rightShiftUint(ptr, 1, 2, ptr2);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0x5555555555555555L, ptr2[1]);
        UintArithmetic.rightShiftUint(ptr, 2, 2, ptr2);
        Assert.assertEquals(0x9555555555555555L, ptr2[0]);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr2[1]);
        UintArithmetic.rightShiftUint(ptr, 64, 2, ptr2);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.rightShiftUint(ptr, 65, 2, ptr2);
        Assert.assertEquals(0x5555555555555555L, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.rightShiftUint(ptr, 127, 2, ptr2);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        UintArithmetic.rightShiftUint(ptr, 2, 2, ptr);
        Assert.assertEquals(0x9555555555555555L, ptr[0]);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr[1]);
        UintArithmetic.rightShiftUint(ptr, 64, 2, ptr);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
    }


    @Test
    public void rightShiftUint128Test() {
        long[] ptr = new long[2];
        long[] ptr2 = new long[2];

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.rightShiftUint128(ptr, 0, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.rightShiftUint128(ptr, 10, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.rightShiftUint128(ptr, 10,  ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0xAAAAAAAAAAAAAAAAL;
        UintArithmetic.rightShiftUint128(ptr, 0, ptr2);
        Assert.assertEquals(0x5555555555555555L, ptr2[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[1]);
        UintArithmetic.rightShiftUint128(ptr, 0, ptr);
        Assert.assertEquals(0x5555555555555555L, ptr[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr[1]);
        UintArithmetic.rightShiftUint128(ptr, 1, ptr2);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0x5555555555555555L, ptr2[1]);
        UintArithmetic.rightShiftUint128(ptr, 2, ptr2);
        Assert.assertEquals(0x9555555555555555L, ptr2[0]);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr2[1]);
        UintArithmetic.rightShiftUint128(ptr, 64, ptr2);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.rightShiftUint128(ptr, 65, ptr2);
        Assert.assertEquals(0x5555555555555555L, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.rightShiftUint128(ptr, 127, ptr2);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        UintArithmetic.rightShiftUint128(ptr, 2, ptr);
        Assert.assertEquals(0x9555555555555555L, ptr[0]);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr[1]);
        UintArithmetic.rightShiftUint128(ptr, 64, ptr);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr[0]);
        Assert.assertEquals(0, ptr[1]);




    }




    @Test
    public void rightShift192Test() {

        long[] ptr = new long[3];
        long[] ptr2 = new long[] {0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL};
        long[] zeros = new long[3];
        UintArithmetic.rightShiftUint192(ptr, 0, ptr2);
        Assert.assertArrayEquals(ptr2, zeros);

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[2] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.rightShiftUint192(ptr, 10, ptr2);
        Assert.assertArrayEquals(ptr2, zeros);

        UintArithmetic.rightShiftUint192(ptr, 10, ptr);
        Assert.assertArrayEquals(ptr, zeros);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0xAAAAAAAAAAAAAAAAL;
        ptr[2] = 0xCDCDCDCDCDCDCDCDL;
        UintArithmetic.rightShiftUint192(ptr, 0, ptr2);
        Assert.assertArrayEquals(ptr, ptr2);

        UintArithmetic.rightShiftUint192(ptr, 0, ptr);
        Assert.assertEquals(0x5555555555555555L, ptr[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr[1]);
        Assert.assertEquals(0xCDCDCDCDCDCDCDCDL, ptr[2]);

        UintArithmetic.rightShiftUint192(ptr, 1, ptr2);
        Assert.assertEquals(0x2AAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0xD555555555555555L, ptr2[1]);
        Assert.assertEquals(0x66E6E6E6E6E6E6E6L, ptr2[2]);

        UintArithmetic.rightShiftUint192(ptr, 2, ptr2);
        Assert.assertEquals(0x9555555555555555L, ptr2[0]);
        Assert.assertEquals(0x6AAAAAAAAAAAAAAAL, ptr2[1]);
        Assert.assertEquals(0x3373737373737373L, ptr2[2]);

        UintArithmetic.rightShiftUint192(ptr, 64, ptr2);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0xCDCDCDCDCDCDCDCDL, ptr2[1]);
        Assert.assertEquals(0, ptr2[2]);

        UintArithmetic.rightShiftUint192(ptr, 65, ptr2);
        Assert.assertEquals(0xD555555555555555L, ptr2[0]);
        Assert.assertEquals(0x66E6E6E6E6E6E6E6L, ptr2[1]);
        Assert.assertEquals(0, ptr2[2]);

        UintArithmetic.rightShiftUint192(ptr, 191, ptr2);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        Assert.assertEquals(0, ptr2[2]);

        UintArithmetic.rightShiftUint192(ptr, 2, ptr);
        Assert.assertEquals(0x9555555555555555L, ptr[0]);
        Assert.assertEquals(0x6AAAAAAAAAAAAAAAL, ptr[1]);
        Assert.assertEquals(0x3373737373737373L, ptr[2]);

        UintArithmetic.rightShiftUint192(ptr, 64, ptr);
        Assert.assertEquals(0x6AAAAAAAAAAAAAAAL, ptr[0]);
        Assert.assertEquals(0x3373737373737373L, ptr[1]);
        Assert.assertEquals(0, ptr[2]);
    }

    @Test
    public void leftShiftUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.leftShiftUint(ptr, 0, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.leftShiftUint(ptr, 10, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        UintArithmetic.leftShiftUint(ptr, 10, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0xAAAAAAAAAAAAAAAAL;
        UintArithmetic.leftShiftUint(ptr, 0, 2, ptr2);
        Assert.assertEquals(0x5555555555555555L, ptr2[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[1]);
        UintArithmetic.leftShiftUint(ptr, 0, 2, ptr2);
        Assert.assertEquals(0x5555555555555555L, ptr[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr[1]);

        UintArithmetic.leftShiftUint(ptr, 1, 2, ptr2);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals(0x5555555555555554L, ptr2[1]);

        UintArithmetic.leftShiftUint(ptr, 2, 2, ptr2);
        Assert.assertEquals(0x5555555555555554L, ptr2[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAA9L, ptr2[1]);

        UintArithmetic.leftShiftUint(ptr, 64, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0x5555555555555555L, ptr2[1]);

        UintArithmetic.leftShiftUint(ptr, 65, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAAAL, ptr2[1]);

        UintArithmetic.leftShiftUint(ptr, 127, 2, ptr2);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0x8000000000000000L, ptr2[1]);

        UintArithmetic.leftShiftUint(ptr, 2, 2, ptr);
        Assert.assertEquals(0x5555555555555554L, ptr[0]);
        Assert.assertEquals(0xAAAAAAAAAAAAAAA9L, ptr[1]);

        UintArithmetic.leftShiftUint(ptr, 64, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0x5555555555555554L, ptr[1]);
    }

    @Test
    public void leftShift192Test() {

        long[] ptr = new long[3];
        long[] ptr2 = new long[] {0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL};
        long[] zeros = new long[3];
        UintArithmetic.leftShiftUint192(ptr, 0, ptr2);
        Assert.assertArrayEquals(ptr2, zeros);

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[2] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmetic.leftShiftUint192(ptr, 10, ptr2);
        Assert.assertArrayEquals(ptr2, zeros);
        UintArithmetic.leftShiftUint192(ptr, 10, ptr);
        Assert.assertArrayEquals(ptr, zeros);

        ptr[0] = 0x5555555555555555L;
        ptr[1] = 0xAAAAAAAAAAAAAAAAL;
        ptr[2] = 0xCDCDCDCDCDCDCDCDL;
        UintArithmetic.leftShiftUint192(ptr, 0, ptr2);
        Assert.assertArrayEquals(ptr2, ptr);

        UintArithmetic.leftShiftUint192(ptr, 0, ptr);
        Assert.assertEquals( 0x5555555555555555L, ptr[0]);
        Assert.assertEquals( 0xAAAAAAAAAAAAAAAAL, ptr[1]);
        Assert.assertEquals( 0xCDCDCDCDCDCDCDCDL, ptr[2]);


        UintArithmetic.leftShiftUint192(ptr, 1, ptr2);
        Assert.assertEquals( 0xAAAAAAAAAAAAAAAAL, ptr2[0]);
        Assert.assertEquals( 0x5555555555555554L, ptr2[1]);
        Assert.assertEquals( 0x9B9B9B9B9B9B9B9BL, ptr2[2]);

        UintArithmetic.leftShiftUint192(ptr, 2, ptr2);
        Assert.assertEquals( 0x5555555555555554L, ptr2[0]);
        Assert.assertEquals( 0xAAAAAAAAAAAAAAA9L, ptr2[1]);
        Assert.assertEquals( 0x3737373737373736L, ptr2[2]);

        UintArithmetic.leftShiftUint192(ptr, 64, ptr2);
        Assert.assertEquals( 0, ptr2[0]);
        Assert.assertEquals( 0x5555555555555555L, ptr2[1]);
        Assert.assertEquals( 0xAAAAAAAAAAAAAAAAL, ptr2[2]);

        UintArithmetic.leftShiftUint192(ptr, 65, ptr2);
        Assert.assertEquals( 0, ptr2[0]);
        Assert.assertEquals( 0xAAAAAAAAAAAAAAAAL, ptr2[1]);
        Assert.assertEquals( 0x5555555555555554L, ptr2[2]);

        UintArithmetic.leftShiftUint192(ptr, 191, ptr2);
        Assert.assertEquals( 0, ptr2[0]);
        Assert.assertEquals( 0, ptr2[1]);
        Assert.assertEquals( 0x8000000000000000L, ptr2[2]);

        UintArithmetic.leftShiftUint192(ptr, 192, ptr2);
        Assert.assertEquals( 0, ptr2[0]);
        Assert.assertEquals( 0, ptr2[1]);
        Assert.assertEquals( 0x5555555555555555L, ptr2[2]);

        UintArithmetic.leftShiftUint192(ptr, 2, ptr);
        Assert.assertEquals( 0x5555555555555554L, ptr[0]);
        Assert.assertEquals( 0xAAAAAAAAAAAAAAA9L, ptr[1]);
        Assert.assertEquals( 0x3737373737373736L, ptr[2]);

        UintArithmetic.leftShiftUint192(ptr, 64, ptr);
        Assert.assertEquals( 0, ptr2[0]);
        Assert.assertEquals( 0x5555555555555554L, ptr[1]);
        Assert.assertEquals( 0xAAAAAAAAAAAAAAA9L, ptr[2]);
    }


    @Test
    public void subUint64Test() {

        long[] res = new long[1];
        long borrow;
        borrow = UintArithmetic.subUint64(0, 0, 0, res);
        Assert.assertEquals(res[0], 0);
        Assert.assertEquals(borrow, 0);

        borrow = UintArithmetic.subUint64(1, 1, 0, res);
        Assert.assertEquals(res[0], 0);
        Assert.assertEquals(borrow, 0);

        borrow = UintArithmetic.subUint64(1, 0, 1, res);
        Assert.assertEquals(res[0], 0);
        Assert.assertEquals(borrow, 0);

        borrow = UintArithmetic.subUint64(0, 1, 1, res);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, res[0]);
        Assert.assertEquals(1, borrow);

        borrow = UintArithmetic.subUint64(1, 1, 1, res);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, res[0]);
        Assert.assertEquals(1, borrow);

        borrow = UintArithmetic.subUint64(0xFFFFFFFFFFFFFFFFL, 1, 0, res);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, res[0]);
        Assert.assertEquals(0, borrow);

        borrow = UintArithmetic.subUint64(1, 0xFFFFFFFFFFFFFFFFL, 0, res);
        Assert.assertEquals(2, res[0]);
        Assert.assertEquals(1, borrow);

        borrow = UintArithmetic.subUint64(0, 0xFFFFFFFFFFFFFFFFL, 0, res);
        Assert.assertEquals(1, res[0]);
        Assert.assertEquals(1, borrow);

        borrow = UintArithmetic.subUint64(1, 0xFFFFFFFFFFFFFFFFL, 1, res);
        Assert.assertEquals(1, res[0]);
        Assert.assertEquals(1, borrow);

        borrow = UintArithmetic.subUint64(2, 0xFFFFFFFFFFFFFFFEL, 0, res);
        Assert.assertEquals(4, res[0]);
        Assert.assertEquals(1, borrow);

        borrow = UintArithmetic.subUint64(2, 0xFFFFFFFFFFFFFFFEL, 1, res);
        Assert.assertEquals(3, res[0]);
        Assert.assertEquals(1, borrow);

        borrow = UintArithmetic.subUint64(0xF00F00F00F00F00FL, 0x0FF0FF0FF0FF0FF0L, 0, res);
        Assert.assertEquals(0xE01E01E01E01E01FL, res[0]);
        Assert.assertEquals(0, borrow);

        borrow = UintArithmetic.subUint64(0xF00F00F00F00F00FL, 0x0FF0FF0FF0FF0FF0L, 1, res);
        Assert.assertEquals(0xE01E01E01E01E01EL, res[0]);
        Assert.assertEquals(0, borrow);
    }

    /**
     * some edge cases
     */
    @Test
    public void subUintTest2() {

        long[] as = new long[]{0, 0, 1};
        long[] bs = new long[]{0, 0x8000000000000000L, 1};
        long[] diff = new long[3];

//        long borrow = UintArithmetic.subUint(as, bs, 3, diff);
//
//        Assert.assertEquals(1, borrow);
//        Assert.assertEquals(0, diff[0]);
//        Assert.assertEquals(0x8000000000000000L, diff[1]);
//        Assert.assertEquals(0xffffffffffffffffL, diff[2]);


        as = new long[] {0, 0x6eeef150000f2000L, 1};
        bs = new long[] {0, 0xe00001e0000f2000L, 1};
        long borrow = UintArithmetic.subUint(as, bs, 3, diff);

        System.out.println("borrow: " +  borrow);
        System.out.println("diff: " + Long.toHexString(diff[0]) + ", " + Long.toHexString(diff[1]) + ", " + Long.toHexString(diff[2]));

    }

    @Test
    public void subUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long[] ptr3 = new long[2];
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        long[] zeros = new long[2];
        long borrow;

        borrow = UintArithmetic.subUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, borrow);
        Assert.assertArrayEquals(zeros, ptr3);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        borrow = UintArithmetic.subUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        borrow = UintArithmetic.subUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);

        ptr[0] = 0;
        ptr[1] = 0;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        borrow = UintArithmetic.subUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(1, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);


        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0;
        ptr3[1] = 0;
        borrow = UintArithmetic.subUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFEL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0;
        ptr3[1] = 0;
        borrow = UintArithmetic.subUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(1, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);

        ptr[0] = 0;
        ptr[1] = 1;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        borrow = UintArithmetic.subUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        // 7 parameters
        ptr[0] = 0;
        ptr[1] = 1;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        borrow = UintArithmetic.subUint(ptr, 2, ptr2, 1, 0,  2, ptr3);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        borrow = UintArithmetic.subUint(ptr, 2, ptr2, 1, 1,  2, ptr3);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

    }

    @Test
    public void addUint64Test() {
        long res[] = new long[1];
        long carry;

        carry = UintArithmetic.addUint64(0, 0, 0, res);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(0, carry);

        carry = UintArithmetic.addUint64(1, 1, 0, res);
        Assert.assertEquals(2, res[0]);
        Assert.assertEquals(0, carry);

        carry = UintArithmetic.addUint64(1, 0, 1, res);
        Assert.assertEquals(2, res[0]);
        Assert.assertEquals(0, carry);

        carry = UintArithmetic.addUint64(0, 1, 1, res);
        Assert.assertEquals(2, res[0]);
        Assert.assertEquals(0, carry);


        carry = UintArithmetic.addUint64(1, 1, 1, res);
        Assert.assertEquals(3, res[0]);
        Assert.assertEquals(0, carry);

        carry = UintArithmetic.addUint64(0xFFFFFFFFFFFFFFFFL, 1, 0, res);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(1, carry);

        carry = UintArithmetic.addUint64(1, 0xFFFFFFFFFFFFFFFFL, 1, res);
        Assert.assertEquals(1, res[0]);
        Assert.assertEquals(1, carry);

        carry = UintArithmetic.addUint64(1, 0xFFFFFFFFFFFFFFFFL, 1, res);
        Assert.assertEquals(1, res[0]);
        Assert.assertEquals(1, carry);

        carry = UintArithmetic.addUint64(2, 0xFFFFFFFFFFFFFFFEL, 0, res);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(1, carry);

        carry = UintArithmetic.addUint64(2, 0xFFFFFFFFFFFFFFFEL, 1, res);
        Assert.assertEquals(1, res[0]);
        Assert.assertEquals(1, carry);


        carry = UintArithmetic.addUint64(0xF00F00F00F00F00FL, 0x0FF0FF0FF0FF0FF0L, 0, res);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, res[0]);
        Assert.assertEquals(0, carry);

        carry = UintArithmetic.addUint64(0xF00F00F00F00F00FL, 0x0FF0FF0FF0FF0FF0L, 1, res);
        Assert.assertEquals(0, res[0]);
        Assert.assertEquals(1, carry);
    }


    @Test
    public void negateUintTest() {


        long[] values = new long[] {-2, -1};
        UintArithmetic.negateUint(values, 2, values);
        System.out.println(Arrays.toString(values));




        long[] ptr = new long[2];
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 1;
        ptr[1] = 0;
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr[1]);
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(1, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 2;
        ptr[1] = 0;
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr[1]);
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(2, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 0;
        ptr[1] = 1;
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr[1]);
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(1, ptr[1]);

        ptr[0] = 0;
        ptr[1] = 2;
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr[1]);
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(2, ptr[1]);

        ptr[0] = 1;
        ptr[1] = 1;
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr[1]);
        UintArithmetic.negateUint(ptr, 2, ptr);
        Assert.assertEquals(1, ptr[0]);
        Assert.assertEquals(1, ptr[1]);

    }

    @Test
    public void decrementUintTest() {
        long[] ptr1 = new long[2];
        long[] ptr2 = new long[2];
        long borrow;

        ptr1[0] = 2;
        ptr1[1] = 2;
        borrow = UintArithmetic.decrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(2, ptr2[1]);
        borrow = UintArithmetic.decrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0, ptr1[0]);
        Assert.assertEquals(2, ptr1[1]);
        borrow = UintArithmetic.decrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[0]);
        Assert.assertEquals(1, ptr2[1]);
        borrow = UintArithmetic.decrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr1[0]);
        Assert.assertEquals(1, ptr2[1]);

        ptr1[0] = 2;
        ptr1[1] = 1;
        borrow = UintArithmetic.decrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(1, ptr2[1]);
        borrow = UintArithmetic.decrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0, ptr1[0]);
        Assert.assertEquals(1, ptr1[1]);
        borrow = UintArithmetic.decrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        borrow = UintArithmetic.decrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr1[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr1[0] = 2;
        ptr1[1] = 0;
        borrow = UintArithmetic.decrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        borrow = UintArithmetic.decrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0, ptr1[0]);
        Assert.assertEquals(0, ptr1[1]);
        borrow = UintArithmetic.decrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(1, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[1]);
        borrow = UintArithmetic.decrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr1[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[1]);

    }


    @Test
    public void incrementUintTest() {
        long[] ptr1 = new long[2];
        long[] ptr2 = new long[2];
        long carry;

        carry = UintArithmetic.incrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
        carry = UintArithmetic.incrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(2, ptr1[0]);
        Assert.assertEquals(0, ptr1[1]);

        ptr1[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr1[1] = 0;
        carry = UintArithmetic.incrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(1, ptr2[1]);
        carry = UintArithmetic.incrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(1, ptr1[0]);
        Assert.assertEquals(1, ptr1[1]);

        ptr1[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr1[1] = 1;
        carry = UintArithmetic.incrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(2, ptr2[1]);
        carry = UintArithmetic.incrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(1, ptr1[0]);
        Assert.assertEquals(2, ptr1[1]);

        ptr1[0] = 0xFFFFFFFFFFFFFFFEL;
        ptr1[1] = 0xFFFFFFFFFFFFFFFFL;
        carry = UintArithmetic.incrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[1]);
        carry = UintArithmetic.incrementUint(ptr2, 2, ptr1);
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, ptr1[0]);
        Assert.assertEquals(0, ptr1[1]);
        carry = UintArithmetic.incrementUint(ptr1, 2, ptr2);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(1, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

    }


    @Test
    public void subUintUint64() {
        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long borrow;

        borrow = UintArithmetic.subUint(ptr, 2, 0, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        borrow = UintArithmetic.subUint(ptr, 2, 1, ptr2);
        Assert.assertEquals(1, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[1]);

        ptr[0] = 1;
        ptr[1] = 0;
        borrow = UintArithmetic.subUint(ptr, 2, 2, ptr2);
        Assert.assertEquals(1, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[1]);

        ptr[0] = 0xFFFFFFFF00000000L;
        ptr[1] = 0;
        borrow = UintArithmetic.subUint(ptr, 2, 0xFFFFFFFFL, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFE00000001L, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr[0] = 0xFFFFFFFF00000000L;
        ptr[1] = 0xFFFFFFFF00000000L;
        borrow = UintArithmetic.subUint(ptr, 2, 0x100000000L, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFE00000000L, ptr2[0]);
        Assert.assertEquals(0xFFFFFFFF00000000L, ptr2[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        borrow = UintArithmetic.subUint(ptr, 2, 1, ptr2);
        Assert.assertEquals(0, borrow);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr2[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[1]);


    }


    @Test
    public void addUintUint64() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long carry;

        carry = UintArithmetic.addUint(ptr, 2, 0, ptr2);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr[0] = 0xFFFFFFFF00000000L;
        ptr[1] = 0;
        carry = UintArithmetic.addUint(ptr, 2, 0xFFFFFFFFL, ptr2);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr[0] = 0xFFFFFFFF00000000L;
        ptr[1] = 0xFFFFFFFF00000000L;
        carry = UintArithmetic.addUint(ptr, 2, 0x100000000L, ptr2);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0xFFFFFFFF00000001L, ptr2[1]);


        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        carry = UintArithmetic.addUint(ptr, 2, 1, ptr2);
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

    }

    /**
     * some edge case comes from other high-level methods failed test
     */
    @Test
    public void addUint2() {

        long[] difference = new long[3];
        long[] numerator = new long[3];

        difference[0] = 0X44E6AF645F07C000L;
        difference[1] = 0XC33846470D40A000L;
        difference[2] = 0XFFF81AE691D51C72L;
        numerator[0] = 0X44E6AF645F07C000L;
        numerator[1] = 0XC33846470D40A000L;
        numerator[2] = 0X14189DD29BB798L;
        long carry;
        carry = UintArithmetic.addUint(difference, numerator, 3, difference);
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0x89cd5ec8be0f8000L ,difference[0]);
        Assert.assertEquals(0x86708c8e1a814000L ,difference[1]);
        Assert.assertEquals(0xc33846470d40bL ,difference[2]);



        numerator = new long[] {0, 0x7bbf85c00e4c4000L, 1};
        difference = new long[] {0, 0x9bbf83e00e3d2000L, 0xffffffffffffffffL};
        carry = UintArithmetic.addUint(difference, numerator, 3, difference);
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, difference[0]);
        Assert.assertEquals(0x177f09a01c896000L, difference[1]);
        Assert.assertEquals(1, difference[2]);


//        System.out.println("carry: " + carry);
//        System.out.printf("difference: %s, %s, %s%n", difference[0], difference[1], difference[2]);
//        System.out.printf("difference: %s, %s, %s%n", Long.toHexString(difference[0]), Long.toHexString(difference[1]), Long.toHexString(difference[2]));
//

    }

    @Test
    public void addUintTest() {

        long[] ptr = new long[2];
        long[] ptr2 = new long[2];
        long[] ptr3 = new long[2];
        long carry;

        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        carry = UintArithmetic.addUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        carry = UintArithmetic.addUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFEL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        carry = UintArithmetic.addUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);


        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[1] = 0xFFFFFFFFFFFFFFFFL;
        carry = UintArithmetic.addUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(0, ptr3[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        ptr3[0] = 0;
        ptr3[1] = 0;
        carry = UintArithmetic.addUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr3[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr3[1]);

        carry = UintArithmetic.addUint(ptr, ptr2, 2, ptr);
        Assert.assertEquals(1, carry);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, ptr[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, ptr[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        carry = UintArithmetic.addUint(ptr, ptr2, 2, ptr3);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(1, ptr3[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 5;
        ptr2[0] = 1;
        ptr2[1] = 0;
        ptr3[0] = 0;
        ptr3[1] = 0;
        carry = UintArithmetic.addUint(ptr, 2, ptr2, 1, 0, 2, ptr3);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(0, ptr3[0]);
        Assert.assertEquals(6, ptr3[1]);

        carry = UintArithmetic.addUint(ptr, 2, ptr2, 1, 1, 2, ptr3);
        Assert.assertEquals(0, carry);
        Assert.assertEquals(1, ptr3[0]);
        Assert.assertEquals(6, ptr3[1]);

    }


}
