package edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.gf2e;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.H3CuckooTable;
import edu.alibaba.mpc4j.common.tool.utils.*;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 3哈希-两核GF(2^l)-OVDM实现。
 *
 * @author Weiran Liu
 * @date 2021/09/27
 */
class H3TcGctGf2eOvdm<T> extends AbstractGf2eOvdm<T> implements SparseGf2eOvdm<T> {
    /**
     * 3 sparse hashes
     */
    private static final int SPARSE_HASH_NUM = 3;
    /**
     * 3哈希-两核乱码布谷鸟表需要4个哈希函数：3个布谷鸟哈希的哈希函数，1个右侧哈希函数
     */
    static final int HASH_NUM = SPARSE_HASH_NUM + 1;
    /**
     * 3哈希-两核乱码布谷鸟表左侧编码放大系数
     */
    private static final double LEFT_EPSILON = 1.3;
    /**
     * 3哈希-两核乱码布谷鸟表右侧编码放大系数
     */
    private static final double RIGHT_EPSILON = 0.5;
    /**
     * 左侧编码比特长度，等于1.3 * n，向上取整为Byte.SIZE的整数倍
     */
    private final int lm;
    /**
     * 右侧编码比特长度，等于0.5 * log(n) + λ，向上取整为Byte.SIZE的整数倍
     */
    private final int rm;
    /**
     * 布谷鸟哈希的第1个哈希函数
     */
    private final Prf h1;
    /**
     * 布谷鸟哈希的第2个哈希函数
     */
    private final Prf h2;
    /**
     * 布谷鸟哈希的第3个哈希函数
     */
    private final Prf h3;
    /**
     * 用于计算右侧r(x)的哈希函数
     */
    private final Prf hr;
    /**
     * 数据到h1的映射表
     */
    private TObjectIntMap<T> dataH1Map;
    /**
     * 数据到h2的映射表
     */
    private TObjectIntMap<T> dataH2Map;
    /**
     * 数据到h3的映射表
     */
    private TObjectIntMap<T> dataH3Map;
    /**
     * 数据到hr的映射表
     */
    private Map<T, boolean[]> dataHrMap;

    H3TcGctGf2eOvdm(EnvType envType, int l, int n, byte[][] prfKeys) {
        super(l, n, getLm(n) + getRm(n));
        lm = getLm(n);
        rm = getRm(n);
        this.h1 = PrfFactory.createInstance(envType, Integer.BYTES);
        this.h1.setKey(prfKeys[0]);
        this.h2 = PrfFactory.createInstance(envType, Integer.BYTES);
        this.h2.setKey(prfKeys[1]);
        this.h3 = PrfFactory.createInstance(envType, Integer.BYTES);
        this.h3.setKey(prfKeys[2]);
        this.hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        this.hr.setKey(prfKeys[3]);
    }

