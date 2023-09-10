package edu.alibaba.mpc4j.crypto.fhe.modulus;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import org.junit.Test;
import org.junit.Assert;

/**
 * @author Qixian Zhou
 * @date 2023/8/3
 */
public class ModulusTest {

    /**
     * test case comes from tests/seal/modulus.cpp
     */
    @Test
    public void createModulusTest() {

        long value = 3;
        Modulus mod = new Modulus(value);

        Assert.assertEquals(3, mod.getValue());
        Assert.assertEquals(2, mod.getBitCount());
        Assert.assertEquals(1, mod.getUint64Count());
        Assert.assertEquals(0X5555555555555555L, mod.getConstRatio()[0]);
        Assert.assertEquals(0X5555555555555555L, mod.getConstRatio()[1]);
        Assert.assertEquals(1, mod.getConstRatio()[2]);


        value = 2;
        mod.setValue(value);
        System.out.printf("barrett quotient: %s, %s, %s%n", Long.toHexString(mod.getConstRatio()[0]), Long.toHexString(mod.getConstRatio()[1]), Long.toHexString(mod.getConstRatio()[2]));
        System.out.printf("barrett remainder: %d%n", mod.getConstRatio()[2]);


        value = 0xF00000F00000FL;
        mod.setValue(value);
        Assert.assertEquals(value, mod.getValue());
        Assert.assertEquals(52, mod.getBitCount());
        Assert.assertEquals(1, mod.getUint64Count());
        Assert.assertEquals(0x1100000000000011L, mod.getConstRatio()[0]);
        Assert.assertEquals(4369, mod.getConstRatio()[1]);
        Assert.assertEquals(281470698520321L, mod.getConstRatio()[2]);

         value = 0xF00000F000079L;
        mod.setValue(value);
        Assert.assertEquals(value, mod.getValue());
        Assert.assertEquals(52, mod.getBitCount());
        Assert.assertEquals(1, mod.getUint64Count());
        Assert.assertEquals(1224979096621368355L, mod.getConstRatio()[0]);
        Assert.assertEquals(4369, mod.getConstRatio()[1]);
        Assert.assertEquals(1144844808538997L, mod.getConstRatio()[2]);
    }

    @Test
    public void reduceTest() {

        Modulus modulus = new Modulus(2);
        Assert.assertEquals(0, modulus.reduce(0));
        Assert.assertEquals(1, modulus.reduce(1));
        Assert.assertEquals(0, modulus.reduce(2));
        Assert.assertEquals(0, modulus.reduce(0xF0F0F0L));

        modulus.setValue(10);
        Assert.assertEquals(0, modulus.reduce(0));
        Assert.assertEquals(1, modulus.reduce(1));
        Assert.assertEquals(8, modulus.reduce(8));
        Assert.assertEquals(7, modulus.reduce(1234567));
        Assert.assertEquals(0, modulus.reduce(12345670));
    }


    @Test
    public void compareModulus() {

        Modulus sm2 = new Modulus(2);
        Modulus sm21 = new Modulus(2);
        Assert.assertEquals(sm2, sm21);

        System.out.println(sm2.equals(sm21));

    }

    /**
     * 验证 Modulus[] 的 copy 是 deep copy or not
     */
    @Test
    public void modulusArrayCopy() {

        Modulus[] moduluses = Numth.getPrimes(8, 16, 5);

        System.out.println(moduluses[0]);

        Modulus[] newModulus = new Modulus[moduluses.length];
        // copy
        System.arraycopy(moduluses, 0, newModulus, 0, moduluses.length);
        // change the new Modulus
        newModulus[0].setValue(100);

        System.out.println(newModulus[0]);
        System.out.println(moduluses[0]); // origin modulus will be changed, so this is a shallow copy
    }






}
