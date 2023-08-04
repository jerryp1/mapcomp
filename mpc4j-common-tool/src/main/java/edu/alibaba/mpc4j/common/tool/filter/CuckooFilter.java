package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Cuckoo Filter. The scheme is described in the following paper:
 * <p>
 * Fan B, Andersen D G, Kaminsky M, et al. Cuckoo filter: Practically better than bloom. CoNET 2014, pp. 75-88.
 * </p>
 * Compared with Bloom Filter, Cuckoo Filter has the following advantages:
 * <li>Support adding and removing items dynamically.</li>
 * <li>Provide higher lookup performance than traditional Bloom Filters.</li>
 * <li>Use less space than Bloom Filters in many practical applications.</li>
 *
 * @author Li Peng, Weiran Liu
 * @date 2020/08/29
 */
public class CuckooFilter<T> implements Filter<T> {
    /**
     * α, number of elements in buckets / total number of elements. The default value is 95.5%, see Table 2.
     */
    private static final double LOAD_FACTOR = 0.955;
    /**
     * number of entries in each bucket. The default value is 4.
     */
    private static final int ENTRIES_PER_BUCKET = 4;
    /**
     * bit length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    static final int FINGERPRINT_BYTE_LENGTH = CommonUtils.getByteLength(
        DoubleMath.roundToInt((CommonConstants.STATS_BIT_LENGTH + 3), RoundingMode.UP)
    );
    /**
     * max number of kicks for collusion. In paper, it is set to be 500.
     * The test shows when inserting 2^20 elements, there are some non-negligible failure probability.
     * Here we set 2^10 = 1024, the same as cuckoo hash.
     */
    private static final int MAX_NUM_KICKS = 1 << 10;
    /**
     * empty byte buffer
     */
    private static final ByteBuffer ZERO_BYTE_BUFFER = ByteBuffer.wrap(new byte[0]);
    /**
     * hash num
     */
    private static final int HASH_NUM = 2;
    /**
     * hash key num
     */
    static final int HASH_KEY_NUM = HASH_NUM;

    /**
     * Gets the bucket num, must be in format 2^k.
     *
     * @param maxSize number of elements.
     * @return 哈希桶数量。
     */
    private static int getBucketNum(int maxSize) {
        return 1 << LongUtils.ceilLog2(
            DoubleMath.roundToInt((1.0 / LOAD_FACTOR) * maxSize / ENTRIES_PER_BUCKET, RoundingMode.UP) + 1
        );
    }

    /**
     * max number of elements.
     */
    private int maxSize;
    /**
     * bucket num
     */
    private int bucketNum;
    /**
     * the random state
     */
    private SecureRandom secureRandom;
    /**
     * cuckoo filter buckets
     */
    private ArrayList<ArrayList<CuckooFilterEntry>> buckets;
    /**
     * bucket hash
     */
    private Prf bucketHash;
    /**
     * fingerprint hash
     */
    private Prf fingerprintHash;
    /**
     * number of inserted elements
     */
    private int size;
    /**
     * item byte length, used for computing compress radio
     */
    private int itemByteLength;

    /**
     * Creates an empty filter.
     *
     * @param envType environment.
     * @param maxSize max number of inserted elements.
     * @param keys    hash keys.
     * @return an empty filter.
     */
    static <X> CuckooFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        MathPreconditions.checkPositive("maxSize", maxSize);
        CuckooFilter<X> cuckooFilter = new CuckooFilter<>();
        cuckooFilter.maxSize = maxSize;
        cuckooFilter.bucketNum = getBucketNum(cuckooFilter.maxSize);
        cuckooFilter.secureRandom = new SecureRandom();
        cuckooFilter.fingerprintHash = PrfFactory.createInstance(envType, FINGERPRINT_BYTE_LENGTH);
        cuckooFilter.fingerprintHash.setKey(keys[0]);
        cuckooFilter.bucketHash = PrfFactory.createInstance(envType, Integer.BYTES);
        cuckooFilter.bucketHash.setKey(keys[1]);
        // initialize buckets
        cuckooFilter.buckets = IntStream.range(0, cuckooFilter.bucketNum)
            .mapToObj(bucketIndex -> new ArrayList<CuckooFilterEntry>(ENTRIES_PER_BUCKET))
            .collect(Collectors.toCollection(ArrayList::new));
        cuckooFilter.size = 0;
        cuckooFilter.itemByteLength = 0;