    @Override
    public int[] sparsePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] sparsePositions = new int[SPARSE_HASH_NUM];
        sparsePositions[0] = h1.getInteger(0, keyBytes, lm);
        // h1 and h2 are distinct
        int h2Index = 0;
        do {
            sparsePositions[1] = h2.getInteger(h2Index, keyBytes, lm);
            h2Index++;
        } while (sparsePositions[1] == sparsePositions[0]);
        // h3 and h1 / h2 are distinct
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
    public boolean[] densePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        return BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
    }

    @Override
    public int maxDensePositionNum() {
        return rm;
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        assert storage.length == getM();
        int[] sparsePositions = sparsePositions(key);
        boolean[] densePositions = densePositions(key);
        byte[] value = new byte[byteL];
        // 三个哈希结果一定不同，计算3次求和
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
    public Gf2eOvdmFactory.Gf2eOvdmType getGf2xOvdmType() {
        return Gf2eOvdmFactory.Gf2eOvdmType.H3_SINGLETON_GCT;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap) throws ArithmeticException {
        assert keyValueMap.size() <= n : "insert value num = " + keyValueMap.size() + ", exceeds n = " + n;
        keyValueMap.values().forEach(x -> {
            assert BytesUtils.isFixedReduceByteArray(x, byteL, l);
        });
        // 构造数据到哈希值的查找表
        Set<T> keySet = keyValueMap.keySet();
        dataH1Map = new TObjectIntHashMap<>(keySet.size());
        dataH2Map = new TObjectIntHashMap<>(keySet.size());
        dataH3Map = new TObjectIntHashMap<>(keySet.size());
        dataHrMap = new HashMap<>(keySet.size());
        for (T key : keySet) {
            int[] sparsePositions = sparsePositions(key);
            boolean[] densePositions = densePositions(key);
            dataH1Map.put(key, sparsePositions[0]);
            dataH2Map.put(key, sparsePositions[1]);
            dataH3Map.put(key, sparsePositions[2]);
            dataHrMap.put(key, densePositions);
        }
        // 生成3哈希-布谷鸟图
        H3CuckooTable<T> h3CuckooTable = generateCuckooTable(keyValueMap);
        // 找到2-core图
        CuckooTableSingletonTcFinder<T> singletonFinder = new CuckooTableSingletonTcFinder<>();
        singletonFinder.findTwoCore(h3CuckooTable);
        // 根据2-core图的所有数据和所有边构造矩阵
        Set<T> coreDataSet = singletonFinder.getRemainedDataSet();
        // 生成矩阵，矩阵中包含右侧的全部解，以及2-core中的全部解
        byte[][] storage = generateStorage(keyValueMap, coreDataSet);
        // 将矩阵拆分为L || R
        byte[][] leftStorage = new byte[lm][];
        byte[][] rightStorage = new byte[rm][];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // 从栈中依次弹出数据，为相应节点赋值
        Stack<T> removedDataStack = singletonFinder.getRemovedDataStack();
        Stack<int[]> removedDataVerticesStack = singletonFinder.getRemovedDataVertices();
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
            // 三个顶点一定不相同
            fullDistinctVertices(leftStorage, innerProduct, vertex0, vertex1, vertex2, removedData);
        }
        // 左侧矩阵补充随机数
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                leftStorage[vertex] = new byte[byteL];
            }
        }
        // 更新矩阵
        System.arraycopy(leftStorage, 0, storage, 0, leftStorage.length);
        // 不应该再有没有更新的矩阵行了
        for (byte[] bytes : storage) {
            assert bytes != null;
        }

        return storage;
    }

    private void fullDistinctVertices(byte[][] leftMatrix, byte[] innerProduct,
                                      int vertex0, int vertex1, int vertex2, T data) {
        if (leftMatrix[vertex0] == null && leftMatrix[vertex1] == null && leftMatrix[vertex2] == null) {
            // 0、1、2都为空
            leftMatrix[vertex0] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            leftMatrix[vertex1] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else if (leftMatrix[vertex0] == null && leftMatrix[vertex1] == null) {
            // 0、1为空
            leftMatrix[vertex0] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex1] = innerProduct;
        } else if (leftMatrix[vertex0] == null && leftMatrix[vertex2] == null) {
            // 0、2为空
            leftMatrix[vertex0] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else if (leftMatrix[vertex1] == null && leftMatrix[vertex2] == null) {
            // 1、2为空
            leftMatrix[vertex1] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else if (leftMatrix[vertex0] == null) {
            // 0为空
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex0] = innerProduct;
        } else if (leftMatrix[vertex1] == null) {
            // 1为空
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex1] = innerProduct;
        } else if (leftMatrix[vertex2] == null) {
            // 2为空
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else {
            // 三个都不为空，不可能出现这种情况
            throw new IllegalStateException(data + "的顶点(" + vertex0 + ", " + vertex1 + ", " + vertex2 + ")均不为空");
        }
    }

    private byte[][] generateStorage(Map<T, byte[]> keyValueMap, Set<T> coreDataSet) {
        // 初始化OVDM存储器，所有位置设置为空
        byte[][] storage = new byte[m][];
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        // 如果没有2-core边，则补充的边都设置为随机数
        if (dTilde == 0) {
            IntStream.range(lm, lm + rm).forEach(index ->
                storage[index] = BytesUtils.randomByteArray(byteL, l, secureRandom)
            );
            return storage;
        }
        if (dTilde > rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + "，d + λ + " + rm + "，线性系统无解");
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
        TIntSet setC = maxLisFinder.getLisRows(tildePrimeMatrix, dTilde);
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
            byte[] mp = new byte[this.byteL];
            if (storage[h1Value] == null) {
                storage[h1Value] = new byte[byteL];
            }
            if (storage[h2Value] == null) {
                storage[h2Value] = new byte[byteL];
            }
            if (storage[h3Value] == null) {
                storage[h3Value] = new byte[byteL];
            }
            // 3个哈希函数一定互不相同
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
        SystemInfo systemInfo = linearSolver.freeSolve(tildeStarMatrix, size, vectorY, vectorX);
        if (systemInfo.compareTo(SystemInfo.Inconsistent) == 0) {
            throw new ArithmeticException("无法完成编码过程，线性系统无解");
        }
        // 将求解结果更新到matrix里面
        int xVectorIndex = 0;
        for (int cIndex : cArray) {
            storage[this.lm + cIndex] = BytesUtils.clone(vectorX[xVectorIndex]);
            xVectorIndex++;
        }
        return storage;
    }

    /**
     * 给定待编码的键值对个数，计算左侧映射比特长度。
     *
     * @param n 待编码的键值对个数。
     * @return 左侧哈希比特长度，向上取整为Byte.SIZE的整数倍。
     */
    static int getLm(int n) {
        // 根据论文第5.4节，lm = 1.3 * n，向上取整到Byte.SIZE的整数倍
        return CommonUtils.getByteLength((int) Math.ceil(LEFT_EPSILON * n)) * Byte.SIZE;
    }

    /**
     * 给定待编码的键值对个数，计算右侧映射比特长度。
     *
     * @param n 待编码的键值对个数。
     * @return 右侧映射比特长度。向上取整为Byte.SIZE的整数倍。
     */
    static int getRm(int n) {
        // 根据论文第5.4节，r = 0.5 * log(n) + λ，向上取整到Byte.SIZE的整数倍
        // 但当n比较小时，哈希碰撞概率较高，会导致2-core图对应边数量很多
        // 测试结果为：2^8: 186，2^9: 328, 2^10: 561, 2^11: 907，当达到2^12时，骤降为4
        int r = CommonUtils.getByteLength(
            (int) Math.ceil(RIGHT_EPSILON * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
        if (n <= 1 << 8) {
            // 小于2^8，取0.5 * log(n) + λ和n的最大值
            return CommonUtils.getByteLength(Math.max(r, n)) * Byte.SIZE;
        } else if (n <= 1 << 9) {
            // 256 < n <= 512
            return CommonUtils.getByteLength(Math.min(n, 328)) * Byte.SIZE;
        } else if (n <= 1 << 10) {
            // 512 < n <= 1024
            return CommonUtils.getByteLength(Math.min(n, 561)) * Byte.SIZE;
        } else if (n <= 1 << 11) {
            // 1024 < n <= 2048
            return CommonUtils.getByteLength(907) * Byte.SIZE;
        } else {
            // n > 2048
            return r;
        }
    }

    /**
     * 生成3哈希-布谷鸟图。
     *
     * @param keyValueMap 键值对映射。
     * @return 3哈希-布谷鸟图。
     */
    H3CuckooTable<T> generateCuckooTable(Map<T, byte[]> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        // 构造3哈希-布谷鸟图
        H3CuckooTable<T> h3CuckooTable = new H3CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            int h3Value = dataH3Map.get(key);
            h3CuckooTable.addData(new int[]{h1Value, h2Value, h3Value}, key);
        }
        return h3CuckooTable;
    }

    @Override
    public int getNegLogFailureProbability() {
        // 根据论文第5.4节，r = 0.5 * log(n) + λ，失败概率为2^(-29.355)
        return 29;
    }
}
