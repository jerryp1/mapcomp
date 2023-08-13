package edu.alibaba.mpc4j.crypto.fhe.bfv;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class RelinKey {


    public long base;
    // ( [-(a_i \cdot s + e_i) ], a_i) , i = \{0, 1, ..., l\}
    // l = \floor(log_T(q)) , q is cipher modulus, T = \sqrt(q)
    public PublicKey[] keys;

    public RelinKey(long base, PublicKey[] keys) {
        this.base = base;
        this.keys = keys;
    }

}
