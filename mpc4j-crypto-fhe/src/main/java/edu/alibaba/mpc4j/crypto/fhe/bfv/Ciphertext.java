package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class Ciphertext {


    public Polynomial c0;
    public Polynomial c1;

    public long scalingFactor;
    public long cipherModulus;


    public Ciphertext(Polynomial c0, Polynomial c1, long scalingFactor, long cipherModulus) {
        this.c0 = c0;
        this.c1 = c1;
        this.scalingFactor = scalingFactor;
        this.cipherModulus = cipherModulus;
    }

    public Ciphertext(Polynomial c0, Polynomial c1) {
        this.c0 = c0;
        this.c1 = c1;
        this.scalingFactor = 0;
        this.cipherModulus = 0;
    }


    @Override
    public String toString() {
        return "Ciphertext{" +
                "c0=" + c0 + "\n" +
                "c1=" + c1 +
                '}';
    }
}
