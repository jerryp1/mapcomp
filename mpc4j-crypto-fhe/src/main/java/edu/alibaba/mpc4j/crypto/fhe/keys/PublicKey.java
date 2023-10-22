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


    private Ciphertext pk;

    public PublicKey() {
        pk = new Ciphertext();
    }

    /**
     * @return the data of PublicKey object
     */
    public Ciphertext data() {
        return pk;
    }

    /**
     * @return parmsId of PublicKey, pointing to an EncryptionParameter object
     */
    public ParmsIdType parmsId() {
        return pk.getParmsId();
    }

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
        if (this == o) return true;

        if (!(o instanceof PublicKey)) return false;

        PublicKey publicKey = (PublicKey) o;

        return new EqualsBuilder().append(pk, publicKey.pk).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(pk).toHashCode();
    }
}
