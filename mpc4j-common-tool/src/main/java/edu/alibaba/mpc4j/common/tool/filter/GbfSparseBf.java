package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;
import java.util.stream.IntStream;


/**
 * 稀疏乱码布隆过滤器（Sparse Garbled Bloom Filter, GBF）方案。原始论文：
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol. CCS 2013.
 * ACM, 2013 pp. 789-800.
 *
 * @author Qixian Zhou
 * @date 2023/5/4
 */
public class GbfSparseBf<T> implements MergeFilter<T> {
	/**
	 * 当布隆过滤器的最优比特数量m = n log_2(e) * log_2(1/p)时，布隆过滤器的最优哈希函数数量 = log_2(1/p)，即安全常数
	 */
	static final int HASH_NUM = CommonConstants.STATS_BIT_LENGTH;

	/**
	 * 计算布隆过滤器的比特数量m，失效率默认为统计安全常数。
	 *
	 * @param n 期望插入的元素数量。
	 * @return 布隆过滤器的最优比特数量m，此数量可以被{@code Byte.SIZE}整除。
	 */
	public static int bitSize(int n) {
		int bitLength = (int) Math.ceil(n * CommonConstants.STATS_BIT_LENGTH / Math.log(2));
		return CommonUtils.getByteLength(bitLength) * Byte.SIZE;
	}

	/**
	 * 插入的最大元素数量
	 */
	private int maxSize;
	/**
	 * 哈希函数的输出范围
	 */
	private int m;
	/**
	 * 已经插入的元素数量
	 */
	private int size;
	/**
	 * 用字节数组表示的布隆过滤器
	 */
	private byte[] gbfSparseBfBytes;
	/**
	 * 布隆过滤器的哈希函数
	 */
	private Prf[] hashes;
	/**
	 * 原始元素的字节长度，用于计算压缩比例
	 */
	private int itemByteLength;

	/**
	 * 创建一个空的布隆过滤器。
	 *
	 * @param envType 环境类型。
	 * @param maxSize 插入的最大元素数量。
	 * @param keys    哈希密钥。
	 * @return 空的布隆过滤器。
	 */
	public static <X> GbfSparseBf<X> create(EnvType envType, int maxSize, byte[][] keys) {
		assert maxSize > 0;
		assert keys.length == HASH_NUM;
		GbfSparseBf<X> gbfSparseBf = new GbfSparseBf<>();
		gbfSparseBf.maxSize = maxSize;
		gbfSparseBf.m = GbfSparseBf.bitSize(gbfSparseBf.maxSize);
		gbfSparseBf.gbfSparseBfBytes = new byte[gbfSparseBf.m / Byte.SIZE];
		gbfSparseBf.hashes = Arrays.stream(keys).map(key -> {
					Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
					hash.setKey(key);
					return hash;
				})
				.toArray(Prf[]::new);
		// 将布隆过滤器初始化为全0
		Arrays.fill(gbfSparseBf.gbfSparseBfBytes, (byte) 0x00);
		gbfSparseBf.size = 0;
		gbfSparseBf.itemByteLength = 0;

		return gbfSparseBf;
	}

	/**
	 * 将用{@code List<byte[]>}表示的过滤器转换为布隆过滤器。
	 *
	 * @param envType       环境类型。
	 * @param byteArrayList 用{@code List<byte[]>}表示的过滤器。
	 * @param <X>           过滤器存储元素类型。
	 * @return 布隆过滤器。
	 */
	static <X> GbfSparseBf<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
		Preconditions.checkArgument(byteArrayList.size() == 5 + HASH_NUM);
		GbfSparseBf<X> gbfSparseBf = new GbfSparseBf<>();
		// 移除过滤器类型
		byteArrayList.remove(0);
		// 期望插入的元素数量
		gbfSparseBf.maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
		gbfSparseBf.m = GbfSparseBf.bitSize(gbfSparseBf.maxSize);
		// 已经插入的元素数量
		gbfSparseBf.size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
		// 原始元素的字节长度
		gbfSparseBf.itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
		// 当前存储状态
		gbfSparseBf.gbfSparseBfBytes = byteArrayList.remove(0);
		// 密钥
		byte[][] keys = byteArrayList.toArray(new byte[0][]);
		gbfSparseBf.hashes = Arrays.stream(keys).map(key -> {
					Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
					hash.setKey(key);
					return hash;
				})
				.toArray(Prf[]::new);

