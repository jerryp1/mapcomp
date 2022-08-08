package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiParams;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * CMG21非平衡PSI方案参数。
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class Cmg21UpsiParams implements UpsiParams {
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 哈希桶数目
     */
    private final int binNum;
    /**
     * 每个哈希桶内分块的最大元素个数
     */
    private final int maxPartitionSizePerBin;
    /**
     * 元素编码后占的卡槽个数
     */
    private final int itemEncodedSlotSize;
    /**
     * Paterson-Stockmeyer方法的低阶值
     */
    private final int psLowDegree;
    /**
     * 查询幂次方
     */
    private final int[] queryPowers;
    /**
     * 明文模数
     */
    private final int plainModulus;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * 系数模数的比特值
     */
    private final int[] coeffModulusBits;
    /**
     * 服务端预估数量
     */
    private final int expectServerSize;
    /**
     * 客户端最大数量
     */
    private final int maxClientSize;

    public Cmg21UpsiParams(CuckooHashBinType cuckooHashBinType, int binNum, int maxPartitionSizePerBin,
                           int itemEncodedSlotSize, int psLowDegree,
                           int[] queryPowers, int plainModulus, int polyModulusDegree, int[] coeffModulusBits,
                           int expectServerSize, int maxClientSize) {
        assert cuckooHashBinType.equals(CuckooHashBinType.NAIVE_3_HASH)
            || cuckooHashBinType.equals(CuckooHashBinType.NO_STASH_ONE_HASH)
            : CuckooHashBinType.class.getSimpleName() + "only support "
            + CuckooHashBinType.NO_STASH_ONE_HASH + " or " + CuckooHashBinType.NAIVE_3_HASH;
        assert binNum > 0 : "bin num should be greater than 0";
        assert itemEncodedSlotSize >= 2 && itemEncodedSlotSize <= 32 : "the size of slots for encoded item should "
            + "smaller than or equal 32 and greater than or equal 2";
        assert psLowDegree <= maxPartitionSizePerBin : "psLowDegree should be smaller or equal than " +
            "maxPartitionSizePerBin";
        // 检查query powers是否合理
        checkQueryPowers(queryPowers, psLowDegree);
        assert (polyModulusDegree & (polyModulusDegree - 1)) == 0 : "polyModulusDegree is not a power of two";
        assert plainModulus % (2 * polyModulusDegree) == 1 : "plainModulus should be a specific prime number to " +
            "supports batching ";
        int encodedBitLength = itemEncodedSlotSize * (int) Math.floor(Math.log(plainModulus) / Math.log(2));
        assert encodedBitLength >= 80 && encodedBitLength <= 128 : "encoded bits should greater than or equal 80 " +
            "and smaller than or equal 128";
        assert binNum % (polyModulusDegree / itemEncodedSlotSize) == 0 : "binNum should be a multiple of " +
            "polyModulusDegree / itemEncodedSlotSize";
        assert expectServerSize > 0 : "ExpectServerSize must be greater than 0: " + expectServerSize;
        int maxItemSize = CuckooHashBinFactory.getMaxItemSize(cuckooHashBinType, binNum);
        assert maxClientSize > 0 && maxClientSize <= maxItemSize
            : "MaxClientElementSize must be in range (0, " + maxItemSize + "]: " + maxClientSize;
        this.cuckooHashBinType = cuckooHashBinType;
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.itemEncodedSlotSize = itemEncodedSlotSize;
        this.psLowDegree = psLowDegree;
        this.queryPowers = queryPowers;
        this.plainModulus = plainModulus;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusBits = coeffModulusBits;
        this.expectServerSize = expectServerSize;
        this.maxClientSize = maxClientSize;
    }

    /**
     * 服务端2K，客户端最大元素数量1
     */
    public static final Cmg21UpsiParams SERVER_2K_CLIENT_1 = new Cmg21UpsiParams(
        CuckooHashBinType.NO_STASH_ONE_HASH, 512, 15,
        8,
        0, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
        40961, 4096, new int[]{24, 24, 24},
        2000, 1
    );

    /**
     * 服务端100K，客户端最大元素数量1
     */
    public static final Cmg21UpsiParams SERVER_100K_CLIENT_1 = new Cmg21UpsiParams(
        CuckooHashBinType.NO_STASH_ONE_HASH, 512, 20,
        8,
        0, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20},
        40961, 4096, new int[]{24, 24, 24},
        100000, 1
    );

    /**
     * 服务端1M，客户端最大元素数量1K，计算量最优
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_1K_CMP = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 2046, 101,
        6,
        0, new int[]{1, 3, 4, 5, 8, 14, 20, 26, 32, 38, 44, 47, 48, 49, 51, 52},
        40961, 4096, new int[]{40, 32, 32},
        1000000, 1024
    );

    /**
     * 服务端1M，客户端最大元素数量1K，通信量最优
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_1K_COM = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 1638, 125,
        5,
        5, new int[]{1, 2, 3, 4, 5, 6, 18, 30, 42, 54, 60},
        188417, 4096, new int[]{48, 36, 25},
        1000000, 1024
    );

    /**
     * 服务端1M，客户端最大元素数量11041
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_11041 = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 16384, 98,
        4,
        8, new int[]{1, 3, 4, 9, 27},
        1785857, 8192, new int[]{56, 56, 24, 24},
        1000000, 11041
    );

    /**
     * 服务端1M，客户端最大元素数量2K，计算量最优
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_2K_CMP = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 3410, 72,
        6,
        0, new int[]{1, 3, 4, 9, 11, 16, 20, 25, 27, 32, 33, 35, 36},
        40961, 4096, new int[]{40, 32, 32},
        1000000, 2048
    );

    /**
     * 服务端1M，客户端最大元素数量2K，通信量最优
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_2K_COM = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 3410, 125,
        6,
        5, new int[]{1, 2, 3, 4, 5, 6, 18, 30, 42, 54, 60},
        65537, 4096, new int[]{48, 30, 30},
        1000000, 2048
    );

    /**
     * 服务端1M，客户端最大元素数量256
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_256 = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 585, 180,
        7,
        0, new int[]{1, 3, 4, 6, 10, 13, 15, 21, 29, 37, 45, 53, 61, 69, 77, 81, 83, 86, 87, 90, 92, 96},
        40961, 4096, new int[]{40, 32, 32},
        1000000, 256
    );

    /**
     * 服务端1M，客户端最大元素数量4K，计算量最优
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_4K_CMP = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 6552, 40,
        5,
        0, new int[]{1, 3, 4, 9, 11, 16, 17, 19, 20},
        65537, 4096, new int[]{48, 30, 30},
        1000000, 4096
    );

    /**
     * 服务端1M，客户端最大元素数量4K，通信量最优
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_4K_COM = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 6825, 98,
        6,
        8, new int[]{1, 3, 4, 9, 27},
        65537, 8192, new int[]{56, 56, 30},
        1000000, 4096
    );

    /**
     * 服务端1M，客户端最大元素数量512，计算量最优
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_512_CMP = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 1364, 128,
        6,
        0, new int[]{1, 3, 4, 5, 8, 14, 20, 26, 32, 38, 44, 50, 56, 59, 60, 61, 63, 64},
        65537, 4096, new int[]{40, 34, 30},
        1000000, 512
    );

    /**
     * 服务端1M，客户端最大元素数量512，通信量最优
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_512_COM = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 1364, 228,
        6,
        4, new int[]{1, 2, 3, 4, 5, 10, 15, 35, 55, 75, 95, 115, 125, 130, 140},
        65537, 4096, new int[]{48, 34, 27},
        1000000, 512
    );

    /**
     * 服务端1M，客户端最大元素数量5535
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_5535 = new Cmg21UpsiParams(
        CuckooHashBinType.NAIVE_3_HASH, 8192, 98,
        4,
        8, new int[]{1, 3, 4, 9, 27},
        1785857, 8192, new int[]{56, 56, 24, 24},
        1000000, 5535
    );

    /**
     * 返回布谷鸟哈希类型。
     *
     * @return 布谷鸟哈希类型。
     */
    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    /**
     * 返回布谷鸟哈希桶的哈希数量。
     *
     * @return 布谷鸟哈希桶的哈希数量。
     */
    public int getCuckooHashKeyNum() {
        return CuckooHashBinFactory.getHashNum(cuckooHashBinType);
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

    @Override
    public String toString() {
        return "Parameters chosen:" + "\n" +
            "  - hash_bin_params: {" + "\n" +
            "     - cuckoo_hash_bin_type : " + cuckooHashBinType + "\n" +
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
    public long[] getHashBinEntryEncodedArray(HashBinEntry<ByteBuffer> hashBinEntry, boolean isReceiver,
                                              SecureRandom secureRandom) {
        long[] encodedArray = new long[itemEncodedSlotSize];
        int bitLength = (BigInteger.valueOf(plainModulus).bitLength() - 1) * itemEncodedSlotSize;
        assert bitLength >= 80;
        int shiftBits = BigInteger.valueOf(plainModulus).bitLength() - 1;
        // 判断是否为空桶
        if (hashBinEntry.getHashIndex() != -1) {
            assert hashBinEntry.getHashIndex() < 3 : "hash index should be [0, 1, 2]";
            BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(hashBinEntry.getItem().array());
            input = input.shiftRight(input.bitLength() - bitLength);
            for (int i = 0; i < itemEncodedSlotSize; i++) {
                encodedArray[i] = input.mod(BigInteger.ONE.shiftLeft(shiftBits)).longValueExact();
                input = input.shiftRight(shiftBits);
            }
        } else {
            IntStream.range(0, itemEncodedSlotSize).forEach(i -> {
                long random = Math.abs(secureRandom.nextLong()) % plainModulus / 8;
                encodedArray[i] = random << 1 | (isReceiver ? 1L : 0L);
            });
        }
        for (int i = 0; i < itemEncodedSlotSize; i++) {
            assert encodedArray[i] < plainModulus : "encoded value must be smaller than plain modulus";
        }
        return encodedArray;
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

    @Override
    public int maxClientSize() {
        return maxClientSize;
    }

    @Override
    public int expectServerSize() {
        return expectServerSize;
    }
}
