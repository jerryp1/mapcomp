package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.Plaintext;
import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.zq.MultiplyUintModOperand;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

import java.util.Arrays;

/**
 * This class provides some scaling methods.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/scalingvariant.cpp
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/26
 */
public class ScalingVariant {

    /**
     * 这个方法处理的是一个 RnsIter，输入是一个 PolyIter + startIndex = RnsIter
     *
     * @param plain
     * @param contextData
     * @param destinationPolyIter
     * @param startIndex
     */
    public static void multiplySubPlainWithScalingVariant(Plaintext plain, Context.ContextData contextData, long[] destinationPolyIter, int destinationCoeffCount, int startIndex) {

        EncryptionParams parms = contextData.getParms();
        int plainCoeffCount = plain.getCoeffCount();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        Modulus plainModulus = contextData.getParms().getPlainModulus();
        MultiplyUintModOperand[] coeffDivPlainModulus = contextData.getCoeffDivPlainModulus();
        long plianUpperHalfThreshold = contextData.getPlainUpperHalfThreshold();
        long qModT = contextData.getCoeffModulusModPlainModulus();

        // todo: maybe < ?
        assert plainCoeffCount <= parms.getPolyModulusDegree();
        assert destinationCoeffCount == parms.getPolyModulusDegree();

        // Coefficients of plain m multiplied by coeff_modulus q, divided by plain_modulus t,
        // and rounded to the nearest integer (rounded up in case of a tie). Equivalent to
        // floor((q * m + floor((t+1) / 2)) / t).

        // 按列遍历，k * N , 每一列对应同一个 coeff 在不同的 RnsBase 下 的值
        long[] prod = new long[2]; // 避免多次 分配内存，写在循环外面
        long[] numerator = new long[2];
        long[] fix = new long[2];
        // todo: 尝试并行化提速
        for (int i = 0; i < plainCoeffCount; i++) {

            Arrays.fill(prod, 0);
            Arrays.fill(numerator, 0);
            // Compute numerator = (q mod t) * m[i] + (t+1)/2
            UintArithmetic.multiplyUint64(plain.getData()[i], qModT, prod);
            // carry is 0 or 1, 低位 + half
            long carry = UintArithmetic.addUint64(prod[0], plianUpperHalfThreshold, numerator);
            // 处理高位
            numerator[1] = prod[1] + carry;

            // Compute fix[0] = floor(numerator / t)
            Arrays.fill(fix, 0);
            UintArithmetic.divideUint128Inplace(numerator, plainModulus.getValue(), fix);

            // Add to ciphertext: floor(q / t) * m + increment
//            int coeffIndex = i;
            for (int j = 0; j < coeffModulusSize; j++) {

                long scaledRoundedHalf = UintArithmeticSmallMod.multiplyAddUintMod(
                        plain.getData()[i],
                        coeffDivPlainModulus[j],
                        fix[0],
                        coeffModulus[j]
                );
                // 和 multiplyAddPlainWithScalingVariant 唯一的区别就是这里调用 subUintMod
                destinationPolyIter[startIndex + j * destinationCoeffCount + i] = UintArithmeticSmallMod.subUintMod(
                        destinationPolyIter[startIndex + j * destinationCoeffCount + i],
                        scaledRoundedHalf,
                        coeffModulus[j]
                );
            }
        }
    }


    /**
     * compute round(plain * q/t) + poly.
     * 这个方法处理的是一个 RnsIter，输入是一个 PolyIter + startIndex = RnsIter
     *
     * @param plain                 plaintext.
     * @param contextData           context data.
     * @param destinationPolyIter   the poly to overwrite.
     * @param destinationCoeffCount coeff count.
     * @param startIndex            start index.
     */
    public static void multiplyAddPlainWithScalingVariant(Plaintext plain, Context.ContextData contextData,
                                                          long[] destinationPolyIter, int destinationCoeffCount,
                                                          int startIndex) {
        EncryptionParams parms = contextData.getParms();
        int plainCoeffCount = plain.getCoeffCount();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        Modulus plainModulus = contextData.getParms().getPlainModulus();
        // q / t mod q_i
        MultiplyUintModOperand[] coeffDivPlainModulus = contextData.getCoeffDivPlainModulus();
        // (plain_modulus + 1) / 2
        long plainUpperHalfThreshold = contextData.getPlainUpperHalfThreshold();
        // q mod t
        long qModT = contextData.getCoeffModulusModPlainModulus();
        assert plainCoeffCount <= parms.getPolyModulusDegree();
        assert destinationCoeffCount == parms.getPolyModulusDegree();
        // Coefficients of plain m multiplied by coeff_modulus q, divided by plain_modulus t,
        // and rounded to the nearest integer (rounded up in case of a tie). Equivalent to
        // floor((q * m + floor((t+1) / 2)) / t).
        long[] prod = new long[2];
        long[] numerator = new long[2];
        long[] fix = new long[2];
        // todo: 尝试并行化提速
        for (int i = 0; i < plainCoeffCount; i++) {
            Arrays.fill(prod, 0);
            Arrays.fill(numerator, 0);
            // (q mod t) * m[i]
            UintArithmetic.multiplyUint64(plain.getData()[i], qModT, prod);
            // 低位 (q mod t) * m[i] + (t+1)/2
            long carry = UintArithmetic.addUint64(prod[0], plainUpperHalfThreshold, numerator);
            // 高位 carry is 0 or 1
            numerator[1] = prod[1] + carry;
            // Compute fix[0] = floor(numerator / t)
            Arrays.fill(fix, 0);
            UintArithmetic.divideUint128Inplace(numerator, plainModulus.getValue(), fix);
            // Add to ciphertext: floor(q / t) * m + increment
            for (int j = 0; j < coeffModulusSize; j++) {
                long scaledRoundedHalf = UintArithmeticSmallMod.multiplyAddUintMod(
                    plain.getData()[i], coeffDivPlainModulus[j], fix[0], coeffModulus[j]);
                // destinationPolyIter 的 coeffCount 和 plainCoeffCount 完全是两回事
                // 所以当然不能用 plainCoeffCount 作为步长来给 destinationPolyIter 赋值！
                destinationPolyIter[startIndex + j * destinationCoeffCount + i] = UintArithmeticSmallMod.addUintMod(
                    destinationPolyIter[startIndex + j * destinationCoeffCount + i],
                    scaledRoundedHalf,
                    coeffModulus[j]
                );
            }
        }
    }
}