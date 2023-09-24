package edu.alibaba.mpc4j.crypto.fhe.utils;


import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.bouncycastle.math.raw.Mod;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/9/11
 */
public class GaloisTool {

    private int coeffCountPower = 0;

    private int coeffCount = 0;

    private long generator = 3;

    private int[][] permutationTables;


    public GaloisTool(int coeffCountPower) {
        initialize(coeffCountPower);
    }

    public void generateTableNtt(long galoisElt, int[] result) {

        assert (galoisElt & 1) != 0;
        assert (galoisElt < 2 * (1L << coeffCountPower));

        int[] temp = new int[coeffCount];
        int tempIndex = 0;
        int coeffCountMinusOne = coeffCount - 1;

        for (int i = coeffCount; i < (coeffCount << 1); i++) {

            int reversed = Common.reverseBits(i, coeffCountPower + 1);
            long indexRaw = (galoisElt * reversed) >>> 1;
            indexRaw &= (long) coeffCountMinusOne;
            temp[tempIndex++] = Common.reverseBits((int) indexRaw, coeffCountPower);
        }
        // copy to result
        System.arraycopy(temp, 0, result, 0, coeffCount);
    }


    private void initialize(int coeffCountPower) {

        if (coeffCountPower < UintCore.getPowerOfTwo(Constants.POLY_MOD_DEGREE_MIN) ||
                coeffCountPower > UintCore.getPowerOfTwo(Constants.POLY_MOD_DEGREE_MAX)
        ) {
            throw new IllegalArgumentException("coeffCountPower out of range");
        }

        this.coeffCountPower = coeffCountPower;
        coeffCount = 1 << coeffCountPower;
        // 注意行数和列数的确定
        permutationTables = new int[coeffCount][coeffCount];
    }

    /**
     * Compute the Galois element corresponding to a given rotation step.
     *
     * @param step
     * @return
     */
    int getEltFromStep(int step) {

        int n = coeffCount;
        int m32 = Common.mulSafe(n, 2, true);
        long m = (long) m32;

        if (step == 0) {
            return (int) m - 1;
        } else {

            // Extract sign of steps.
            // When steps is positive, the rotation is to the left; when steps is negative, it is to the right.
            boolean sign = step < 0;
            int posStep = Math.abs(step);

            if (posStep >= (n >>> 1)) {
                throw new IllegalArgumentException("step count too large");
            }

            posStep &= (m32 - 1);

            if (sign) {
                step = (n >>> 1) - posStep;
            } else {
                step = posStep;
            }
            // Construct Galois element for row rotation
            long gen = (long) generator;
            long galoisElt = 1;
            while (step-- > 0) {
                galoisElt *= gen;
                galoisElt &= (m - 1);
            }
            return (int) galoisElt;
        }
    }

    /**
     * Compute the Galois elements corresponding to a vector of given rotation steps.
     *
     * @param steps
     * @return
     */
    public int[] getEltsFromSteps(int[] steps) {
        return Arrays.stream(steps).parallel().map(this::getEltFromStep).toArray();
    }

    /**
     * Compute a vector of all necessary galois_elts.
     *
     * @return
     */
    public int[] getEltsAll() {

        int m = (int) ((long) coeffCount << 1);
        int[] galoisElts = new int[2 * (coeffCountPower - 1) + 1];
        int i = 0;

        // Generate Galois keys for m - 1 (X -> X^{m-1})
        galoisElts[i++] = m - 1;

        // Generate Galois key for power of generator_ mod m (X -> X^{3^k}) and
        // for negative power of generator_ mod m (X -> X^{-3^k})
        long posPower = generator;
        long[] temp = new long[1];
        UintArithmeticSmallMod.tryInvertUintMod(generator, (long) m, temp);
        long negPower = temp[0];

        for (int j = 0; j < coeffCountPower - 1; j++) {
            galoisElts[i++] = (int) posPower;
            posPower *= posPower;
            posPower &= (m - 1);

            galoisElts[i++] = (int) negPower;
            negPower *= negPower;
            negPower &= (m - 1);
        }

        return galoisElts;
    }

    public void applyGalois(long[] operand, int galoisElt, Modulus modulus, long[] result) {

        assert operand != result;
        // Verify coprime conditions.
        assert (galoisElt & 1) > 0 && (galoisElt < 2 * (1L << coeffCountPower));
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long coeffCountMinusOne = coeffCount - 1;
        long indexRaw = 0;

        int operandIndex = 0;

        for (int i = 0; i <= coeffCountMinusOne; i++) {

            long index = indexRaw & coeffCountMinusOne;
            long resultValue = operand[operandIndex];

            if (((indexRaw >>> coeffCountPower) & 1) > 0) {
                // Explicit inline
                // result[index] = negate_uint_mod(result[index], modulus);
                long nonZero = resultValue != 0 ? 1 : 0;
                resultValue = (modulusValue - resultValue) & (-nonZero);
            }
            result[(int) index] = resultValue;

            indexRaw += galoisElt;
            operandIndex++;
        }
    }

    public void applyGaloisNtt(long[] operand, int galoisElt, long[] result) {

        assert operand != result;
        // Verify coprime conditions.
        assert (galoisElt & 1) > 0 && (galoisElt < 2 * (1L << coeffCountPower));


        generateTableNtt(galoisElt, permutationTables[getIndexFromElt(galoisElt)]);

        int[] table = permutationTables[getIndexFromElt(galoisElt)];
        // perform permutation
        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
                    result[i] = operand[table[i]];
                }
        );
    }


    /**
     * 方法后缀是 RnsIter, 表示这个方法 处理数据的基本单位是 RnsIter, 我们用 long[] + rnsIterCoeffCount 来表示一个 RnsIter
     * 从而避免 new RnsIter 带来的额外开销.
     * 输入是 RnsIter + 方法是 RnsIter
     * 输入是 PolyIter + startIndex = RnsIter, 方法是 RnsIter
     *
     * @param rnsIter single poly in rns, length is k * N
     * @param rnsIterCoeffCount N
     * @param coeffModulusSize k
     * @param galoisElt
     * @param resultRnsIter single poly in rns
     */
    public void applyGaloisNttRnsIter(long[] rnsIter, int rnsIterCoeffCount, int coeffModulusSize, int galoisElt, long[] resultRnsIter, int resultRnsIterCoeffCount) {

        assert rnsIter != resultRnsIter;
        // Verify coprime conditions.
        assert (galoisElt & 1) > 0 && (galoisElt < 2 * (1L << coeffCountPower));

        assert coeffModulusSize > 0 && rnsIterCoeffCount == coeffCount;
        assert resultRnsIterCoeffCount == coeffCount;

        // 这里在残次循环是不变的
        generateTableNtt(galoisElt, permutationTables[getIndexFromElt(galoisElt)]);
        int[] table = permutationTables[getIndexFromElt(galoisElt)];

        // perform permutation
        // 简单操作，直接 for 循环
        for (int j = 0; j < coeffModulusSize; j++) {
            for (int i = 0; i < coeffCount; i++) {
                resultRnsIter[j * coeffCount + i] = rnsIter[table[i]];
            }
        }

    }



    public static int getIndexFromElt(int galoisElt) {

        assert (galoisElt & 1) > 0;

        return (galoisElt - 1) >>> 1;
    }

}
