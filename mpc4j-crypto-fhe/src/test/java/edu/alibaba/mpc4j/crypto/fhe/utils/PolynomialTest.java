package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Test;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author Qixian Zhou
 * @date 2023/7/12
 */
public class PolynomialTest {




    @Test
    public void testAdd() {
        long coeffModulus = 20;
        Polynomial p1 = new Polynomial(5, new long[]{1, 2, 3, 4, 5});
        Polynomial p2 =  new Polynomial(5, new long[]{1, 2, 3, 4, 5});
        assert p1.add(p2, coeffModulus).equals( new Polynomial(5, new long[]{2, 4, 6, 8, 10}));

        Polynomial p3 = new Polynomial(5, new long[]{-1, -2, -3, -4, -5});
        Polynomial p4 =  new Polynomial(5, new long[]{-1, -2, -3, -4, -5});
        assert p3.add(p4, coeffModulus).equals( new Polynomial(5, new long[]{18, 16, 14, 12, 10}));
    }

    @Test
    public void testMul() {

        long coeffModulus = 73;
        Polynomial p1 = new Polynomial(4, new long[]{0, 1, 4, 5});
        Polynomial p2 = new Polynomial(4, new long[]{1, 2, 4, 3});
        Polynomial p3 = p1.mul(p2, coeffModulus);
        assert Arrays.equals(p3.coeffs, new long[]{44, 42, 64, 17});

        NttContext nttContext = new NttContext(4, coeffModulus);
        Polynomial p4 = p1.mul(p2, nttContext);
        assert p3.equals(p4);

        assert p2.mul(p1, coeffModulus).equals(p3);
        assert p2.mul(p1, nttContext).equals(p4);


        p1 = new Polynomial(4, new long[]{-1, 1, -1, 1});
        p2 = new Polynomial(4, new long[]{1, -1, 1, -1});

        p3 = p1.mul(p2, coeffModulus);
        assert Arrays.equals(p3.coeffs, new long[] {2, 0, 71, 4});

        p4 = p1.mul(p2, nttContext);
        assert p3.equals(p4);

        assert p2.mul(p1, coeffModulus).equals(p3);
        assert p2.mul(p1, nttContext).equals(p4);
    }

    @Test
    public void testMulCrt() {
        int logModulus = 10;
        int primeSize = 40;
        int logPolyDegree = 2;
        int polyModulusDegree = 1 << logPolyDegree;

        int numPrime = ( 2 + logPolyDegree + 4 * logModulus + primeSize - 1) / primeSize;

        CrtContext crtContext = new CrtContext(numPrime, primeSize, polyModulusDegree);
        Polynomial p1 = new Polynomial(polyModulusDegree, new long[]{0, 1, 4, 5});
        Polynomial p2 = new Polynomial(polyModulusDegree, new long[]{1, 2, 4, 3});

        BigInteger[] polyProd = p1.mul(p2, crtContext);
        BigInteger[] truth = new BigInteger[] { new BigInteger("1208925819915895360733940"),
                                                new BigInteger("1208925819915895360733938"),
                                                new BigInteger("1208925819915895360733960"),
                                                BigInteger.valueOf(17)};
        assert Arrays.equals(polyProd, truth);

    }




    @Test
    public void testEquals() {

        Polynomial p1 = new Polynomial(4, new long[]{0, 0, 0, 0});
        Polynomial p2 = new Polynomial(4, new long[]{0, 0, 0, 0});
        Polynomial p3 = new Polynomial(4, new long[]{0, 0, 0, 1});
        Polynomial p4 = new Polynomial(5, new long[]{0, 0, 0, 0, 0});

        assert p1.equals(p2);
        assert !p1.equals(p3);
        assert !p3.equals(p4);
    }


    @Test
    public void testToString() {
        Polynomial p = new Polynomial(4, new long[]{1,2,3,4});
        System.out.println(p);
        assert Objects.equals(p.toString(), "4x^3 + 3x^2 + 2x^1 + 1");
    }

    @Test
    public void testLongMod() {

       long a = ( 1L << 62) + 1;
       long b = ( 1L << 20) + 1;

       long c = (a * b) % 13;

       System.out.println(c);

       BigInteger aa = BigInteger.valueOf(a);
       BigInteger bb = BigInteger.valueOf(b);
       long cc = aa.multiply(bb).mod(BigInteger.valueOf(13)).longValue();
       System.out.println(cc);
       System.out.println( aa.multiply(bb));

    }
}
