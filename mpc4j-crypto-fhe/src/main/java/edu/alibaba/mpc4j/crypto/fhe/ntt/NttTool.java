package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.zq.MultiplyUintModOperand;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/8/27
 */
public class NttTool {

    public static void nttNegAcyclicHarveyLazy(long[] operand, NttTables tables) {
        tables.nttHandler.transformToRev(
                operand,
                tables.getCoeffCountPower(),
                tables.getRootPowers(),
                null
        );
    }


    public static void nttNegAcyclicHarvey(long[] operand, NttTables tables) {


        nttNegAcyclicHarveyLazy(operand, tables);

        // Finally maybe we need to reduce every coefficient modulo q, but we
        // know that they are in the range [0, 4q).
        // Since word size is controlled this is fast.
        long modulus = tables.getModulus().getValue();
        long twoTimesModulus = modulus * 2;
        int n = 1 << tables.getCoeffCountPower();

        IntStream.range(0, n).parallel().forEach(
                i -> {
                    // Note: I must be passed to the lambda by reference.
                    if (operand[i] >= twoTimesModulus) {
                        operand[i] -= twoTimesModulus;
                    }
                    if (operand[i] >= modulus) {
                        operand[i] -= modulus;
                    }
//                    if (Long.compareUnsigned(operand[i], twoTimesModulus) >= 0) {
//                        operand[i] -= twoTimesModulus;
//                    }
//                    if (Long.compareUnsigned(operand[i], modulus) >= 0) {
//                        operand[i] -= modulus;
//                    }

                }
        );
    }

    public static void inverseNttNegAcyclicHarveyLazy(long[] operand, NttTables tables) {

        MultiplyUintModOperand invDegreeModulo = tables.getInvDegreeModulo();

        tables.nttHandler.transformFromRev(
                operand,
                tables.getCoeffCountPower(),
                tables.getInvRootPowers(),
                invDegreeModulo
        );
    }

    public static void inverseNttNegAcyclicHarvey(long[] operand, NttTables tables) {

        inverseNttNegAcyclicHarveyLazy(operand, tables);

        long modulus = tables.getModulus().getValue();
        int n = 1 << tables.getCoeffCountPower();
        IntStream.range(0, n).parallel().forEach(
                i -> {
                    if (operand[i] >= modulus) {
                        operand[i] -= modulus;
                    }
//                    if (Long.compareUnsigned(operand[i], modulus) >= 0) {
//                        operand[i] -= modulus;
//                    }
                }
        );
    }
}
