package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author Qixian Zhou
 * @date 2023/9/10
 */
public class PlaintextTest {

    @Test
    public void basic() {

        Plaintext plain = new Plaintext(2);
        Assert.assertEquals(2, plain.capacity());
        Assert.assertEquals(2, plain.coeffCount());
        Assert.assertEquals(0, plain.significantCoeffCount());
        Assert.assertEquals(0, plain.nonZeroCoeffCount());
        Assert.assertFalse(plain.isNttForm());

        plain.set(0, 1);
        plain.set(1, 2);
        plain.reserve(10);
        Assert.assertEquals(10, plain.capacity());
        Assert.assertEquals(2, plain.coeffCount());
        Assert.assertEquals(2, plain.significantCoeffCount());
        Assert.assertEquals(2, plain.nonZeroCoeffCount());
        Assert.assertEquals(1, plain.get(0));
        Assert.assertEquals(2, plain.get(1));
        Assert.assertFalse(plain.isNttForm());

        plain.resize(5);
        Assert.assertEquals(10, plain.capacity());
        Assert.assertEquals(5, plain.coeffCount());
        Assert.assertEquals(2, plain.significantCoeffCount());
        Assert.assertEquals(2, plain.nonZeroCoeffCount());
        Assert.assertEquals(1, plain.get(0));
        Assert.assertEquals(2, plain.get(1));
        Assert.assertEquals(0, plain.get(2));
        Assert.assertEquals(0, plain.get(3));
        Assert.assertEquals(0, plain.get(4));
        Assert.assertFalse(plain.isNttForm());

        Plaintext plain2 = new Plaintext();
        plain2.resize(15);
        Assert.assertEquals(15, plain2.capacity());
        Assert.assertEquals(15, plain2.coeffCount());
        Assert.assertEquals(0, plain2.significantCoeffCount());
        Assert.assertEquals(0, plain2.significantCoeffCount());
        Assert.assertFalse(plain.isNttForm());

        plain2 = plain;
        Assert.assertEquals(10, plain2.capacity());
        Assert.assertEquals(5, plain2.coeffCount());
        Assert.assertEquals(2, plain2.significantCoeffCount());
        Assert.assertEquals(2, plain2.nonZeroCoeffCount());
        Assert.assertEquals(1, plain2.get(0));
        Assert.assertEquals(2, plain2.get(1));
        Assert.assertEquals(0, plain2.get(2));
        Assert.assertEquals(0, plain2.get(3));
        Assert.assertEquals(0, plain2.get(4));
        Assert.assertSame(plain2, plain);

        Plaintext plain3 = new Plaintext(plain2);
        Assert.assertEquals(10, plain3.capacity());
        Assert.assertEquals(5, plain3.coeffCount());
        Assert.assertEquals(2, plain3.significantCoeffCount());
        Assert.assertEquals(2, plain3.nonZeroCoeffCount());
        Assert.assertEquals(1, plain3.get(0));
        Assert.assertEquals(2, plain3.get(1));
        Assert.assertEquals(0, plain3.get(2));
        Assert.assertEquals(0, plain3.get(3));
        Assert.assertEquals(0, plain3.get(4));
        Assert.assertNotSame(plain2, plain3);

        plain.setParmsId(new long[]{1, 2, 3, 4});
        Assert.assertTrue(plain.isNttForm());

        plain2.setParmsId(ParmsIdType.PARMS_ID_ZERO);
        Assert.assertFalse(plain2.isNttForm());
        plain2.setParmsId(new long[]{1, 2, 3, 5});
        plain2.isNttForm();
        Assert.assertTrue(plain2.isNttForm());
    }

}
