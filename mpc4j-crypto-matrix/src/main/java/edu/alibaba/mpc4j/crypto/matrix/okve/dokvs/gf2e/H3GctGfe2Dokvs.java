package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import cc.redberry.rings.linear.LinearSolver;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.H3CuckooTable;
import edu.alibaba.mpc4j.crypto.matrix.okve.tool.BinaryLinearSolver;
import edu.alibaba.mpc4j.crypto.matrix.okve.tool.BinaryMaxLisFinder;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

/**
 * garbled cuckoo table with 2 hash functions. The non-doubly construction is from the following paper:
 * <p>
 * Garimella G, Pinkas B, Rosulek M, et al. Oblivious Key-Value Stores and Amplification for Private Set Intersection.
 * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
 * </p>
 * The doubly-obliviousness construction is form the following paper:
 * <p>
 * Zhang, Cong, Yu Chen, Weiran Liu, Min Zhang, and Dongdai Lin. Linear Private Set Union from Multi-Query Reverse
 * Private Membership Test. USENIX Security 2023.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
class H3GctGfe2Dokvs<T> extends AbstractGf2eDokvs<T> implements SparseGf2eDokvs<T> {
    /**
     * number of sparse hashes
     */
    private static final int SPARSE_HASH_NUM = 3;
    /**
     * number of total hashes
     */
    static final int TOTAL_HASH_NUM = SPARSE_HASH_NUM + 1;
    /**
     * left ε, i.e., ε_l.
     */
    private static final double LEFT_EPSILON = 1.3;
    /**
     * right ε, i.e., ε_r.
     */
    private static final double RIGHT_EPSILON = 0.5;

    /**
     * Gets left m. The result is shown in Section 5.4 of the paper.
     *
     * @param n number of key-value pairs.
     * @return lm = (1 + ε_l) * n, with lm % Byte.SIZE == 0.
     */
    static int getLm(int n) {
        MathPreconditions.checkPositive("n", n);
        return CommonUtils.getByteLength((int) Math.ceil((1 + LEFT_EPSILON) * n)) * Byte.SIZE;
    }

    /**
     * Gets right m. The result is shown in Section 5.4 of the paper.
     *
     * @param n number of key-value pairs.
     * @return rm = (1 + ε_r) * log(n) + λ, with with rm % Byte.SIZE == 0.
     */
    static int getRm(int n) {
        // when n is very small, we have very high collision probabilities. Wo do the test and find that
        // n = 2^8: 186, n = 2^9: 328, n = 2^10: 561, n = 2^11: 907. When n > 2^12, use formula to compute rm.
        int r = CommonUtils.getByteLength(
            (int) Math.ceil(RIGHT_EPSILON * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
        if (n <= (1 << 8)) {
            return CommonUtils.getByteLength(Math.max(r, n)) * Byte.SIZE;
        } else if (n <= (1 << 9)) {
            // 256 < n <= 512
            return CommonUtils.getByteLength(Math.min(n, 328)) * Byte.SIZE;
        } else if (n <= (1 << 10)) {
            // 512 < n <= 1024
            return CommonUtils.getByteLength(Math.min(n, 561)) * Byte.SIZE;
        } else if (n <= (1 << 11)) {
            // 1024 < n <= 2048
            return CommonUtils.getByteLength(907) * Byte.SIZE;
        } else {
            // n > 2048
            return r;
        }
    }

    /**
     * left m, i.e., sparse part. lm = (1 + ε_l) * n, with lm % Byte.SIZE == 0.
     */
    private final int lm;
    /**
     * right m, i.e., dense part. rm = 0.5 * log(n) + λ, with rm % Byte.SIZE == 0.
     */
    private final int rm;
    /**
     * H1: {0, 1}^* -> [0, lm)
     */
    private final Prf h1;
    /**
     * H2: {0, 1}^* -> [0, lm)
     */
    private final Prf h2;
    /**
     * H3: {0, 1}^* -> [0, lm)
     */
    private final Prf h3;
    /**
     * Hr: {0, 1}^* -> {0, 1}^rm
     */
    private final Prf hr;
    /**
     * two core finder
     */
    private final CuckooTableSingletonTcFinder<T> singletonTcFinder;
    /**
     * binary linear solver
     */
    private final BinaryLinearSolver linearSolver;
    /**
     * max linear independent finder
     */
    private final BinaryMaxLisFinder maxLisFinder;
    /**
     * key -> h1
     */
    private TObjectIntMap<T> dataH1Map;
    /**
     * key -> h2
     */
    private TObjectIntMap<T> dataH2Map;
    /**
     * key -> h3
     */
    private TObjectIntMap<T> dataH3Map;
    /**
     * key -> hr
     */
    private Map<T, boolean[]> dataHrMap;

    H3GctGfe2Dokvs(EnvType envType, int l, int n, byte[][] keys) {
        this(envType, l, n, keys, new SecureRandom());
    }

    H3GctGfe2Dokvs(EnvType envType, int l, int n, byte[][] keys, SecureRandom secureRandom) {
        super(l, n, getLm(n) + getRm(n));
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, TOTAL_HASH_NUM);
        lm = getLm(n);
        rm = getRm(n);
        h1 = PrfFactory.createInstance(envType, Integer.BYTES);
        h1.setKey(keys[0]);
        h2 = PrfFactory.createInstance(envType, Integer.BYTES);
        h2.setKey(keys[1]);
        h3 = PrfFactory.createInstance(envType, Integer.BYTES);
        h3.setKey(keys[2]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[3]);
        singletonTcFinder = new CuckooTableSingletonTcFinder<>();
        linearSolver = new BinaryLinearSolver(l, secureRandom);
        maxLisFinder = new BinaryMaxLisFinder();
    }

    @Override
    public int sparsePositionRange() {
        return lm;
    }

    @Override
    public int[] sparsePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] sparsePositions = new int[SPARSE_HASH_NUM];
        // h1
        sparsePositions[0] = h1.getInteger(0, keyBytes, lm);
        // h2 != h1
        int h2Index = 0;
        do {
            sparsePositions[1] = h2.getInteger(h2Index, keyBytes, lm);
            h2Index++;
        } while (sparsePositions[1] == sparsePositions[0]);
        // h3 != h1 and h3 != h2
        int h3Index = 0;
        do {
            sparsePositions[2] = h3.getInteger(h3Index, keyBytes, lm);
            h3Index++;
        } while (sparsePositions[2] == sparsePositions[0] || sparsePositions[2] == sparsePositions[1]);

        return sparsePositions;
    }

    @Override
    public int sparsePositionNum() {
        return SPARSE_HASH_NUM;
    }

    @Override
    public boolean[] binaryDensePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        return BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
    }

    @Override
    public int maxDensePositionNum() {
        return rm;
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        int[] sparsePositions = sparsePositions(key);
        boolean[] densePositions = binaryDensePositions(key);
        byte[] value = new byte[byteL];
        // h1, h2 and h3 must be distinct
        BytesUtils.xori(value, storage[sparsePositions[0]]);
        BytesUtils.xori(value, storage[sparsePositions[1]]);
        BytesUtils.xori(value, storage[sparsePositions[2]]);
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (densePositions[rmIndex]) {
                BytesUtils.xori(value, storage[lm + rmIndex]);
            }
        }
        assert BytesUtils.isFixedReduceByteArray(value, byteL, l);
        return value;
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.H3_SINGLETON_GCT;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        keyValueMap.values().forEach(x -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(x, byteL, l)));
        // construct maps
        Set<T> keySet = keyValueMap.keySet();
        int keySize = keySet.size();
        dataH1Map = new TObjectIntHashMap<>(keySize);
        dataH2Map = new TObjectIntHashMap<>(keySize);
        dataH3Map = new TObjectIntHashMap<>(keySize);
        dataHrMap = new HashMap<>(keySize);
        for (T key : keySet) {
            int[] sparsePositions = sparsePositions(key);
            boolean[] densePositions = binaryDensePositions(key);
            dataH1Map.put(key, sparsePositions[0]);
            dataH2Map.put(key, sparsePositions[1]);
            dataH3Map.put(key, sparsePositions[2]);
            dataHrMap.put(key, densePositions);
        }
        // generate cuckoo table with 3 hash functions
        H3CuckooTable<T> h3CuckooTable = generateCuckooTable(keyValueMap);
        // find two-core graph
        singletonTcFinder.findTwoCore(h3CuckooTable);
        // construct matrix based on two-core graph
        Set<T> coreDataSet = singletonTcFinder.getRemainedDataSet();
        // generate storage that contains all solutions in the right part and involved left part.
        byte[][] storage;
        if (doublyEncode) {
            storage = generateDoublyStorage(keyValueMap, coreDataSet);
        } else {
            TIntSet coreVertexSet = new TIntHashSet(keySet.size());
            coreDataSet.stream().map(h3CuckooTable::getVertices).forEach(coreVertexSet::addAll);
            storage = generateFreeStorage(keyValueMap, coreVertexSet, coreDataSet);
        }
        // split D = L || R
        byte[][] leftStorage = new byte[lm][];
        byte[][] rightStorage = new byte[rm][];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // back-fill
        Stack<T> removedDataStack = singletonTcFinder.getRemovedDataStack();
        Stack<int[]> removedDataVerticesStack = singletonTcFinder.getRemovedDataVertices();
        while (!removedDataStack.empty()) {
            T removedData = removedDataStack.pop();
            int[] removedDataVertices = removedDataVerticesStack.pop();
            int vertex0 = removedDataVertices[0];
            int vertex1 = removedDataVertices[1];
            int vertex2 = removedDataVertices[2];
            boolean[] rx = dataHrMap.get(removedData);
            byte[] innerProduct = BytesUtils.innerProduct(rightStorage, byteL, rx);
            byte[] value = keyValueMap.get(removedData);
            BytesUtils.xori(innerProduct, value);
            fullDistinctVertices(leftStorage, innerProduct, vertex0, vertex1, vertex2, removedData);
        }
        // fill randomness in the left part
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                if (doublyEncode) {
                    leftStorage[vertex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                } else {
                    leftStorage[vertex] = new byte[byteL];
                }
            }
        }
        // update storage
        System.arraycopy(leftStorage, 0, storage, 0, leftStorage.length);
        return storage;
    }

    private H3CuckooTable<T> generateCuckooTable(Map<T, byte[]> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        H3CuckooTable<T> h3CuckooTable = new H3CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            int h3Value = dataH3Map.get(key);
            h3CuckooTable.addData(new int[]{h1Value, h2Value, h3Value}, key);
        }
        return h3CuckooTable;
    }

    private void fullDistinctVertices(byte[][] leftMatrix, byte[] innerProduct,
                                      int vertex0, int vertex1, int vertex2, T removedData) {
        if (leftMatrix[vertex0] == null) {
            leftMatrix[vertex1] = (leftMatrix[vertex1] == null) ?
                BytesUtils.randomByteArray(byteL, l, secureRandom) : leftMatrix[vertex1];
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = (leftMatrix[vertex2] == null) ?
                BytesUtils.randomByteArray(byteL, l, secureRandom) : leftMatrix[vertex2];
            BytesUtils.xori(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex0] = innerProduct;
        } else if (leftMatrix[vertex1] == null) {
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            leftMatrix[vertex2] = (leftMatrix[vertex2] == null) ?
                BytesUtils.randomByteArray(byteL, l, secureRandom) : leftMatrix[vertex2];
            BytesUtils.xori(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex1] = innerProduct;
        } else if (leftMatrix[vertex2] == null) {
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else {
            throw new IllegalStateException(
                removedData + ":(" + vertex0 + ", " + vertex1 + ", " + vertex2 + ") are all full, error"
            );
        }
    }

    private byte[][] generateDoublyStorage(Map<T, byte[]> keyValueMap, Set<T> coreDataSet) {
        byte[][] storage = new byte[m][];
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        if (dTilde == 0) {
            // d˜ = 0, we do not need to solve equations, fill random variables.
            IntStream.range(lm, lm + rm).forEach(index ->
                storage[index] = BytesUtils.randomByteArray(byteL, l, secureRandom)
            );
            return storage;
        }
        if (dTilde > rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + "，d + λ + " + rm + " no solutions");
        }
        // Let M˜' ∈ {0, 1}^{d˜ × (d + λ)} be the sub-matrix of M˜ obtained by taking the row indexed by R.
        int dByteTilde = CommonUtils.getByteLength(dTilde);
        int dOffsetTilde = dByteTilde * Byte.SIZE - dTilde;
        byte[][] tildePrimeMatrix = new byte[rm][dByteTilde];
        int tildePrimeMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                BinaryUtils.setBoolean(tildePrimeMatrix[rmIndex], dOffsetTilde + tildePrimeMatrixRowIndex, rxBinary[rmIndex]);
            }
            tildePrimeMatrixRowIndex++;
        }
        // let M˜* be the sub-matrix of M˜ containing an invertible d˜ × d˜ matrix,
        // and C ⊂ [d + λ] index the corresponding columns of M˜.
        TIntSet setC = maxLisFinder.getLisColumns(tildePrimeMatrix, dTilde);
        int[] cArray = setC.toArray();
        int size = setC.size();
        int byteSize = CommonUtils.getByteLength(size);
        int offsetSize = byteSize * Byte.SIZE - size;
        byte[][] tildeStarMatrix = new byte[dTilde][byteSize];
        int tildeStarMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            int rmIndex = 0;
            for (int r : cArray) {
                BinaryUtils.setBoolean(tildeStarMatrix[tildeStarMatrixRowIndex], offsetSize + rmIndex, rxBinary[r]);
                rmIndex++;
            }
            tildeStarMatrixRowIndex++;
        }
        // Let C' = {j | i \in R, M'_{i, j} = 1} ∪ ([d + λ] \ C + m')
        TIntSet setPrimeC = new TIntHashSet(dTilde * 2 + rm / 2);
        for (T data : coreDataSet) {
            setPrimeC.add(dataH1Map.get(data));
            setPrimeC.add(dataH2Map.get(data));
            setPrimeC.add(dataH3Map.get(data));
        }
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (!setC.contains(rmIndex)) {
                setPrimeC.add(lm + rmIndex);
            }
        }
        // For i ∈ C' assign P_i ∈ G
        for (int primeIndexC : setPrimeC.toArray()) {
            storage[primeIndexC] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        // For i ∈ R, define v'_i = v_i - (MP), where P_i is assigned to be zero if unassigned.
        byte[][] vectorY = new byte[dTilde][];
        int coreRowIndex = 0;
        for (T data : coreDataSet) {
            int h1Value = dataH1Map.get(data);
            int h2Value = dataH2Map.get(data);
            int h3Value = dataH3Map.get(data);
            boolean[] rx = dataHrMap.get(data);
            byte[] mp = new byte[byteL];
            storage[h1Value] = (storage[h1Value] == null) ? new byte[byteL] : storage[h1Value];
            storage[h2Value] = (storage[h2Value] == null) ? new byte[byteL] : storage[h2Value];
            storage[h3Value] = (storage[h3Value] == null) ? new byte[byteL] : storage[h3Value];
            // h1, h2 and h3 must be distinct
            BytesUtils.xori(mp, storage[h1Value]);
            BytesUtils.xori(mp, storage[h2Value]);
            BytesUtils.xori(mp, storage[h3Value]);
            for (int rxIndex = 0; rxIndex < rx.length; rxIndex++) {
                if (rx[rxIndex]) {
                    if (storage[lm + rxIndex] == null) {
                        storage[lm + rxIndex] = new byte[byteL];
                    }
                    BytesUtils.xori(mp, storage[lm + rxIndex]);
                }
            }
            vectorY[coreRowIndex] = BytesUtils.xor(keyValueMap.get(data), mp);
            coreRowIndex++;
        }
        // Using Gaussian elimination solve the system
        // M˜* (P_{m' + C_1}, ..., P_{m' + C_{d˜})^T = (v'_{R_1}, ..., v'_{R_{d˜})^T.
        byte[][] vectorX = new byte[size][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(tildeStarMatrix, size, vectorY, vectorX);
        assert systemInfo.equals(LinearSolver.SystemInfo.Consistent);
        // update the result into the storage
        int xVectorIndex = 0;
        for (int cIndex : cArray) {
            storage[lm + cIndex] = BytesUtils.clone(vectorX[xVectorIndex]);
            xVectorIndex++;
        }
        return storage;
    }

    private byte[][] generateFreeStorage(Map<T, byte[]> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        if (dTilde > rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + "，d + λ + " + rm + " no solutions");
        }
        if (dTilde == 0) {
            byte[][] storage = new byte[m][];
            // d˜ = 0, we do not need to solve equations, fill 0 variables
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = new byte[byteL]);
            return storage;
        } else {
            // we need to solve equations
            byte[][] matrixM = new byte[dTilde][byteM];
            byte[][] vectorX = new byte[m][];
            byte[][] vectorY = new byte[dTilde][];
            int rowIndex = 0;
            for (T coreData : coreDataSet) {
                int h1Value = dataH1Map.get(coreData);
                int h2Value = dataH2Map.get(coreData);
                int h3Value = dataH3Map.get(coreData);
                boolean[] rx = dataHrMap.get(coreData);
                BinaryUtils.setBoolean(matrixM[rowIndex], h1Value, true);
                BinaryUtils.setBoolean(matrixM[rowIndex], h2Value, true);
                BinaryUtils.setBoolean(matrixM[rowIndex], h3Value, true);
                for (int columnIndex = 0; columnIndex < rm; columnIndex++) {
                    BinaryUtils.setBoolean(matrixM[rowIndex], lm + columnIndex, rx[columnIndex]);
                }
                vectorY[rowIndex] = BytesUtils.clone(keyValueMap.get(coreData));
                rowIndex++;
            }
            LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(matrixM, m, vectorY, vectorX);
            assert systemInfo.equals(LinearSolver.SystemInfo.Consistent);
            byte[][] storage = new byte[m][];
            for (int vertex : coreVertexSet.toArray()) {
                // set left part
                storage[vertex] = BytesUtils.clone(vectorX[vertex]);
            }
            // set right part
            System.arraycopy(vectorX, lm, storage, lm, rm);
            return storage;
        }
    }
}
