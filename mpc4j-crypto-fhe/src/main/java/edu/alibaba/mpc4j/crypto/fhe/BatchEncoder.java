package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValueChecker;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/10/5
 */
public class BatchEncoder {

    // 阻止编译器警告，一般来说听编译器的
    @SuppressWarnings("FieldMayBeFinal")
    private Context context;

    private int slots;

    private long[] rootsOfUnity;

    private int[] matrixReversePositionIndexMap;


    public BatchEncoder(Context context) {

        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }

        Context.ContextData contextData = context.firstContextData();

        if (contextData.getParms().getScheme() != SchemeType.BFV && contextData.getParms().getScheme() != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }

        if (!contextData.getQualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters are not valid for batching");
        }

        this.context = context;

        slots = contextData.getParms().getPolyModulusDegree();
        // Reserve space for all of the primitive roots
        rootsOfUnity = new long[slots];
        // Fill the vector of roots of unity with all distinct odd powers of generator.
        // These are all the primitive (2*slots_)-th roots of unity in integers modulo
        // parms.plain_modulus().
        populateRootsOfUnityVector(contextData);

        //
        populateMatrixRepsIndexMap();
    }

    /**
     * 注意后缀 I64 表示 将输入long 视为 int64 来处理，即有符号数，默认的 encode 是对于 uint64 的处理
     *
     * @param valuesMatrix
     * @param destination
     */
    public void encodeInt64(long[] valuesMatrix, Plaintext destination) {

        Context.ContextData contextData = context.firstContextData();

        int valuesMatrixSize = valuesMatrix.length;
        if (valuesMatrixSize > slots) {
            throw new IllegalArgumentException("values_matrix size is too large");
        }
        // debug assert, 输入值应该 [0, t)
        long modulus = contextData.getParms().getPlainModulus().getValue();
        long plainModulusDivTwo = modulus >> 1;
        for (int i = 0; i < valuesMatrix.length; i++) {
            // value <= t/2
            assert !Common.unsignedGt(Math.abs(valuesMatrix[i]), plainModulusDivTwo);
        }

        // Set destination to full size
        destination.resize(slots);
        destination.setParmsId(ParmsIdType.parmsIdZero());

        // First write the values to destination coefficients.
        // Read in top row, then bottom row.
        long value;
        for (int i = 0; i < valuesMatrixSize; i++) {
            // 这里的实现不需要将 valuesMatrix[i] 转换为 uint64_t, 实际上在 Java里也没法转换
            // 这里直接使用，和Seal里转换后的效果是相同的, 例如 int64_t a = -1, 转换为 uint64_t 后就是 其最大值
            value = (valuesMatrix[i] < 0) ? (modulus + valuesMatrix[i]) : valuesMatrix[i];
            // index --> value
            destination.set(matrixReversePositionIndexMap[i], value);
        }
        // 剩余位置置0
        // todo: 可以取消掉这一步吗？因为 没有被赋值的位置默认都是 0
        for (int i = valuesMatrixSize; i < slots; i++) {
            // index --> value
            destination.set(matrixReversePositionIndexMap[i], 0);
        }
        // Transform destination using inverse of negacyclic NTT
        // Note: We already performed bit-reversal when reading in the matrix

        // 一次 inverseNtt, 给定的输入 可以理解为 (点，值) 表示法下的多项式，将其转换回 系数表示法
        NttTool.inverseNttNegAcyclicHarvey(destination.getData(), contextData.getPlainNttTables());
    }

    public void encode(long[] valuesMatrix, Plaintext destination) {

        Context.ContextData contextData = context.firstContextData();

        int valuesMatrixSize = valuesMatrix.length;
        if (valuesMatrixSize > slots) {
            throw new IllegalArgumentException("values_matrix size is too large");
        }
        // debug assert, 输入值应该 [0, t)
        assert Arrays.stream(valuesMatrix).allMatch(n -> n < contextData.getParms().getPlainModulus().getValue());
        // Set destination to full size
        destination.resize(slots);
        destination.setParmsId(ParmsIdType.parmsIdZero());

        // First write the values to destination coefficients.
        // Read in top row, then bottom row.
        for (int i = 0; i < valuesMatrixSize; i++) {
            // index --> value
            destination.set(matrixReversePositionIndexMap[i], valuesMatrix[i]);
        }
        // 剩余位置置0
        // todo: 可以取消掉这一步吗？因为 没有被赋值的位置默认都是 0
        for (int i = valuesMatrixSize; i < slots; i++) {
            // index --> value
            destination.set(matrixReversePositionIndexMap[i], 0);
        }
        // Transform destination using inverse of negacyclic NTT
        // Note: We already performed bit-reversal when reading in the matrix

        // 一次 inverseNtt, 给定的输入 可以理解为 (点，值) 表示法下的多项式，将其转换回 系数表示法
        NttTool.inverseNttNegAcyclicHarvey(destination.getData(), contextData.getPlainNttTables());
    }

    /**
     * 将输出的 long 视为 int64_t,即 有符号数
     * todo: 考虑通过添加 boolean unsigned 参数，来统一 有符号数和无符号数的调用接口？
     * todo: 但是这样的话 用户调用时需要多传一个参数，需要讨论决定
     *
     * @param plain
     * @param destination
     */
    public void decodeInt64(Plaintext plain, long[] destination) {

        if (!ValueChecker.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        if (plain.isNttForm()) {
            throw new IllegalArgumentException("plain cannot be in NTT form");
        }

        Context.ContextData contextData = context.firstContextData();
        long modulus = contextData.getParms().getPlainModulus().getValue();

        // Set destination
        assert destination.length == slots;

        // Never include the leading zero coefficient (if present)
        // plainCoeffCount 指的是当前这个明文真正的 coeffCount, 例如 1x^1, 那么这个plainCoeffCount是 2
        int plainCoeffCount = Math.min(plain.getCoeffCount(), slots);

        long[] tempDest = new long[slots];
        // Make a copy of poly
        System.arraycopy(plain.getData(), 0, tempDest, 0, plainCoeffCount);
        // 其余位置默认为 0, 不需要再处理一次了

        // Transform destination using negacyclic NTT.
        // 现在就是 系数表示法 转换为 点值表示法，正向的 ntt 来一次
        NttTool.nttNegAcyclicHarvey(tempDest, contextData.getPlainNttTables());

        // 再把值重构到 正确的顺序(normal order)上
        long plainModulusDivTwo = modulus >> 1;
        long curValue;
        for (int i = 0; i < slots; i++) {
            curValue = tempDest[matrixReversePositionIndexMap[i]];
            destination[i] = (Long.compareUnsigned(curValue, plainModulusDivTwo) > 0) ? (curValue - modulus) : curValue;
        }
    }

    public void decode(Plaintext plain, long[] destination) {

        if (!ValueChecker.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        if (plain.isNttForm()) {
            throw new IllegalArgumentException("plain cannot be in NTT form");
        }

        Context.ContextData contextData = context.firstContextData();

        // Set destination
        assert destination.length == slots;

        // Never include the leading zero coefficient (if present)
        // plainCoeffCount 指的是当前这个明文真正的 coeffCount, 例如 1x^1, 那么这个plainCoeffCount是 2
        int plainCoeffCount = Math.min(plain.getCoeffCount(), slots);

        long[] tempDest = new long[slots];
        // Make a copy of poly
        System.arraycopy(plain.getData(), 0, tempDest, 0, plainCoeffCount);
        // 其余位置默认为 0, 不需要再处理一次了

        // Transform destination using negacyclic NTT.
        // 现在就是 系数表示法 转换为 点值表示法，正向的 ntt 来一次
        NttTool.nttNegAcyclicHarvey(tempDest, contextData.getPlainNttTables());

        // 再把值重构到 正确的顺序(normal order)上
        for (int i = 0; i < slots; i++) {
            destination[i] = tempDest[matrixReversePositionIndexMap[i]];
        }
    }


    private void populateRootsOfUnityVector(Context.ContextData contextData) {

        long root = contextData.getPlainNttTables().getRoot();
        Modulus modulus = contextData.getParms().getPlainModulus();
        // g^2 mod t
        long generatorSq = UintArithmeticSmallMod.multiplyUintMod(root, root, modulus);
        rootsOfUnity[0] = root;
        // g g^3 g^5 .... g^{2n - 1}, just 2n-th roots of unity in integer mod t
        for (int i = 1; i < slots; i++) {
            rootsOfUnity[i] = UintArithmeticSmallMod.multiplyUintMod(rootsOfUnity[i - 1], generatorSq, modulus);
        }
    }


    private void populateMatrixRepsIndexMap() {
        // bitCount, 也就是 需要反转的 index 所在范围的最大比特数, 例如 N = 8, 那么只需要3个比特就可以表示
        // 那么位置反转也就是在 3个比特上进行的
        int logN = UintCore.getPowerOfTwo(slots);
        matrixReversePositionIndexMap = new int[slots];

        // Copy from the matrix to the value vectors
        int rowSize = slots >>> 1;
        int m = slots << 1;

        long gen = 3;
        long pos = 1;
        for (int i = 0; i < rowSize; i++) {
            // Position in normal bit order
            long index1 = (pos - 1) >> 1;
            long index2 = (m - pos - 1) >> 1;

            // Set the bit-reversed locations
            matrixReversePositionIndexMap[i] = (int) Common.reverseBits(index1, logN);
            matrixReversePositionIndexMap[rowSize | i] = (int) Common.reverseBits(index2, logN);
            // Next primitive root
            pos *= gen;
            pos &= (m - 1);
        }
    }


    private void reverseBit(long[] input) {
        assert input != null;

        int coeffCount = context.firstContextData().getParms().getPolyModulusDegree();
        int logN = UintCore.getPowerOfTwo(coeffCount);
        long temp;
        for (int i = 0; i < coeffCount; i++) {
            int reversedI = Common.reverseBits(i, logN);
            if (i < reversedI) {
                // swap
                temp = input[i];
                input[i] = input[reversedI];
                input[reversedI] = temp;
            }
        }
    }

    public int slotCount() {
        return slots;
    }
}
