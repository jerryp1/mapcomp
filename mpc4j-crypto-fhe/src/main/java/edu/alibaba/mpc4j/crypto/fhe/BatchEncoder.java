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

import java.util.Arrays;

/**
 * Provides functionality for CRT batching. If the polynomial modulus degree is N, and
 * the plaintext modulus is a prime number T such that T is congruent to 1 modulo 2N,
 * then BatchEncoder allows the plaintext elements to be viewed as 2-by-(N/2)
 * matrices of integers modulo T. Homomorphic operations performed on such encrypted
 * matrices are applied coefficient (slot) wise, enabling powerful SIMD functionality
 * for computations that are vectorizable. This functionality is often called "batching"
 * in the homomorphic encryption literature.
 * <p>
 * Mathematically speaking, if the polynomial modulus is X^N+1, N is a power of two, and
 * plain_modulus is a prime number T such that 2N divides T-1, then integers modulo T
 * contain a primitive 2N-th root of unity and the polynomial X^N+1 splits into n distinct
 * linear factors as X^N+1 = (X-a_1)*...*(X-a_N) mod T, where the constants a_1, ..., a_n
 * are all the distinct primitive 2N-th roots of unity in integers modulo T. The Chinese
 * Remainder Theorem (CRT) states that the plaintext space Z_T[X]/(X^N+1) in this case is
 * isomorphic (as an algebra) to the N-fold direct product of fields Z_T. The isomorphism
 * is easy to compute explicitly in both directions, which is what this class does.
 * Furthermore, the Galois group of the extension is (Z/2NZ)* ~= Z/2Z x Z/(N/2) whose
 * action on the primitive roots of unity is easy to describe. Since the batching slots
 * correspond 1-to-1 to the primitive roots of unity, applying Galois automorphisms on the
 * plaintext act by permuting the slots. By applying generators of the two cyclic
 * subgroups of the Galois group, we can effectively view the plaintext as a 2-by-(N/2)
 * matrix, and enable cyclic row rotations, and column rotations (row swaps).
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/batchencoder.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/10/5
 */
public class BatchEncoder {

    /**
     * context
     */
    private final Context context;
    /**
     * slot num
     */
    private final int slots;
    /**
     * roots of unity
     */
    private final long[] rootsOfUnity;
    /**
     * matrix reverse position index map
     */
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
        // Reserve space for all the primitive roots
        rootsOfUnity = new long[slots];
        // Fill the vector with roots of unity with all distinct odd powers of generator.
        // These are all the primitive (2*slots_)-th roots of unity in integers modulo
        // parms.plain_modulus().
        populateRootsOfUnityVector(contextData);
        populateMatrixRepsIndexMap();
    }

    /**
     * encode int64 array into plaintext.
     * 注意后缀 I64 表示 将输入long 视为 int64 来处理，即有符号数，默认的 encode 是对于 uint64 的处理
     *
     * @param valuesMatrix int64 array.
     * @param destination  the plaintext to overwrite.
     */
    public void encodeInt64(long[] valuesMatrix, Plaintext destination) {
        Context.ContextData contextData = context.firstContextData();
        int valuesMatrixSize = valuesMatrix.length;
        if (valuesMatrixSize > slots) {
            throw new IllegalArgumentException("values_matrix size is too large");
        }
        long modulus = contextData.getParms().getPlainModulus().getValue();
        long plainModulusDivTwo = modulus >> 1;
        // debug assert, 输入值应该 [0, t)
        for (long matrix : valuesMatrix) {
            // value <= t/2
            assert !Common.unsignedGt(Math.abs(matrix), plainModulusDivTwo);
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
        // represent plaintext in coefficient values.
        NttTool.inverseNttNegacyclicHarvey(destination.getData(), contextData.getPlainNttTables());
    }

    /**
     * encode uint64 array into plaintext.
     *
     * @param valuesMatrix uint64 array to encode.
     * @param destination  the plaintext to overwrite.
     */
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
        // represent plaintext in coefficient values.
        NttTool.inverseNttNegacyclicHarvey(destination.getData(), contextData.getPlainNttTables());
    }

    /**
     * decode plaintext to int64 array.
     *
     * @param plain       plaintext to decode.
     * @param destination the int64 array to overwrite.
     */
    public void decodeInt64(Plaintext plain, long[] destination) {
        // todo: 考虑通过添加 boolean unsigned 参数，来统一 有符号数和无符号数的调用接口？
        // todo: 但是这样的话 用户调用时需要多传一个参数，需要讨论决定
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
        NttTool.nttNegacyclicHarvey(tempDest, contextData.getPlainNttTables());
        // 再把值重构到 正确的顺序(normal order)上
        long plainModulusDivTwo = modulus >> 1;
        long curValue;
        for (int i = 0; i < slots; i++) {
            curValue = tempDest[matrixReversePositionIndexMap[i]];
            destination[i] = (Long.compareUnsigned(curValue, plainModulusDivTwo) > 0) ? (curValue - modulus) : curValue;
        }
    }

    /**
     * decode plaintext to uint64 array.
     *
     * @param plain       plaintext to decode.
     * @param destination the uint64 array to overwrite.
     */
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
        NttTool.nttNegacyclicHarvey(tempDest, contextData.getPlainNttTables());
        // 再把值重构到 正确的顺序(normal order)上
        for (int i = 0; i < slots; i++) {
            destination[i] = tempDest[matrixReversePositionIndexMap[i]];
        }
    }

    /**
     * compute g g^3 g^5 .... g^{2n - 1}, where g is the 2n-th primitive root of unity mod t.
     *
     * @param contextData context data.
     */
    private void populateRootsOfUnityVector(Context.ContextData contextData) {
        // 2n-th primitive root of unity mod t
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

    /**
     * store the bit-reversed locations, isomorphic to Z_{n/2} * Z_2.
     */
    private void populateMatrixRepsIndexMap() {
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

    /**
     * return slot num.
     *
     * @return slot num.
     */
    public int slotCount() {
        return slots;
    }
}