package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/5
 */
public class UintArithmeticSmallModTest {




    @Test
    public void multiplyUIntModOperandTest() {

        Modulus mod = new Modulus(3);
        MultiplyUintModOperand y = new MultiplyUintModOperand();
        y.set(1, mod);
        Assert.assertEquals(1, y.operand);
        Assert.assertEquals(6148914691236517205L, y.quotient);

        y.set(2, mod);
        y.setQuotient(mod);
        Assert.assertEquals(2, y.operand);
        Assert.assertEquals(0XAAAAAAAAAAAAAAAAL, y.quotient); //  12297829382473034410L

        mod.setValue(2147483647);
        y.set(1, mod);
        Assert.assertEquals(1, y.operand);
        Assert.assertEquals(8589934596L, y.quotient);
        y.set(2147483646L, mod);
        y.setQuotient(mod);
        Assert.assertEquals(2147483646L, y.operand);
        Assert.assertEquals(0xFFFFFFFDFFFFFFFBL, y.quotient); //  18446744065119617019

        mod.setValue(2305843009211596801L);
        y.set(1, mod);
        Assert.assertEquals(1, y.operand);
        Assert.assertEquals(8, y.quotient);

        y.set(2305843009211596800L, mod);
        y.setQuotient(mod);
        Assert.assertEquals(2305843009211596800L, y.operand);
        Assert.assertEquals(0xFFFFFFFFFFFFFFF7L, y.quotient); //  18446744073709551607

    }

    @Test
    public void dotProductTest() {

        long[] arr1 = new long[64];
        long[] arr2 = new long[64];
        Arrays.fill(arr1, 2);
        Arrays.fill(arr2, 3);

        Modulus mod = new Modulus(5);
        Assert.assertEquals(0, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 1, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 2, mod));
        Assert.assertEquals(15 % mod.getValue(), UintArithmeticSmallMod.dotProductMod(arr1, arr2, 15, mod));
        Assert.assertEquals(16 % mod.getValue(), UintArithmeticSmallMod.dotProductMod(arr1, arr2, 16, mod));
        Assert.assertEquals(17 % mod.getValue(), UintArithmeticSmallMod.dotProductMod(arr1, arr2, 17, mod));
        Assert.assertEquals(32 % mod.getValue(), UintArithmeticSmallMod.dotProductMod(arr1, arr2, 32, mod));
        Assert.assertEquals(64 % mod.getValue(), UintArithmeticSmallMod.dotProductMod(arr1, arr2, 64, mod));

