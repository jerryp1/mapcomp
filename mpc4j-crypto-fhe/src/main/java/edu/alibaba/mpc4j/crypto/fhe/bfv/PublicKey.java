package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class PublicKey {



    Polynomial p0;

    Polynomial p1;


    public PublicKey() {

    }

    public void setP0(Polynomial p0) {
        this.p0 = p0;
    }
    public void setP1(Polynomial p1) {
        this.p1 = p1;
    }


    public PublicKey(Polynomial p0, Polynomial p1) {
        this.p0 = p0;
        this.p1 = p1;
    }

    @Override
    public String toString() {
        return "PublicKey{" +
                "p0=" + p0 + "\n" +
                "p1=" + p1 +
                '}';
    }
}
