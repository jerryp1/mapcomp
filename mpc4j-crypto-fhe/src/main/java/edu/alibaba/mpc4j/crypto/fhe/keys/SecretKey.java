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

    /**
     * secret key
     */
    private Plaintext sk;

    /**
     Creates an empty secret key.
     */
    public SecretKey() {
        sk = new Plaintext();
    }

    /**
     * Returns the underlying polynomial of secret key.
     *
     * @return the underlying polynomial of secret key.
     */
    public Plaintext data() {
        return sk;
    }

    /**
     * Returns parmsId of secret key, pointing to an EncryptionParameter object.
     *
     * @return parmsId of secret key.
     */
    public ParmsIdType parmsId() {
        return sk.getParmsId();
    }

    /**
     * Sets given parms ID to secret key.
     *
     * @param parmsId the given parms ID.
     */
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