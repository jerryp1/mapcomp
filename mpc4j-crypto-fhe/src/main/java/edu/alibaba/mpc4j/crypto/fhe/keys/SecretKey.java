package edu.alibaba.mpc4j.crypto.fhe.keys;

import edu.alibaba.mpc4j.crypto.fhe.Plaintext;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;

/**
 * Class to store a secret key.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/secretkey.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class SecretKey implements Cloneable {

    private Plaintext sk;

    public SecretKey() {
        sk = new Plaintext();
    }

    /**
     * @return the data of SecretKey object
     */
    public Plaintext data() {
        return sk;
    }

    /**
     * @return parmsId of SecretKey, pointing to an EncryptionParameter object
     */
    public ParmsIdType parmsId() {
        return sk.getParmsId();
    }

    public void setParmsId(ParmsIdType parmsId) {
        sk.setParmsId(parmsId);
    }


    @Override
    public SecretKey clone() {
        try {
            SecretKey clone = (SecretKey) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.sk = this.sk.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
