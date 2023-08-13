package edu.alibaba.mpc4j.crypto.fhe.bfv;

/**
 * @author Qixian Zhou
 * @date 2023/7/13
 */
public class BfvParameters {


    public long polyModulusDegree;

    public long plainModulus;
    // cipherModulus long 装得下吗？需要显式的指定吗？ Seal 里应该都是默认生成的一组 primes , 根据 N 直接生成的
    public long cipherModulus;

    public long scalingFactor;

    public BfvParameters(long polyModulusDegree, long plainModulus, long cipherModulus) {

        this.polyModulusDegree = polyModulusDegree;
        this.plainModulus = plainModulus;
        this.cipherModulus = cipherModulus;
        // \Delta = \ceil(q/t)
        this.scalingFactor =  cipherModulus / plainModulus;

    }

    @Override
    public String toString() {
        return "BfvParameters{" +
                "polyModulusDegree=" + polyModulusDegree +
                ", plainModulus=" + plainModulus +
                ", cipherModulus=" + cipherModulus +
                '}';
    }
}
