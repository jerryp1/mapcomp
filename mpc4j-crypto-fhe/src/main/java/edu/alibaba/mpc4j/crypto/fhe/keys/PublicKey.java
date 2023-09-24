package edu.alibaba.mpc4j.crypto.fhe.keys;

import edu.alibaba.mpc4j.crypto.fhe.Ciphertext;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;

/**
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class PublicKey implements Cloneable {


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
}