        mod = Numth.getPrime(1024 * 2, Constants.MOD_BIT_COUNT_MAX);
        Arrays.fill(arr1, mod.getValue() - 1);
        Arrays.fill(arr2, mod.getValue() - 1);
        Assert.assertEquals(0, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 1, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 2, mod));
        Assert.assertEquals(15, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 15, mod));
        Assert.assertEquals(16, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 16, mod));
        Assert.assertEquals(17, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 17, mod));
        Assert.assertEquals(32, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 32, mod));
        Assert.assertEquals(64, UintArithmeticSmallMod.dotProductMod(arr1, arr2, 64, mod));
    }

    @Test
    public void exponentUintMod() {

        Modulus mod = new Modulus(5);
        Assert.assertEquals(1, UintArithmeticSmallMod.exponentUintMod(1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.exponentUintMod(1, 0xFFFFFFFFFFFFFFFFL, mod));
        Assert.assertEquals(3, UintArithmeticSmallMod.exponentUintMod(2, 0xFFFFFFFFFFFFFFFFL, mod));

        mod.setValue(0x1000000000000000L);
        Assert.assertEquals(0, UintArithmeticSmallMod.exponentUintMod(2, 60, mod));
        Assert.assertEquals(0x800000000000000L, UintArithmeticSmallMod.exponentUintMod(2, 59, mod));

        mod.setValue(131313131313L);
        Assert.assertEquals(39418477653L, UintArithmeticSmallMod.exponentUintMod(2424242424L, 16, mod));
    }


    @Test
    public void tryInvertModulusTest() {

        long[] res = new long[1];
        Modulus mod = new Modulus(5);
        Assert.assertFalse(UintArithmeticSmallMod.tryInvertUintMod(0, mod, res));
        Assert.assertTrue(UintArithmeticSmallMod.tryInvertUintMod(1, mod, res));
        Assert.assertEquals(1, res[0]);
        Assert.assertTrue(UintArithmeticSmallMod.tryInvertUintMod(2, mod, res));
        Assert.assertEquals(3, res[0]);
        Assert.assertTrue(UintArithmeticSmallMod.tryInvertUintMod(3, mod, res));
        Assert.assertEquals(2, res[0]);
        Assert.assertTrue(UintArithmeticSmallMod.tryInvertUintMod(4, mod, res));
        Assert.assertEquals(4, res[0]);

        mod.setValue(6);
        Assert.assertFalse(UintArithmeticSmallMod.tryInvertUintMod(2, mod, res));
        Assert.assertFalse(UintArithmeticSmallMod.tryInvertUintMod(3, mod, res));
        Assert.assertTrue(UintArithmeticSmallMod.tryInvertUintMod(5, mod, res));
        Assert.assertEquals(5, res[0]);

        mod.setValue(1351315121);
        Assert.assertTrue(UintArithmeticSmallMod.tryInvertUintMod(331975426, mod, res));
        Assert.assertEquals(1052541512, res[0]);

    }



    @Test
    public void moduloUintModTest() {

        long[] values = new long[4];
        long[] valuesCopy = Arrays.copyOf(values, values.length);

        Modulus mod = new Modulus(2);
        UintArithmeticSmallMod.moduloUintInplace(values, 3, mod);
        Assert.assertEquals(0, values[0]);
        Assert.assertEquals(0, values[1]);
        Assert.assertEquals(0, values[2]);

        long res = UintArithmeticSmallMod.moduloUint(valuesCopy, 3, mod);
        Assert.assertEquals(res, 0);




        values[0] = 1;
        values[1] = 0;
        values[2] = 0;
        UintArithmeticSmallMod.moduloUintInplace(values, 3, mod);
        Assert.assertEquals(1, values[0]);
        Assert.assertEquals(0, values[1]);
        Assert.assertEquals(0, values[2]);

        valuesCopy = Arrays.copyOf(values, values.length);
        res = UintArithmeticSmallMod.moduloUint(valuesCopy, 3, mod);
        Assert.assertEquals(1, res);


        values[0] = 2;
        values[1] = 0;
        values[2] = 0;
        UintArithmeticSmallMod.moduloUintInplace(values, 3, mod);
        Assert.assertEquals(0, values[0]);
        Assert.assertEquals(0, values[1]);
        Assert.assertEquals(0, values[2]);

        valuesCopy = Arrays.copyOf(values, values.length);
        res = UintArithmeticSmallMod.moduloUint(valuesCopy, 3, mod);
        Assert.assertEquals(0, res);

        values[0] = 3;
        values[1] = 0;
        values[2] = 0;
        UintArithmeticSmallMod.moduloUintInplace(values, 3, mod);
        Assert.assertEquals(1, values[0]);
        Assert.assertEquals(0, values[1]);
        Assert.assertEquals(0, values[2]);

        valuesCopy = Arrays.copyOf(values, values.length);
        res = UintArithmeticSmallMod.moduloUint(valuesCopy, 3, mod);
        Assert.assertEquals(1, res);


        mod.setValue(0xFFFFL);
        values[0] = 0X850717BF66F1FDB4L; // 9585656442714717620
        values[1] = 1817697005049051848L;
        values[2] = 0;
        UintArithmeticSmallMod.moduloUintInplace(values, 3, mod);
        Assert.assertEquals(65143L, values[0]);
        Assert.assertEquals(0, values[1]);
        Assert.assertEquals(0, values[2]);

        valuesCopy = Arrays.copyOf(values, values.length);
        res = UintArithmeticSmallMod.moduloUint(valuesCopy, 3, mod);
        Assert.assertEquals(65143L, res);



        mod.setValue(0x1000);
        values[0] = 0X850717BF66F1FDB4L; // 9585656442714717620
        values[1] = 1817697005049051848L;
        values[2] = 0;
        UintArithmeticSmallMod.moduloUintInplace(values, 3, mod);
        Assert.assertEquals(0xDB4, values[0]);
        Assert.assertEquals(0, values[1]);
        Assert.assertEquals(0, values[2]);

        valuesCopy = Arrays.copyOf(values, values.length);
        res = UintArithmeticSmallMod.moduloUint(valuesCopy, 3, mod);
        Assert.assertEquals(0xDB4, res);

        mod.setValue(0xFFFFFFFFC001L);
        values[0] = 0X850717BF66F1FDB4L; // 9585656442714717620
        values[1] = 1817697005049051848L;
        values[2] = 0XC87F88F385299344L; // 14447416709120365380
        values[3] = 67450014862939159L;
        UintArithmeticSmallMod.moduloUintInplace(values, 4, mod);
        Assert.assertEquals(124510066632001L, values[0]);
        Assert.assertEquals(0, values[1]);
        Assert.assertEquals(0, values[2]);
        Assert.assertEquals(0, values[3]);


        valuesCopy = Arrays.copyOf(values, values.length);
        res = UintArithmeticSmallMod.moduloUint(valuesCopy, 4, mod);
        Assert.assertEquals(124510066632001L, res);



    }

    @Test
    public void multiplyAddMod2() {

        Modulus mod = new Modulus(7);
        MultiplyUintModOperand y = new MultiplyUintModOperand();
        y.set(0, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(0, y, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(1, y, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyAddUintMod(0, y, 1, mod));
        y.set(1, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(0, y, 0, mod));
        y.set(4, mod);
        Assert.assertEquals(3, UintArithmeticSmallMod.multiplyAddUintMod(3, y, 5, mod));

        mod.setValue(0x1FFFFFFFFFFFFFFFL);
        y.set(0, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(0, y, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(1, y, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyAddUintMod(0, y, 1, mod));
        y.set(1, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(0, y, 0, mod));
        y.set(mod.getValue() - 1, mod);
        // (p-1)*(p-1) + (p-1) % (p-1)
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(mod.getValue() - 1, y, mod.getValue() - 1, mod));


    }

    @Test
    public void multiplyAddMod() {

        Modulus mod = new Modulus(7);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(0, 0, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(1, 0, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(0, 1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyAddUintMod(0, 0, 1, mod));
        Assert.assertEquals(3, UintArithmeticSmallMod.multiplyAddUintMod(3, 4, 5, mod));

        mod.setValue(0x1FFFFFFFFFFFFFFFL);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(0, 0, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(1, 0, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(0, 1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyAddUintMod(0, 0, 1, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyAddUintMod(mod.getValue() - 1, mod.getValue() - 1, mod.getValue() - 1, mod));
    }

    @Test
    public void multiplyUIntModLazyTest() {

        Modulus mod = new Modulus(2);
        MultiplyUintModOperand y = new MultiplyUintModOperand();
        y.set(0, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(0, y, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(1, y, mod));
        y.set(1, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(0, y, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintModLazy(1, y, mod));

        mod.setValue(10);
        y.set(0, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(0, y, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(1, y, mod));
        y.set(1, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(0, y, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintModLazy(1, y, mod));
        y.set(6, mod);
        Assert.assertEquals(2, UintArithmeticSmallMod.multiplyUintModLazy(7, y, mod));
        y.set(7, mod);
        Assert.assertEquals(9, UintArithmeticSmallMod.multiplyUintModLazy(7, y, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.multiplyUintModLazy(6, y, mod));

        mod.setValue(2305843009211596801L);
        y.set(0, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(0, y, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(1, y, mod));
        y.set(1, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintModLazy(0, y, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintModLazy(1, y, mod));
        y.set(1152921504605798400L, mod);
        Assert.assertEquals(576460752302899200L, UintArithmeticSmallMod.multiplyUintModLazy(1152921504605798401L, y, mod));
        y.set(1152921504605798401L, mod);
        Assert.assertEquals(576460752302899200L, UintArithmeticSmallMod.multiplyUintModLazy(1152921504605798400L, y, mod));
        Assert.assertEquals(1729382256908697601L, UintArithmeticSmallMod.multiplyUintModLazy(1152921504605798401L, y, mod));
        y.set(2305843009211596800L, mod);
        // [p, 2p) case
        Assert.assertEquals(2305843009211596802L, UintArithmeticSmallMod.multiplyUintModLazy(2305843009211596800L, y, mod));

    }

    // using MultiplyOperandUintMod
    @Test
    public void multiplyUintModTest2() {

        Modulus mod = new Modulus(2);
        MultiplyUintModOperand y = new MultiplyUintModOperand();
        y.set(0, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, y, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(1, y, mod));
        y.set(1, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, y, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintMod(1, y, mod));

        mod.setValue(10);
        y.set(0, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, y, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(1, y, mod));
        y.set(1, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, y, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintMod(1, y, mod));
        y.set(6, mod);
        Assert.assertEquals(2, UintArithmeticSmallMod.multiplyUintMod(7, y, mod));
        y.set(7, mod);
        Assert.assertEquals(9, UintArithmeticSmallMod.multiplyUintMod(7, y, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.multiplyUintMod(6, y, mod));

        mod.setValue(2305843009211596801L);
        y.set(0, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, y, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(1, y, mod));
        y.set(1, mod);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, y, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintMod(1, y, mod));
        y.set(1152921504605798400L, mod);
        Assert.assertEquals(576460752302899200L, UintArithmeticSmallMod.multiplyUintMod(1152921504605798401L, y, mod));
        y.set(1152921504605798401L, mod);
        Assert.assertEquals(576460752302899200L, UintArithmeticSmallMod.multiplyUintMod(1152921504605798400L, y, mod));
        Assert.assertEquals(1729382256908697601L, UintArithmeticSmallMod.multiplyUintMod(1152921504605798401L, y, mod));
        y.set(2305843009211596800L, mod);
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintMod(2305843009211596800L, y, mod));

    }

    @Test
    public void multiplyUintModTest() {

        Modulus mod = new Modulus(2);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, 1, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintMod(1, 1, mod));

        mod.setValue(10);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, 1, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintMod(1, 1, mod));
        Assert.assertEquals(9, UintArithmeticSmallMod.multiplyUintMod(7, 7, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.multiplyUintMod(6, 7, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.multiplyUintMod(7, 6, mod));

        mod.setValue(2305843009211596801L);
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(0, 1, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.multiplyUintMod(1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintMod(1, 1, mod));
        Assert.assertEquals(576460752302899200L, UintArithmeticSmallMod.multiplyUintMod(1152921504605798400L, 1152921504605798401L, mod));
        Assert.assertEquals(576460752302899200L, UintArithmeticSmallMod.multiplyUintMod(1152921504605798401L, 1152921504605798400L, mod));
        Assert.assertEquals(1729382256908697601L, UintArithmeticSmallMod.multiplyUintMod(1152921504605798401L, 1152921504605798401L, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.multiplyUintMod(2305843009211596800L, 2305843009211596800L, mod));
    }

    @Test
    public void barrettReduce128Test() {

        long[] input = new long[2];

        Modulus mod = new Modulus(2);
        Assert.assertEquals(0, UintArithmeticSmallMod.barrettReduce128(input, mod));
        input[0] = 1;
        input[1] = 0;
        Assert.assertEquals(1, UintArithmeticSmallMod.barrettReduce128(input, mod));
        input[0] = 0xFFFFFFFFFFFFFFFFL;
        input[1] = 0xFFFFFFFFFFFFFFFFL;
        Assert.assertEquals(1, UintArithmeticSmallMod.barrettReduce128(input, mod));

        mod.setValue(3);
        input[0] = 0;
        input[1] = 0;
        Assert.assertEquals(0, UintArithmeticSmallMod.barrettReduce128(input, mod));
        input[0] = 1;
        input[1] = 0;
        Assert.assertEquals(1, UintArithmeticSmallMod.barrettReduce128(input, mod));
        input[0] = 123;
        input[1] = 456;
        Assert.assertEquals(0, UintArithmeticSmallMod.barrettReduce128(input, mod));
        input[0] = 0xFFFFFFFFFFFFFFFFL;
        input[1] = 0xFFFFFFFFFFFFFFFFL;
        Assert.assertEquals(0, UintArithmeticSmallMod.barrettReduce128(input, mod));


        mod.setValue(13131313131313L);
        input[0] = 0;
        input[1] = 0;
        Assert.assertEquals(0, UintArithmeticSmallMod.barrettReduce128(input, mod));
        input[0] = 1;
        input[1] = 0;
        Assert.assertEquals(1, UintArithmeticSmallMod.barrettReduce128(input, mod));
        input[0] = 123;
        input[1] = 456;
        Assert.assertEquals(8722750765283L, UintArithmeticSmallMod.barrettReduce128(input, mod));
        input[0] = 24242424242424L;
        input[1] = 79797979797979L;
        Assert.assertEquals(1010101010101L, UintArithmeticSmallMod.barrettReduce128(input, mod));
    }



    @Test
    public void incrementUintModTest(){

        Modulus modulus = new Modulus(2);
        Assert.assertEquals(1, UintArithmeticSmallMod.incrementUintMod(0, modulus) );
        Assert.assertEquals(0, UintArithmeticSmallMod.incrementUintMod(1, modulus) );

        modulus.setValue(0x10000);
        Assert.assertEquals(1, UintArithmeticSmallMod.incrementUintMod(0, modulus) );
        Assert.assertEquals(2, UintArithmeticSmallMod.incrementUintMod(1, modulus) );
        Assert.assertEquals(0, UintArithmeticSmallMod.incrementUintMod(0xFFFF, modulus) );

        modulus.setValue(2305843009211596801L);
        Assert.assertEquals(1, UintArithmeticSmallMod.incrementUintMod(0, modulus) );
        Assert.assertEquals(0, UintArithmeticSmallMod.incrementUintMod(2305843009211596800L, modulus) );
        Assert.assertEquals(1, UintArithmeticSmallMod.incrementUintMod(0, modulus) );

    }


    @Test
    public void decrementUintModTest(){

        Modulus modulus = new Modulus(2);
        Assert.assertEquals(0, UintArithmeticSmallMod.decrementUintMod(1, modulus) );
        Assert.assertEquals(1, UintArithmeticSmallMod.decrementUintMod(0, modulus) );


        modulus.setValue(0x10000);
        Assert.assertEquals(0, UintArithmeticSmallMod.decrementUintMod(1, modulus) );
        Assert.assertEquals(1, UintArithmeticSmallMod.decrementUintMod(2, modulus) );
        Assert.assertEquals(0xFFFF, UintArithmeticSmallMod.decrementUintMod(0, modulus) );

        modulus.setValue(2305843009211596801L);
        Assert.assertEquals(0, UintArithmeticSmallMod.decrementUintMod(1, modulus) );
        Assert.assertEquals(2305843009211596800L, UintArithmeticSmallMod.decrementUintMod(0, modulus) );
    }

    @Test
    public void negateUintModTest(){

        Modulus modulus = new Modulus(2);
        Assert.assertEquals(0, UintArithmeticSmallMod.negateUintMod(0, modulus) );
        Assert.assertEquals(1, UintArithmeticSmallMod.negateUintMod(1, modulus) );


        modulus.setValue(0xFFFF);
        Assert.assertEquals(0, UintArithmeticSmallMod.negateUintMod(0, modulus) );
        Assert.assertEquals(0xFFFE, UintArithmeticSmallMod.negateUintMod(1, modulus) );
        Assert.assertEquals(1, UintArithmeticSmallMod.negateUintMod(0xFFFE, modulus) );

        modulus.setValue(0x10000);
        Assert.assertEquals(0, UintArithmeticSmallMod.negateUintMod(0, modulus) );
        Assert.assertEquals(0xFFFF, UintArithmeticSmallMod.negateUintMod(1, modulus) );
        Assert.assertEquals(1, UintArithmeticSmallMod.negateUintMod(0xFFFF, modulus) );


        modulus.setValue(2305843009211596801L);
        Assert.assertEquals(0, UintArithmeticSmallMod.negateUintMod(0, modulus) );
        Assert.assertEquals(2305843009211596800L, UintArithmeticSmallMod.negateUintMod(1, modulus) );
    }

    @Test
    public void div2UintModTest() {

        Modulus mod = new Modulus(3);
        Assert.assertEquals(0, UintArithmeticSmallMod.div2UintMod(0, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.div2UintMod(1, mod));

        mod.setValue(17);
        Assert.assertEquals(11, UintArithmeticSmallMod.div2UintMod(5, mod));
        Assert.assertEquals(4, UintArithmeticSmallMod.div2UintMod(8, mod));

        mod.setValue(0xFFFFFFFFFFFFFFFL);
        Assert.assertEquals(0x800000000000000L, UintArithmeticSmallMod.div2UintMod(1, mod));
        Assert.assertEquals(0x800000000000001L, UintArithmeticSmallMod.div2UintMod(3, mod));
    }


    @Test
    public void addUintModTest() {

        Modulus mod = new Modulus(2);
        Assert.assertEquals(0, UintArithmeticSmallMod.addUintMod(0, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.addUintMod(1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.addUintMod(0, 1, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.addUintMod(1, 1, mod));

        mod.setValue(10);
        Assert.assertEquals(0, UintArithmeticSmallMod.addUintMod(0, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.addUintMod(1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.addUintMod(0, 1, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.addUintMod(1, 1, mod));
        Assert.assertEquals(4, UintArithmeticSmallMod.addUintMod(7, 7, mod));
        Assert.assertEquals(3, UintArithmeticSmallMod.addUintMod(6, 7, mod));

        mod.setValue(2305843009211596801L);
        Assert.assertEquals(0, UintArithmeticSmallMod.addUintMod(0, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.addUintMod(1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.addUintMod(0, 1, mod));
        Assert.assertEquals(2, UintArithmeticSmallMod.addUintMod(1, 1, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.addUintMod(1152921504605798400L, 1152921504605798401L, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.addUintMod(1152921504605798401L, 1152921504605798401L, mod));
        Assert.assertEquals(2305843009211596799L, UintArithmeticSmallMod.addUintMod(2305843009211596800L, 2305843009211596800L, mod));
    }


    @Test
    public void subUintModTest() {

        Modulus mod = new Modulus(2);
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(0, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.subUintMod(1, 0, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.subUintMod(0, 1, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(1, 1, mod));

        mod.setValue(10);
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(0, 0, mod));
        Assert.assertEquals(9, UintArithmeticSmallMod.subUintMod(0, 1, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.subUintMod(1, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(1, 1, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(7, 7, mod));
        Assert.assertEquals(9, UintArithmeticSmallMod.subUintMod(6, 7, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.subUintMod(7, 6, mod));

        mod.setValue(2305843009211596801L);
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(0, 0, mod));
        Assert.assertEquals(2305843009211596800L, UintArithmeticSmallMod.subUintMod(0, 1, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.subUintMod(1, 0, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(1, 1, mod));
        Assert.assertEquals(2305843009211596800L, UintArithmeticSmallMod.subUintMod(1152921504605798400L, 1152921504605798401L, mod));
        Assert.assertEquals(1, UintArithmeticSmallMod.subUintMod(1152921504605798401L, 1152921504605798400L, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(1152921504605798401L, 1152921504605798401L, mod));
        Assert.assertEquals(0, UintArithmeticSmallMod.subUintMod(2305843009211596800L, 2305843009211596800L, mod));

    }



}
