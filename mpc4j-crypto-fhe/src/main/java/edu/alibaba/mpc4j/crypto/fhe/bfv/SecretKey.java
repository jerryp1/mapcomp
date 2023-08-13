package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class SecretKey {


    public Polynomial s;


    public SecretKey(Polynomial s) {

        this.s = s;
    }


    @Override
    public String toString() {
        return "SecretKey{" +
                "s=" + s +
                '}';
    }
}
