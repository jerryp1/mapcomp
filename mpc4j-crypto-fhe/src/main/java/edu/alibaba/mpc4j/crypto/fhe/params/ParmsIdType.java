package edu.alibaba.mpc4j.crypto.fhe.params;

import edu.alibaba.mpc4j.crypto.fhe.utils.HashFunction;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public class ParmsIdType {

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
        this.value = value;
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
        ParmsIdType that = (ParmsIdType)obj;
        return new EqualsBuilder()
                .append(this.value, that.value)
                .isEquals();
    }

    @Override
    public String toString() {
        return "ParmsIdType{" +
                "value=" + Arrays.toString(value) +
                '}';
    }
}
