package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.*;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Distinct Bloom Filter related to distrinct Garbled Bloom Filter, which requires that any inputs have constant distinct
 * positions in the Garbled Bloom Filter. This requirement is used in the following paper:
 * <p>
 * Lepoint, Tancrede, Sarvar Patel, Mariana Raykova, Karn Seth, and Ni Trieu. Private join and compute from PIR with
 * default. ASIACRYPT 2021, Part II, pp. 605-634. Cham: Springer International Publishing, 2021.
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/5/4
 */
public class DistinctBloomFilter<T> extends AbstractBloomFilter<T> {
    /**
     * When m = n log_2(e) * log_2(1/p), HASH_NUM = log_2(1/p)
     */
    static final int HASH_NUM = CommonConstants.STATS_BIT_LENGTH;
    /**
     * hash key num = hash num for computing distinct outputs
     */
    static final int HASH_KEY_NUM = HASH_NUM;

    /**
     * Gets m for the given n.
     *
     * @param maxSize number of elements.
     * @return m.
     */
    public static int bitSize(int maxSize) {
        MathPreconditions.checkPositive("n", maxSize);
        // m = n / ln(2) * σ, flooring so that m % Byte.SIZE = 0.
        return CommonUtils.getByteLength((int) Math.ceil(maxSize * CommonConstants.STATS_BIT_LENGTH / Math.log(2))) * Byte.SIZE;
    }

    /**
     * Creates an empty filter.
     *
     * @param envType environment.
     * @param maxSize max number of inserted elements.
     * @param keys    hash keys.
     * @return an empty filter.
     */
    public static <X> DistinctBloomFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, HASH_KEY_NUM);
        int m = DistinctBloomFilter.bitSize(maxSize);
        byte[] storage = new byte[CommonUtils.getByteLength(m)];
        // all positions are initialed as 0
        Arrays.fill(storage, (byte) 0x00);

        return new DistinctBloomFilter<>(envType, maxSize, m, keys, 0, storage, 0);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> DistinctBloomFilter<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("byteArrayList.size", "desired size", byteArrayList.size(), 5 + HASH_KEY_NUM);
        // type
        byteArrayList.remove(0);
        // max size
        int maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        int m = DistinctBloomFilter.bitSize(maxSize);
        // size
        int size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // item byte length
        int itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // storage
        byte[] storage = byteArrayList.remove(0);
        // keys
        byte[][] keys = byteArrayList.toArray(new byte[0][]);
        MathPreconditions.checkEqual("keys.length", "hash key num", keys.length, HASH_KEY_NUM);

        return new DistinctBloomFilter<>(envType, maxSize, m, keys, size, storage, itemByteLength);
    }

    /**
     * hashes
     */
    private final Prf[] hashes;

    DistinctBloomFilter(EnvType envType, int maxSize, int m, byte[][] keys, int size, byte[] storage, int itemByteLength) {
        super(FilterType.DISTINCT_BLOOM_FILTER, envType, maxSize, m, size, storage, itemByteLength);
        hashes = Arrays.stream(keys)
            .map(key -> {
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
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
        // keys
        for (Prf hash : hashes) {
            byteArrayList.add(BytesUtils.clone(hash.getKey()));
        }

        return byteArrayList;
    }

    @Override
    public int[] hashIndexes(T data) {
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        int[] hashIndexes = new int[HASH_NUM];
        TIntSet positionSet = new TIntHashSet(HASH_NUM);
        hashIndexes[0] = hashes[0].getInteger(0, dataBytes, m);
        positionSet.add(hashIndexes[0]);
        // generate k distinct positions
        for (int i = 1; i < HASH_NUM; i++) {
            int hiIndex = 0;
            do {
                hashIndexes[i] = hashes[i].getInteger(hiIndex, dataBytes, m);
                hiIndex++;
            } while (positionSet.contains(hashIndexes[i]));
            positionSet.add(hashIndexes[i]);
        }
        return hashIndexes;
    }

    @Override
    public void merge(MergeFilter<T> other) {
        DistinctBloomFilter<T> that = (DistinctBloomFilter<T>) other;
        // max size should be the same
        MathPreconditions.checkEqual("this.maxSize", "that.maxSize", this.maxSize, that.maxSize);
        MathPreconditions.checkEqual("this.hashNum", "that.hashNum", this.hashes.length, that.hashes.length);
        int hashNum = hashes.length;
        IntStream.range(0, hashNum).forEach(hashIndex -> {
            // hash type should be equal
            Preconditions.checkArgument(this.hashes[hashIndex].getPrfType().equals(that.hashes[hashIndex].getPrfType()));
            // key should be equal
            Preconditions.checkArgument(Arrays.equals(hashes[hashIndex].getKey(), that.hashes[hashIndex].getKey()));
        });
        MathPreconditions.checkLessOrEqual("merge size", this.size + that.size, maxSize);
        // merge Bloom Filter
        BytesUtils.ori(storage, that.storage);
        size += that.size;
        itemByteLength += that.itemByteLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DistinctBloomFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        DistinctBloomFilter<T> that = (DistinctBloomFilter<T>) obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.storage, that.storage);
        int hashNum = hashes.length;
        IntStream.range(0, hashNum).forEach(hashIndex -> {
            equalsBuilder.append(hashes[hashIndex].getPrfType(), that.hashes[hashIndex].getPrfType());
            equalsBuilder.append(hashes[hashIndex].getKey(), that.hashes[hashIndex].getKey());
        });
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder
            .append(maxSize)
            .append(size)
            .append(itemByteLength)
            .append(storage);
        int hashNum = hashes.length;
        IntStream.range(0, hashNum).forEach(hashIndex -> {
            hashCodeBuilder.append(hashes[hashIndex].getPrfType());
            hashCodeBuilder.append(hashes[hashIndex].getKey());
        });
        return hashCodeBuilder.toHashCode();
    }
}
