package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.util.Map;
import java.util.Set;

/**
 * Distinct Garbled Bloom Filter (GBF) DOKVS. The original scheme is described in the following paper:
 * <p>
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol. CCS 2013.
 * ACM, 2013 pp. 789-800.
 * </p>
 * In this implementation, we require that any inputs have constant distinct positions in the Garbled Bloom Filter.
 * This requirement is used in the following paper:
 * <p>
 * Lepoint, Tancrede, Sarvar Patel, Mariana Raykova, Karn Seth, and Ni Trieu. Private join and compute from PIR with
 * default. ASIACRYPT 2021, Part II, pp. 605-634. Cham: Springer International Publishing, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
class DistinctGbfDokvs<T> extends AbstractGf2eDokvs<T> implements SparseConstantGf2eDokvs<T> {
    /**
     * Garbled Bloom Filter needs λ hashes
     */
    private static final int SPARSE_HASH_NUM = CommonConstants.STATS_BIT_LENGTH;
    /**
     * we only need to use one hash key
     */
    static final int HASH_KEY_NUM = 1;

    /**
     * Gets m for the given n.
     *
     * @param n number of key-value pairs.
     * @return m.
     */
    static int getM(int n) {
        MathPreconditions.checkPositive("n", n);
        // m = n / ln(2) * σ, flooring so that m % Byte.SIZE = 0.
        return CommonUtils.getByteLength(
            (int) Math.ceil(n * CommonConstants.STATS_BIT_LENGTH / Math.log(2))
        ) * Byte.SIZE;
    }

    /**
     * hashes
     */
    private final Prf hash;

    public DistinctGbfDokvs(EnvType envType, int n, int l, byte[] key) {
        super(n, getM(n), l);
        hash = PrfFactory.createInstance(envType, Integer.BYTES * SPARSE_HASH_NUM);
        hash.setKey(key);
    }

    @Override
    public int sparsePositionRange() {
        return m;
    }

    @Override
    public int[] sparsePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] hashes = IntUtils.byteArrayToIntArray(hash.getBytes(keyBytes));
        // we now use the method provided in VOLE-PSI to get distinct hash indexes
        for (int j = 0; j < SPARSE_HASH_NUM; j++) {
            // hj = r % (m - j)
            int modulus = m - j;
            int hj = Math.abs(hashes[j] % modulus);
            int i = 0;
            int end = j;
            // for each previous hi <= hj, we set hj = hj + 1.
            while (i != end) {
                if (hashes[i] <= hj) {
                    hj++;
                } else {
                    break;
                }
                i++;
            }
            // now we now that all hi > hj, we place the value
            while (i != end) {
                hashes[end] = hashes[end - 1];
                end--;
            }
            hashes[i] = hj;
        }
        return hashes;
    }

    @Override
    public int sparsePositionNum() {
        return SPARSE_HASH_NUM;
    }

    @Override
    public boolean[] binaryDensePositions(T key) {
        // garbled bloom filter does not contain dense part
        return new boolean[0];
    }

    @Override
    public int maxDensePositionNum() {
        return 0;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        keyValueMap.values().forEach(x -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(x, byteL, l)));
        Set<T> keySet = keyValueMap.keySet();
        // compute positions for all keys, create shares.
        byte[][] storage = new byte[m][];
        for (T key : keySet) {
            byte[] finalShare = BytesUtils.clone(keyValueMap.get(key));
            assert finalShare.length == byteL;
            int[] sparsePositions = sparsePositions(key);
            int emptySlot = -1;
            for (int position : sparsePositions) {
                if (storage[position] == null && emptySlot == -1) {
                    // if we find an empty position, reserve the location for finalShare）
                    emptySlot = position;
                } else if (storage[position] == null) {
                    // if the current position is null, generate a new share
                    storage[position] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                    BytesUtils.xori(finalShare, storage[position]);
                } else {
                    // if the current position is not null, reuse the share
                    BytesUtils.xori(finalShare, storage[position]);
                }
            }
            if (emptySlot == -1) {
                // we cannot find an empty position, which happens with probability 1 - 2^{-λ}
                throw new ArithmeticException("Failed to encode Key-Value Map, cannot find empty slot");
            }
            storage[emptySlot] = finalShare;
        }
        // pad random elements in all empty positions.
        for (int i = 0; i < m; i++) {
            if (storage[i] == null) {
                storage[i] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }

        return storage;
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        assert storage.length == getM();
        int[] sparsePositions = sparsePositions(key);
        byte[] value = new byte[byteL];
        for (int position : sparsePositions) {
            BytesUtils.xori(value, storage[position]);
        }
        assert BytesUtils.isFixedReduceByteArray(value, byteL, l);
        return value;
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.DISTINCT_GBF;
    }
}
