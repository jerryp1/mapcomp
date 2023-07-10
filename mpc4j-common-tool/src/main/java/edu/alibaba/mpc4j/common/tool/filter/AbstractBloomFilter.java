package edu.alibaba.mpc4j.common.tool.filter;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

/**
 * abstract Bloom Filter.
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
abstract class AbstractBloomFilter<T> implements BloomFilter<T> {
    /**
     * Bloom Filter type
     */
    protected final FilterType bloomFilterType;
    /**
     * max number of elements
     */
    protected final int maxSize;
    /**
     * storage bit length
     */
    protected final int m;
    /**
     * offset
     */
    private final int offset;
    /**
     * storage
     */
    protected final byte[] storage;
    /**
     * number of inserted elements
     */
    protected int size;
    /**
     * item byte length, used for computing compress radio
     */
    protected int itemByteLength;

    /**
     * Creates a Bloom Filter.
     *
     * @param bloomFilterType Bloom Filter type.
     * @param envType         environment.
     * @param maxSize         max number of element.
     * @param m               number of positions in the Bloom Filter.
     * @param size            number of inserted elements.
     * @param storage         the storage.
     * @param itemByteLength  sum of byte length for inserted elements.
     */
    AbstractBloomFilter(FilterType bloomFilterType, EnvType envType, int maxSize, int m,
                        int size, byte[] storage, int itemByteLength) {
        this.bloomFilterType = bloomFilterType;
        MathPreconditions.checkPositive("maxSize", maxSize);
        this.maxSize = maxSize;
        this.m = m;
        int byteM = CommonUtils.getByteLength(m);
        offset = byteM * Byte.SIZE - m;
        MathPreconditions.checkNonNegative("size", size);
        this.size = size;
        MathPreconditions.checkEqual("storage.length", "byteM", storage.length, byteM);
        this.storage = storage;
        MathPreconditions.checkNonNegative("itemByteLength", itemByteLength);
        this.itemByteLength = itemByteLength;
    }

    @Override
    public FilterType getFilterType() {
        return bloomFilterType;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public byte[] getStorage() {
        return storage;
    }

    @Override
    public int getM() {
        return m;
    }

    @Override
    public boolean mightContain(T data) {
        // verify each position in the sparse indexes is true
        int[] hashIndexes = hashIndexes(data);
        for (int index : hashIndexes) {
            if (!BinaryUtils.getBoolean(storage, index + offset)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void put(T data) {
        MathPreconditions.checkLess("size", size, maxSize);
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        // mightContain has checked if there is any collision, here we can directly insert the item.
        int[] hashIndexes = hashIndexes(data);
        for (int index : hashIndexes) {
            if (!BinaryUtils.getBoolean(storage, index + offset)) {
                BinaryUtils.setBoolean(storage, index + offset, true);
            }
        }
        // update the item byte length
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        itemByteLength += dataBytes.length;
        size++;
    }

    @Override
    public double ratio() {
        return (double) storage.length / itemByteLength;
    }
}
