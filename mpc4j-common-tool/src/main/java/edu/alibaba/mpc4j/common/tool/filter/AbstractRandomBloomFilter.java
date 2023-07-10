package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * abstract random Bloom Filter, i.e., the hash indexes are random but may not be distinct.
 *
 * @author Weiran Liu
 * @date 2023/5/7
 */
abstract class AbstractRandomBloomFilter<T> extends AbstractBloomFilter<T> {
    /**
     * number of hashes
     */
    private final int hashNum;
    /**
     * hash
     */
    protected final Prf hash;

    /**
     * Creates a Bloom Filter.
     *
     * @param bloomFilterType Bloom Filter type.
     * @param envType         environment.
     * @param maxSize         max number of element.
     * @param m               number of positions in the Bloom Filter.
     * @param hashNum         number of hashes.
     * @param key             hash key.
     * @param size            number of inserted elements.
     * @param storage         the storage.
     * @param itemByteLength  sum of byte length for inserted elements.
     */
    AbstractRandomBloomFilter(FilterType bloomFilterType, EnvType envType, int maxSize, int m, int hashNum, byte[] key,
                              int size, byte[] storage, int itemByteLength) {
        super(bloomFilterType, envType, maxSize, m, size, storage, itemByteLength);
        MathPreconditions.checkPositive("hashNum", hashNum);
        this.hashNum = hashNum;
        // we use on hash to compute all positions
        hash = PrfFactory.createInstance(envType, hashNum * Integer.BYTES);
        hash.setKey(key);
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // type
        byteArrayList.add(IntUtils.intToByteArray(bloomFilterType.ordinal()));
        // max size
        byteArrayList.add(IntUtils.intToByteArray(maxSize));
        // size
        byteArrayList.add(IntUtils.intToByteArray(size));
        // item byte length
        byteArrayList.add(IntUtils.intToByteArray(itemByteLength));
        // storage
        byteArrayList.add(BytesUtils.clone(storage));
        // key
        byteArrayList.add(BytesUtils.clone(hash.getKey()));

        return byteArrayList;
    }

    @Override
    public int[] hashIndexes(T data) {
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        byte[] hashes = hash.getBytes(dataBytes);
        return Arrays.stream(IntUtils.byteArrayToIntArray(hashes))
            .map(hi -> Math.abs(hi % m))
            .distinct()
            .toArray();
    }

    @Override
    public void merge(MergeFilter<T> other) {
        AbstractRandomBloomFilter<T> that = (AbstractRandomBloomFilter<T>) other;
        // max size should be the same
        MathPreconditions.checkEqual("this.maxSize", "that.maxSize", this.maxSize, that.maxSize);
        MathPreconditions.checkEqual("this.hashNum", "that.hashNum", this.hashNum, that.hashNum);
        // hash type should be equal
        Preconditions.checkArgument(this.hash.getPrfType().equals(that.hash.getPrfType()));
        // key should be equal
        Preconditions.checkArgument(Arrays.equals(hash.getKey(), that.hash.getKey()));
        MathPreconditions.checkLessOrEqual("merge size", this.size + that.size, maxSize);
        // merge Bloom Filter
        BytesUtils.ori(storage, that.storage);
        size += that.size;
        itemByteLength += that.itemByteLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractRandomBloomFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        AbstractRandomBloomFilter<T> that = (AbstractRandomBloomFilter<T>) obj;
        return new EqualsBuilder()
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.storage, that.storage)
            .append(this.hashNum, that.hashNum)
            .append(this.hash.getPrfType(), that.hash.getPrfType())
            .append(this.hash.getKey(), that.hash.getKey())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(maxSize)
            .append(size)
            .append(itemByteLength)
            .append(storage)
            .append(hashNum)
            .append(hash.getPrfType())
            .append(hash.getKey())
            .toHashCode();
    }
}
