package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * CMG21关键词索引PIR方案参数。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirParams {
    /**
     * table params : 哈希算法数目
     */
    private final int hashNum;
    /**
     * table params : 哈希桶数目
     */
    private final int binNum;
    /**
     * table params : 每个哈希桶内分块的最大元素个数
     */
    private final int maxPartitionSizePerBin;
    /**
     * item params : 元素编码后占的卡槽个数
     */
    private final int itemEncodedSlotSize;
    /**
     * query params : Paterson-Stockmeyer方法的低阶值
     */
    private final int psLowDegree;
    /**
     * table params : 查询幂次方
     */
    private final int[] queryPowers;
    /**
     * SEAL params : 明文模数
     */
    private final int plainModulus;
    /**
     * SEAL params : 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * SEAL params : 系数模数的比特值
     */
    private final int[] coeffModulusBits;
    /**
     * 单次查询的最大查询元素数量
     */
    private final int maxItemPerQuery;

    public Cmg21KwPirParams(int hashNum, int binNum, int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                            int[] queryPowers, int plainModulus, int polyModulusDegree, int[] coeffModulusBits,
                            int maxItemPerQuery) {
        assert hashNum >= 1 && hashNum <= 3 : "hash num must be in {1, 2, 3}";
        assert binNum > 0 : "bin num should be greater than 0";
        assert itemEncodedSlotSize >= 2 && itemEncodedSlotSize <= 32 : "the size of slots for encoded item should "
            + "smaller than or equal 32 and greater than or equal 2";
        assert psLowDegree <= maxPartitionSizePerBin : "psLowDegree should be smaller or equal than " +
            "maxPartitionSizePerBin";
        // 检查query powers是否合理
        checkQueryPowers(queryPowers, psLowDegree);
        assert (polyModulusDegree & (polyModulusDegree-1)) == 0 : "polyModulusDegree is not a power of two";
        assert plainModulus % (2 * polyModulusDegree) == 1 : "plainModulus should be a specific prime number to " +
            "supports batching ";
        // 元素的比特长度为 itemEncodedSlotSize*floor(log_2(plain_modulus))
        int encodedBitLength = itemEncodedSlotSize * (int) Math.floor(Math.log(plainModulus) / Math.log(2));
        assert encodedBitLength >= 80 && encodedBitLength <= 128 : "encoded bits should greater than or equal 80 " +
            "and smaller than or equal 128";
        assert binNum % (polyModulusDegree / itemEncodedSlotSize) == 0 : "binNum should be a multiple of " +
            "polyModulusDegree / itemEncodedSlotSize";
        assert maxItemPerQuery > 0;
        this.hashNum = hashNum;
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.itemEncodedSlotSize = itemEncodedSlotSize;
        this.psLowDegree = psLowDegree;
        this.queryPowers = queryPowers;
        this.plainModulus = plainModulus;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusBits = coeffModulusBits;
        this.maxItemPerQuery = maxItemPerQuery;
    }

    public static final Cmg21KwPirParams ONE_MILLION_4096_32 = new Cmg21KwPirParams(3, 6552, 770,
        5,
        26, new int[]{1, 5, 8, 27, 135},
        1785857, 8192, new int[]{50, 56, 56, 50},
        4096
    );

    public static final Cmg21KwPirParams ONE_MILLION_1_32 = new Cmg21KwPirParams(1, 1638, 228,
        5,
        0, new int[]{1, 3, 8, 19, 33, 39, 92, 102},
        65537, 8192, new int[]{56, 48, 48},
        1
    );

    /**
     * 返回哈希算法数目。
     *
     * @return 哈希算法数目。
     */
    public int getHashNum() {
        return hashNum;
    }

    /**
     * 返回哈希桶数目。
     *
     * @return 哈希桶数目。
     */
    public int getBinNum() {
        return binNum;
    }

    /**
     * 返回每个哈希桶内分块的最大元素个数。
     *
     * @return 每个哈希桶内分块的最大元素个数。
     */
    public int getMaxPartitionSizePerBin() {
        return maxPartitionSizePerBin;
    }

    /**
     * 返回元素编码后占的卡槽个数。
     *
     * @return 元素编码后占的卡槽个数。
     */
    public int getItemEncodedSlotSize() {
        return itemEncodedSlotSize;
    }

    /**
     * 返回Paterson-Stockmeyer方法的低阶值。
     *
     * @return Paterson-Stockmeyer方法的低阶值。
     */
    public int getPsLowDegree() {
        return psLowDegree;
    }

    /**
     * 返回查询幂次方。
     *
     * @return 查询幂次方。
     */
    public int[] getQueryPowers() {
        return queryPowers;
    }

    /**
     * 返回明文模数。
     *
     * @return 明文模数。
     */
    public int getPlainModulus() {
        return plainModulus;
    }

    /**
     * 返回多项式阶。
     *
     * @return 多项式阶。
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * 返回系数模数的比特值。
     *
     * @return 系数模数的比特值。
     */
    public int[] getCoeffModulusBits() {
        return coeffModulusBits;
    }

    /**
     * 返回单次查询的最大查询元素数量。
     *
     * @return 单次查询的最大查询元素数量。
     */
    public int getMaxItemPerQuery() { return maxItemPerQuery; }

    @Override
    public String toString() {
        return "Parameters chosen:" + "\n" +
            "  - hash_bin_params: {" + "\n" +
            "     - hash_num : " + hashNum + "\n" +
            "     - bin_num : " + binNum + "\n" +
            "     - max_items_per_bin : " + maxPartitionSizePerBin + "\n" +
            "  }" + "\n" +
            "  - item_params: {" + "\n" +
            "     - felts_per_item : " + itemEncodedSlotSize + "\n" +
            "  }" + "\n" +
            "  - query_params: {" + "\n" +
            "     - ps_low_degree : " + psLowDegree + "\n" +
            "     - query_powers : " + Arrays.toString(queryPowers) + "\n" +
            "  }" + "\n" +
            "  - seal_params: {" + "\n" +
            "     - plain_modulus : " + plainModulus + "\n" +
            "     - poly_modulus_degree : " + polyModulusDegree + "\n" +
            "     - coeff_modulus_bits : " + Arrays.toString(coeffModulusBits) + "\n" +
            "  }" + "\n";
    }

    /**
     * 返回哈希桶条目中元素对应的编码数组。
     *
     * @param hashBinEntry 哈希桶条目。
     * @param isReceiver   是否为接收方。
     * @return 哈希桶条目中元素对应的编码数组。
     */
    public long[] getHashBinEntryEncodedArray(HashBinEntry<ByteBuffer> hashBinEntry, boolean isReceiver) {
        long[] encodedResult = new long[itemEncodedSlotSize];
        int encodedItemBitLength = (BigInteger.valueOf(plainModulus).bitLength()-1) * itemEncodedSlotSize;
        assert encodedItemBitLength >= 80;
        int shiftBits = BigInteger.valueOf(plainModulus).bitLength() - 1;
        // 判断是否为空桶
        if (hashBinEntry.getHashIndex() != -1) {
            // the index of the hash function should be [0, 1, 2], index 3 is used for dummy elements
            assert(hashBinEntry.getHashIndex() < 3);
            BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(hashBinEntry.getItem().array());
            input = input.shiftRight(input.bitLength() - encodedItemBitLength);
            // encode the input itself, except for the last bucket_count_log() bits
            for (int i = 0; i < itemEncodedSlotSize; i++) {
                encodedResult[i] = input.mod(BigInteger.ONE.shiftLeft(shiftBits)).longValueExact();
                input = input.shiftRight(shiftBits);
            }
        } else {
            // for the dummy element, we use a non-existent hash function index (3)
            // and 0 or 1 for the input depending on whether it's the sender or the receiver who needs a dummy.
            encodedResult = IntStream.range(0, itemEncodedSlotSize).mapToLong(i -> 3L | ((isReceiver ? 1L : 0L) << 2)).toArray();
        }
        for (int i = 0; i < itemEncodedSlotSize; i++) {
            assert (encodedResult[i] < plainModulus);
        }
        return encodedResult;
    }

    /**
     * 返回标签编码数组。
     *
     * @param labelBytes   标签字节。
     * @param partitionNum 分块数目。
     * @return 标签编码数组。
     */
    public long[][] encodeLabel(ByteBuffer labelBytes, int partitionNum) {
        long[][] encodedResult = new long[partitionNum][itemEncodedSlotSize];
        int shiftBits = (int) Math.ceil(labelBytes.array().length*8.0 / (itemEncodedSlotSize*partitionNum));
        BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(labelBytes.array());
        for (int i = 0; i < partitionNum; i++) {
            for (int j = 0; j < itemEncodedSlotSize; j++) {
                encodedResult[i][j] = input.mod(BigInteger.ONE.shiftLeft(shiftBits)).longValueExact();
                input = input.shiftRight(shiftBits);
            }
        }
        for (int i = 0; i < partitionNum; i++) {
            for (int j = 0; j < itemEncodedSlotSize; j++) {
                assert (encodedResult[i][j] < plainModulus);
            }
        }
        return encodedResult;
    }

    /**
     * 检查问询幂次方的有效性。
     *
     * @param sourcePowers 问询幂次方数组（不一定有序）。
     * @param psLowDegree  最低问询阶。
     */
    public static void checkQueryPowers(int[] sourcePowers, int psLowDegree) {
        int[] sortSourcePowers = Arrays.stream(sourcePowers)
            .peek(sourcePower -> {
                assert sourcePower > 0 : "query power must be greater than 0: " + sourcePower;
            })
            .distinct()
            .sorted()
            .toArray();
        assert sortSourcePowers.length == sourcePowers.length : "query powers must be distinct";
        assert sortSourcePowers[0] == 1 : "query powers must contain 1";
        for (int sourcePower : sourcePowers) {
            assert sourcePower <= psLowDegree || sourcePower % (psLowDegree + 1) == 0
                : "query powers中大于ps_low_degree的输入应能被ps_low_degree + 1整除: " + sourcePower;
        }
    }
}
