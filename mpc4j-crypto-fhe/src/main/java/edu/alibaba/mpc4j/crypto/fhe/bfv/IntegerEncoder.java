package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;

import java.awt.*;

/**
 * @author Qixian Zhou
 * @date 2023/7/14
 */
public class IntegerEncoder {



    public int base;
    public long polyModulusDegree;

    public IntegerEncoder(long polyModulusDegree) {
        this.polyModulusDegree = polyModulusDegree;
        this.base = 2;
    }

    public IntegerEncoder(long polyModulusDegree, int base) {
        this.polyModulusDegree = polyModulusDegree;
        this.base = base;
    }

    public Plaintext encode(long value) {

        long[] coeffs = new long[(int) polyModulusDegree];
        int i = 0;
        while (value > 0) {
            coeffs[i] = value % base;
            value /= base;
            i += 1;
        }
        return new Plaintext(new Polynomial(polyModulusDegree, coeffs));
    }

    public long decode(Plaintext plain) {
        long value = 0;
        int power = 1;
        for(long c: plain.poly.coeffs) {
            value += c * power;
            power *= base;
        }
        return value;
    }




}