		return gbfSparseBf;
	}

	private GbfSparseBf() {
		// empty
	}

	@Override
	public List<byte[]> toByteArrayList() {
		List<byte[]> byteArrayList = new LinkedList<>();
		// 过滤器类型
		byteArrayList.add(IntUtils.intToByteArray(getFilterType().ordinal()));
		// 预计插入的元素数量
		byteArrayList.add(IntUtils.intToByteArray(maxSize()));
		// 已经插入的元素数量
		byteArrayList.add(IntUtils.intToByteArray(size()));
		// 原始元素的字节长度
		byteArrayList.add(IntUtils.intToByteArray(itemByteLength));
		// 当前存储状态
		byteArrayList.add(BytesUtils.clone(gbfSparseBfBytes));
		// 密钥
		for (Prf hash : hashes) {
			byteArrayList.add(BytesUtils.clone(hash.getKey()));
		}

		return byteArrayList;
	}

	@Override
	public FilterType getFilterType() {
		return FilterType.GBF_SPARSE_BF;
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
		int[] sparseIndexes = sparseIndexes(objectBytes);
		// 依次验证每个哈希结果所对应的比特位是否为true
		for (int index : sparseIndexes) {
			if (!BinaryUtils.getBoolean(gbfSparseBfBytes, index)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public synchronized void put(T data) {
		assert size < maxSize;
		if (mightContain(data)) {
			throw new IllegalArgumentException("Insert might duplicate item: " + data);
		}
		// mightContain已经检查了重复元素，直接插入
		byte[] objectBytes = ObjectUtils.objectToByteArray(data);
		int[] sparseIndexes = sparseIndexes(objectBytes);
		for (int index : sparseIndexes) {
			if (!BinaryUtils.getBoolean(gbfSparseBfBytes, index)) {
				BinaryUtils.setBoolean(gbfSparseBfBytes, index, true);
			}
		}
		// 更新存储信息
		itemByteLength += objectBytes.length;
		size++;
	}

	public int[] sparseIndexes(byte[] objectBytes) {
		int[] sparsePositions = new int[HASH_NUM];
		Set<Integer> positionSet = new HashSet<>(HASH_NUM);
		sparsePositions[0] = hashes[0].getInteger(0, objectBytes, m);
		positionSet.add(sparsePositions[0]);
		// generate k distinct positions
		for (int i = 1; i < HASH_NUM; i++) {
			int hiIndex = 0;
			do {
				sparsePositions[i] = hashes[i].getInteger(hiIndex, objectBytes, m);
				hiIndex++;
			} while (positionSet.contains(sparsePositions[i]));
			positionSet.add(sparsePositions[i]);
		}
		return sparsePositions;
	}

	@Override
	public void merge(MergeFilter<T> otherFilter) {
		assert otherFilter instanceof GbfSparseBf;
		GbfSparseBf<T> otherGbfSparseBf = (GbfSparseBf<T>) otherFilter;
		// 预计插入的元素数量应该一致，否则哈希长度不一致
		assert maxSize == otherGbfSparseBf.maxSize;
		IntStream.range(0, HASH_NUM).forEach(hashIndex -> {
			// 哈希函数的类型相同
			assert hashes[hashIndex].getPrfType().equals(otherGbfSparseBf.hashes[hashIndex].getPrfType());
			// 哈希函数的密钥相同
			assert Arrays.equals(hashes[hashIndex].getKey(), otherGbfSparseBf.hashes[hashIndex].getKey());
		});
		assert maxSize >= size + otherGbfSparseBf.size;
		// 合并布隆过滤器
		BytesUtils.ori(gbfSparseBfBytes, otherGbfSparseBf.gbfSparseBfBytes);
		size += otherGbfSparseBf.size;
		itemByteLength += otherGbfSparseBf.itemByteLength;
	}

	@Override
	public double ratio() {
		return (double) gbfSparseBfBytes.length / itemByteLength;
	}

	/**
	 * 返回布隆过滤器存储的字节数组。
	 *
	 * @return 布隆过滤器存储的字节数组。
	 */
	public byte[] getBytes() {
		return gbfSparseBfBytes;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GbfSparseBf)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		//noinspection unchecked
		GbfSparseBf<T> that = (GbfSparseBf<T>) obj;
		EqualsBuilder equalsBuilder = new EqualsBuilder();
		equalsBuilder.append(this.maxSize, that.maxSize)
				.append(this.size, that.size)
				.append(this.itemByteLength, that.itemByteLength)
				.append(this.gbfSparseBfBytes, that.gbfSparseBfBytes);
		IntStream.range(0, HASH_NUM).forEach(hashIndex -> {
			equalsBuilder.append(hashes[hashIndex].getPrfType(), that.hashes[hashIndex].getPrfType());
			equalsBuilder.append(hashes[hashIndex].getKey(), that.hashes[hashIndex].getKey());
		});
		return equalsBuilder.isEquals();
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
		hashCodeBuilder.append(maxSize)
				.append(size)
				.append(itemByteLength)
				.append(gbfSparseBfBytes);
		IntStream.range(0, HASH_NUM).forEach(hashIndex -> {
			hashCodeBuilder.append(hashes[hashIndex].getPrfType());
			hashCodeBuilder.append(hashes[hashIndex].getKey());
		});
		return hashCodeBuilder.toHashCode();
	}
}
