package edu.alibaba.mpc4j.crypto.fhe.params;

import edu.alibaba.mpc4j.crypto.fhe.utils.HashFunction;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * The data type to store unique identifiers of encryption parameters.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptionparams.h#L43
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public class ParmsIdType implements Cloneable, Serializable {

    /**
     * parms id
     */
    public long[] value;
    /**
     * return a zero parms id
     */
    public ParmsIdType() {
        value = new long[HashFunction.HASH_BLOCK_UINT64_COUNT];
    }

    public ParmsIdType(long[] value) {
        assert value.length == HashFunction.HASH_BLOCK_UINT64_COUNT;
        this.value = new long[HashFunction.HASH_BLOCK_UINT64_COUNT];
        System.arraycopy(value, 0, this.value, 0, HashFunction.HASH_BLOCK_UINT64_COUNT);
    }

    public static ParmsIdType parmsIdZero() {
        return new ParmsIdType();
    }

    /**
     * deep-copy a ParamsIdType object
     *
     * @param other another ParamsIdType object
     */
    public ParmsIdType(ParmsIdType other) {
        this.value = new long[other.value.length];
        System.arraycopy(other.value, 0, value, 0, value.length);
    }

    /**
     * set parms id to given value.
     *
     * @param value parms id.
     */
    public void set(long[] value) {
        assert value.length == HashFunction.HASH_BLOCK_UINT64_COUNT;
        this.value = value;
    }

    /**
     * set the parms id to zero.
     */
    public void setZero() {
        Arrays.fill(value, 0);
    }

    /**
     * whether the parms id is zero.
     *
     * @return return true if the parms id is zer0, otherwise false.
     */
    public boolean isZero() {
        return (value[0] == 0) && (value[1] == 0) && (value[2] == 0) && (value[3] == 0);
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParmsIdType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ParmsIdType that = (ParmsIdType) obj;
        return new EqualsBuilder().append(this.value, that.value).isEquals();
    }

    /**
     * reference seal/encryptionparams.h   struct hash<seal::parms_id_type>
     *
     * @return the hashcode of a ParmsIdType object
     */
    @Override
    public int hashCode() {
        long result = 17;
        result = 31 * result + value[0];
        result = 31 * result + value[1];
        result = 31 * result + value[2];
        result = 31 * result + value[3];
        return (int) result;
    }

    @Override
    public String toString() {
        return "{" + "value=" + value[0] + '}';
    }

    @Override
    public ParmsIdType clone() {
        try {
            ParmsIdType clone = (ParmsIdType) super.clone();
            clone.value = new long[this.value.length];
            System.arraycopy(this.value, 0, clone.value, 0, this.value.length);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}