        return cuckooFilter;
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> CuckooFilter<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkGreaterOrEqual("byteArrayList.size", byteArrayList.size(), 6);
        CuckooFilter<X> cuckooFilter = new CuckooFilter<>();
        // type
        byteArrayList.remove(0);
        // max size
        cuckooFilter.maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        cuckooFilter.bucketNum = getBucketNum(cuckooFilter.maxSize);
        // size
        cuckooFilter.size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // item byte length
        cuckooFilter.itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // fingerprint hash key
        byte[] fingerprintHashKey = byteArrayList.remove(0);
        cuckooFilter.fingerprintHash = PrfFactory.createInstance(envType, FINGERPRINT_BYTE_LENGTH);
        cuckooFilter.fingerprintHash.setKey(fingerprintHashKey);
        // bucket hash key
        byte[] bucketHashKey = byteArrayList.remove(0);
        cuckooFilter.bucketHash = PrfFactory.createInstance(envType, Integer.BYTES);
        cuckooFilter.bucketHash.setKey(bucketHashKey);
        cuckooFilter.secureRandom = new SecureRandom();
        // elements
        MathPreconditions.checkEqual(
            "element num", "desired num",
            byteArrayList.size(), cuckooFilter.bucketNum * ENTRIES_PER_BUCKET
        );
        ByteBuffer[] bucketFlattenedElements = byteArrayList.stream().map(ByteBuffer::wrap).toArray(ByteBuffer[]::new);
        cuckooFilter.buckets = IntStream.range(0, cuckooFilter.bucketNum)
            .mapToObj(bucketIndex -> {
                ArrayList<CuckooFilterEntry> bucket = new ArrayList<>(ENTRIES_PER_BUCKET);
                IntStream.range(0, ENTRIES_PER_BUCKET).forEach(index -> {
                    // empty elements are set to 0
                    if (!bucketFlattenedElements[bucketIndex * ENTRIES_PER_BUCKET + index].equals(ZERO_BYTE_BUFFER)) {
                        bucket.add(
                            new CuckooFilterEntry(bucketFlattenedElements[bucketIndex * ENTRIES_PER_BUCKET + index])
                        );
                    }
                });
                return bucket;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        byteArrayList.clear();

        return cuckooFilter;
    }

    private CuckooFilter() {
        // empty
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> cuckooFilterList = new LinkedList<>();
        // type
        cuckooFilterList.add(IntUtils.intToByteArray(getFilterType().ordinal()));
        // max size
        cuckooFilterList.add(IntUtils.intToByteArray(maxSize));
        // size
        cuckooFilterList.add(IntUtils.intToByteArray(size));
        // item byte length
        cuckooFilterList.add(IntUtils.intToByteArray(itemByteLength));
        // fingerprint hash
        cuckooFilterList.add(BytesUtils.clone(fingerprintHash.getKey()));
        // bucket hash
        cuckooFilterList.add(BytesUtils.clone(bucketHash.getKey()));
        // elements
        IntStream.range(0, bucketNum).forEach(bucketIndex -> {
            // empty elements are replaced with 0
            List<CuckooFilterEntry> bucket = buckets.get(bucketIndex);
            int remainSize = ENTRIES_PER_BUCKET - bucket.size();
            for (CuckooFilterEntry cuckooFilterEntry : bucket) {
                cuckooFilterList.add(BytesUtils.clone(cuckooFilterEntry.getFingerprint().array()));
            }
            while (remainSize > 0) {
                cuckooFilterList.add(new byte[0]);
                remainSize--;
            }
        });

        return cuckooFilterList;
    }

    @Override
    public FilterFactory.FilterType getFilterType() {
        return FilterFactory.FilterType.CUCKOO_FILTER;
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
    public boolean mightContain(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int bucketIndex1 = bucketHash.getInteger(objectBytes, bucketNum);
        int fingerPrintHash = bucketHash.getInteger(fingerprint.array(), bucketNum);
        int bucketIndex2 = Math.abs((bucketIndex1 ^ fingerPrintHash) % bucketNum);
        CuckooFilterEntry entry = new CuckooFilterEntry(fingerprint);

        return buckets.get(bucketIndex1).contains(entry) || buckets.get(bucketIndex2).contains(entry);
    }

    @Override
    public void put(T data) {
        MathPreconditions.checkLess("size", size, maxSize);
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int bucketIndex1 = bucketHash.getInteger(objectBytes, bucketNum);
        int fingerprintHash = bucketHash.getInteger(fingerprint.array(), bucketNum);
        int bucketIndex2 = Math.abs((bucketIndex1 ^ fingerprintHash) % bucketNum);
        // if bucket[i_1] or bucket[i_2] has an empty entry, then add f to that bucket
        if (buckets.get(bucketIndex1).size() < ENTRIES_PER_BUCKET) {
            buckets.get(bucketIndex1).add(new CuckooFilterEntry(fingerprint));
            size++;
            itemByteLength += objectBytes.length;
        } else if (buckets.get(bucketIndex2).size() < ENTRIES_PER_BUCKET) {
            buckets.get(bucketIndex2).add(new CuckooFilterEntry(fingerprint));
            size++;
            itemByteLength += objectBytes.length;
        } else {
            // i = randomly pick i_1 or i_2
            int choiceIndex = secureRandom.nextBoolean() ? bucketIndex1 : bucketIndex2;
            List<CuckooFilterEntry> choiceBucket = buckets.get(choiceIndex);
            byte[] addedFingerprintBytes = Arrays.copyOf(fingerprint.array(), fingerprint.array().length);
            byte[] choiceFingerprintBytes;
            int choiceEntryIndex;
            for (int count = 0; count < MAX_NUM_KICKS; count++) {
                // randomly select an entry e from bucket[i]
                choiceEntryIndex = secureRandom.nextInt(ENTRIES_PER_BUCKET);
                // 将指纹值拷贝出来
                ByteBuffer choiceFingerprint = choiceBucket.remove(choiceEntryIndex).getFingerprint();
                choiceFingerprintBytes = choiceFingerprint.array();
                // 将待插入的指纹插入到新的位置中
                ByteBuffer copyAddedFingerprint = ByteBuffer.wrap(BytesUtils.clone(addedFingerprintBytes));
                choiceBucket.add(new CuckooFilterEntry(copyAddedFingerprint));
                // 踢出再插入元素后，哈希桶中元素的数量应该仍然为ENTRIES_PER_BUCKET
                Preconditions.checkArgument(choiceBucket.size() == ENTRIES_PER_BUCKET);
                addedFingerprintBytes = BytesUtils.clone(choiceFingerprintBytes);
                int choiceFingerprintHash = bucketHash.getInteger(choiceFingerprintBytes, bucketNum);
                choiceIndex = Math.abs((choiceIndex ^ choiceFingerprintHash) % bucketNum);
                choiceBucket = buckets.get(choiceIndex);
                // bucket[i] has an empty entry, then add f to that bucket
                if (choiceBucket.size() < ENTRIES_PER_BUCKET) {
                    choiceBucket.add(new CuckooFilterEntry(ByteBuffer.wrap(addedFingerprintBytes)));
                    size++;
                    itemByteLength += objectBytes.length;
                    return;
                }
                // 如果到达这个位置，意味着踢出元素放置到新的哈希桶中后，新的哈希桶元素数量仍然达到ENTRIES_PER_BUCKET，需要重复踢出
            }
            // 如果到达这个位置，意味着不能再踢出元素了
            throw new IllegalArgumentException("Cannot add item, exceeding max tries: " + data);
        }
    }

    @Override
    public double ratio() {
        // 当前占用的字节长度等于已插入元素的数量乘以指纹长度，加上空占位符的数量
        int cuckooFilterByteLength = size * FINGERPRINT_BYTE_LENGTH
            + (bucketNum * ENTRIES_PER_BUCKET - size) * CommonUtils.getByteLength(1);
        return ((double) cuckooFilterByteLength) / itemByteLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CuckooFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        CuckooFilter<T> that = (CuckooFilter<T>) obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.fingerprintHash.getPrfType(), that.fingerprintHash.getPrfType())
            .append(this.fingerprintHash.getKey(), that.fingerprintHash.getKey())
            .append(this.bucketHash.getPrfType(), that.bucketHash.getPrfType())
            .append(this.bucketHash.getKey(), that.bucketHash.getKey());
        // 因为插入顺序是没关系的，因此要变成集合
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            equalsBuilder.append(
                new HashSet<>(this.buckets.get(buckedIndex)),
                new HashSet<>(that.buckets.get(buckedIndex))
            )
        );
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder
            .append(maxSize)
            .append(size)
            .append(itemByteLength)
            .append(fingerprintHash.getPrfType())
            .append(fingerprintHash.getKey())
            .append(bucketHash.getKey());
        // 因为插入顺序是没关系的，因此要变成集合
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            hashCodeBuilder.append(new HashSet<>(buckets.get(buckedIndex))));
        return hashCodeBuilder.toHashCode();
    }
}
