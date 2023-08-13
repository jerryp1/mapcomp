package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.checkerframework.checker.units.qual.A;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
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
        Assert.assertEquals(0X5555555555555555L, mod.getBarrettQuotient()[0]);
        Assert.assertEquals(0X5555555555555555L, mod.getBarrettQuotient()[1]);
        Assert.assertEquals(1, mod.getBarrettRemainder());


        value = 2;
        mod.setValue(value);
        System.out.printf("barrett quotient: %s, %s, %s%n", Long.toHexString(mod.getBarrettQuotient()[0]), Long.toHexString(mod.getBarrettQuotient()[1]), Long.toHexString(mod.getBarrettQuotient()[2]));
        System.out.printf("barrett remainder: %d%n", mod.getBarrettRemainder());


        value = 0xF00000F00000FL;
        mod.setValue(value);
        Assert.assertEquals(value, mod.getValue());
        Assert.assertEquals(52, mod.getBitCount());
        Assert.assertEquals(1, mod.getUint64Count());
        Assert.assertEquals(0x1100000000000011L, mod.getBarrettQuotient()[0]);
        Assert.assertEquals(4369, mod.getBarrettQuotient()[1]);
        Assert.assertEquals(281470698520321L, mod.getBarrettRemainder());

         value = 0xF00000F000079L;
        mod.setValue(value);
        Assert.assertEquals(value, mod.getValue());
        Assert.assertEquals(52, mod.getBitCount());
        Assert.assertEquals(1, mod.getUint64Count());
        Assert.assertEquals(1224979096621368355L, mod.getBarrettQuotient()[0]);
        Assert.assertEquals(4369, mod.getBarrettQuotient()[1]);
        Assert.assertEquals(1144844808538997L, mod.getBarrettRemainder());
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






}
