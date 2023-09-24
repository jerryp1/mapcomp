package edu.alibaba.mpc4j.crypto.fhe.params;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.crypto.fhe.utils.HashFunction;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public class ParmsIdType implements Cloneable {

    public long[] value;

    public static final ParmsIdType PARMS_ID_ZERO;

    static {
        PARMS_ID_ZERO = new ParmsIdType(HashFunction.HASH_ZERO_BLOCK);
    }

    public static final long[] ZERO_VALUE = HashFunction.HASH_ZERO_BLOCK;


    public static ParmsIdType parmsIdZero() {
        long[] value = new long[HashFunction.HASH_BLOCK_UINT64_COUNT];
        return new ParmsIdType(value);
    }


    public ParmsIdType(long[] value) {
        assert value.length == HashFunction.HASH_BLOCK_UINT64_COUNT;

        this.value = new long[HashFunction.HASH_BLOCK_UINT64_COUNT];
        System.arraycopy(value, 0, this.value, 0, HashFunction.HASH_BLOCK_UINT64_COUNT);
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

    public void set(long[] value) {

        assert value.length == HashFunction.HASH_BLOCK_UINT64_COUNT;
        this.value = value;
    }


    public boolean isZero() {
        return Arrays.equals(this.value, ZERO_VALUE);
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
        return "ParmsIdType{" +
                "value=" + Arrays.toString(value) +
                '}';
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
