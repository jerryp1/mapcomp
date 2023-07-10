package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Clustering blazing fast DOKVS using garbled cuckoo table with 3 hash functions. The construction is from the paper:
 * <p>
 * Raghuraman, Srinivasan, and Peter Rindal. Blazing fast PSI from improved OKVS and subfield VOLE. ACM CCS 2022,
 * pp. 2505-2517.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
class H3ClusterBlazeGctGf2eDokvs<T> implements BinaryGf2eDokvs<T> {
    /**
     * number of sparse hashes
     */
    static final int SPARSE_HASH_NUM = AbstractH3GctGf2eDokvs.SPARSE_HASH_NUM;
    /**
     * number of hash keys, one more key for bin
     */
    static final int HASH_KEY_NUM = AbstractH3GctGf2eDokvs.HASH_KEY_NUM + 1;
    /**
     * expected bin size, i.e., m^* = 2^14
     */
    private static final int EXPECT_BIN_SIZE = 1 << 14;

    /**
     * Gets m.
     *
     * @param n number of key-value pairs.
     * @return m.
     */
    static int getM(int n) {
        int binNum = CommonUtils.getUnitNum(n, EXPECT_BIN_SIZE);
        int binN = MaxBinSizeUtils.expectMaxBinSize(n, binNum);
        int binLm = H3BlazeGctGf2eDokvs.getLm(binN);
        int binRm = H3BlazeGctGf2eDokvs.getRm(binN);
        int binM = binLm + binRm;
        return binNum * binM;
    }

    /**
     * number of key-value pairs.
     */
    private final int n;
    /**
     * bit length of values
     */
    private final int l;
    /**
     * l in byte
     */
    private final int byteL;
    /**
     * parallel encode
     */
    private boolean parallelEncode;
    /**
     * number of bins
     */
    private final int binNum;
    /**
     * number of key-value pairs in each bin
     */
    private final int binN;
    /**
     * right m in each bin
     */
    private final int binRm;
    /**
     * m for each bin
     */
    private final int binM;
    /**
     * size of encode storage.
     */
    private final int m;
    /**
     * bin hash
     */
    private final Prf binHash;
    /**
     * bins
     */
    private final ArrayList<H3BlazeGctGf2eDokvs<T>> bins;

    H3ClusterBlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H3ClusterBlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        // here we only need to require l > 0
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        parallelEncode = false;
        // calculate bin_num and bin_size
        binNum = CommonUtils.getUnitNum(n, EXPECT_BIN_SIZE);
        binN = MaxBinSizeUtils.expectMaxBinSize(n, binNum);
        int binLm = H3BlazeGctGf2eDokvs.getLm(binN);
        binRm = H3BlazeGctGf2eDokvs.getRm(binN);
        binM = binLm + binRm;
        m = binNum * binM;
        // clone keys
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, HASH_KEY_NUM);
        // init bin hash
        binHash = PrfFactory.createInstance(envType, Integer.BYTES);
        binHash.setKey(keys[0]);
        byte[][] cloneKeys = new byte[HASH_KEY_NUM - 1][];
        for (int keyIndex = 0; keyIndex < HASH_KEY_NUM - 1; keyIndex++) {
            cloneKeys[keyIndex] = BytesUtils.clone(keys[keyIndex + 1]);
        }
        // create bins
        Kdf kdf = KdfFactory.createInstance(envType);
        bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                for (int keyIndex = 0; keyIndex < HASH_KEY_NUM - 1; keyIndex++) {
                    cloneKeys[keyIndex] = kdf.deriveKey(cloneKeys[keyIndex]);
                }
                return new H3BlazeGctGf2eDokvs<T>(envType, binN, l, cloneKeys, secureRandom);
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }


    @Override
    public int[] positions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] binPositions = bins.get(binIndex).positions(key);
        return Arrays.stream(binPositions)
            .map(position -> position + binM * binIndex)
            .toArray();
    }

    @Override
    public int maxPositionNum() {
        return SPARSE_HASH_NUM + binNum * binRm;
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.H3_CLUSTER_BLAZE_GCT;
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        this.parallelEncode = parallelEncode;
    }

    @Override
    public boolean getParallelEncode() {
        return parallelEncode;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        keyValueMap.values().forEach(x -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(x, byteL, l)));
        // create and split bins
        ArrayList<Map<T, byte[]>> keyValueMaps = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new ConcurrentHashMap<T, byte[]>(binN))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<Map.Entry<T, byte[]>> keyValueStream = keyValueMap.entrySet().stream();
        keyValueStream = parallelEncode ? keyValueStream.parallel() : keyValueStream;
        keyValueStream.forEach(entry -> {
            byte[] keyByte = ObjectUtils.objectToByteArray(entry.getKey());
            int binIndex = binHash.getInteger(keyByte, binNum);
            keyValueMaps.get(binIndex).put(entry.getKey(), entry.getValue());
        });
        // encode
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallelEncode ? binIndexIntStream.parallel() : binIndexIntStream;
        return binIndexIntStream
            .mapToObj(binIndex -> bins.get(binIndex).encode(keyValueMaps.get(binIndex), doublyEncode))
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] decode(byte[][] storage, int from, int to, T key) {
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        return bins.get(binIndex).decode(storage, binIndex * binM + from, (binIndex + 1) * binM + from, key);
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getM() {
        return m;
    }
}
