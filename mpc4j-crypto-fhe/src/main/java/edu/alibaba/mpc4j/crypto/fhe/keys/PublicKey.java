package edu.alibaba.mpc4j.crypto.fhe.keys;

import edu.alibaba.mpc4j.crypto.fhe.Ciphertext;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Class to store a public key.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/publickey.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class PublicKey implements Cloneable, Serializable {

    /**
     * public key
     */
    private Ciphertext pk;

    /**
     Creates an empty public key.
     */
    public PublicKey() {
        pk = new Ciphertext();
    }

    /**
     * Returns the underlying polynomials of public key.
     *
     * @return the underlying polynomials of public key.
     */
    public Ciphertext data() {
        return pk;
    }

    /**
     * Returns parmsId of public key, pointing to an EncryptionParameter object.
     *
     * @return parmsId of public key.
     */
    public ParmsIdType parmsId() {
        return pk.getParmsId();
    }

    /**
     * Sets given parms ID to public key.
     *
     * @param parmsId the given parms ID.
     */
    public void setParmsId(ParmsIdType parmsId) {
        this.pk.setParmsId(parmsId);
    }

    @Override
    public PublicKey clone() {
        try {
            PublicKey clone = (PublicKey) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.pk = this.pk.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicKey)) {
            return false;
        }
        PublicKey publicKey = (PublicKey) o;
        return new EqualsBuilder().append(pk, publicKey.pk).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(pk).toHashCode();
    }
}
