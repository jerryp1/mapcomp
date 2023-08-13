package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;
import org.checkerframework.checker.units.qual.C;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class BfvDecryptor {


    public long polyModulusDegree;

    public long cipherModulus;

    public long plainModulus;

    public long scalingFactor;

    public SecretKey secretKey;


    public BfvDecryptor(BfvParameters param, SecretKey secretKey) {
        this.polyModulusDegree = param.polyModulusDegree;
        this.cipherModulus = param.cipherModulus;
        this.plainModulus = param.plainModulus;
        this.scalingFactor = param.scalingFactor;
        this.secretKey = secretKey;
    }

    public Plaintext decrypt(Ciphertext ciphertext) {

        Polynomial c0 = ciphertext.c0;
        Polynomial c1 = ciphertext.c1;
        Polynomial c0PlusC1MulS = c0.add(c1.mul(secretKey.s, cipherModulus), cipherModulus);

        c0PlusC1MulS = c0PlusC1MulS.mulScalarRound((double) 1/scalingFactor);
        c0PlusC1MulS.modInplace(plainModulus);

        return new Plaintext(c0PlusC1MulS);
    }



}
