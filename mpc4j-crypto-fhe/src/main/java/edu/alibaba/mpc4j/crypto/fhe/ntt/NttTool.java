package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.iterator.PolyIter;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
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

    public static void nttNegAcyclicHarveyLazy(long[] operandArray, int startIndex, NttTables tables) {
        tables.nttHandler.transformToRev(
                operandArray,
                startIndex,
                tables.getCoeffCountPower(),
                tables.getRootPowers(),
                null
        );
    }


    public static void nttNegAcyclicHarveyLazy(RnsIter operand, int coeffModulusSize, NttTables[] tables) {

        IntStream.range(0, coeffModulusSize).parallel().forEach(
//                i -> nttNegAcyclicHarveyLazy(operand.getCoeffIter(i), tables[i])
                i -> nttNegAcyclicHarvey(operand.coeffIter, i * tables[i].getCoeffCount(), tables[i])
        );
    }


    public static void nttNegAcyclicHarveyLazy(PolyIter operand, int size, NttTables[] tables) {
        int coeffModulusSize = operand.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> nttNegAcyclicHarveyLazy(operand.getRnsIter(i), coeffModulusSize, tables)
        );
    }

    public static void nttNegAcyclicHarvey(RnsIter operand, int coeffModulusSize, NttTables[] tables) {


        IntStream.range(0, coeffModulusSize).parallel().forEach(
//                i -> nttNegAcyclicHarvey(operand.getCoeffIter(i), tables[i])
                i -> nttNegAcyclicHarvey(operand.coeffIter, i * tables[i].getCoeffCount(), tables[i])
        );
    }

    public static void nttNegAcyclicHarvey(long[] rnsIter, int coeffModulusSize, NttTables[] tables) {

        assert rnsIter.length == coeffModulusSize * tables[0].getCoeffCount();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
//                i -> nttNegAcyclicHarvey(operand.getCoeffIter(i), tables[i])
                i -> nttNegAcyclicHarvey(rnsIter, i * tables[i].getCoeffCount(), tables[i])
        );
    }


    public static void nttNegAcyclicHarvey(PolyIter operand, int size, NttTables[] tables) {
        int coeffModulusSize = operand.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> nttNegAcyclicHarvey(operand.getRnsIter(i), coeffModulusSize, tables)
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

    public static void nttNegAcyclicHarvey(long[] operandArray, int startIndex, NttTables tables) {


        nttNegAcyclicHarveyLazy(operandArray, startIndex, tables);
        // Finally maybe we need to reduce every coefficient modulo q, but we
        // know that they are in the range [0, 4q).
        // Since word size is controlled this is fast.
        long modulus = tables.getModulus().getValue();
        long twoTimesModulus = modulus * 2;
        int n = 1 << tables.getCoeffCountPower();

        IntStream.range(0, n).parallel().forEach(
                i -> {
                    // Note: I must be passed to the lambda by reference.
                    if (operandArray[startIndex + i] >= twoTimesModulus) {
                        operandArray[startIndex + i] -= twoTimesModulus;
                    }
                    if (operandArray[startIndex + i] >= modulus) {
                        operandArray[startIndex + i] -= modulus;
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

    public static void inverseNttNegAcyclicHarveyLazy(long[] operandArray, int startIndex, NttTables tables) {

        MultiplyUintModOperand invDegreeModulo = tables.getInvDegreeModulo();

        tables.nttHandler.transformFromRev(
                operandArray,
                startIndex,
                tables.getCoeffCountPower(),
                tables.getInvRootPowers(),
                invDegreeModulo
        );
    }

    public static void inverseNttNegAcyclicHarveyLazy(RnsIter operand, int coeffModulusSize, NttTables[] tables) {

        IntStream.range(0, coeffModulusSize).parallel().forEach(
//                i -> inverseNttNegAcyclicHarveyLazy(operand.getCoeffIter(i), tables[i])
                i -> inverseNttNegAcyclicHarveyLazy(operand.coeffIter, i * tables[i].getCoeffCount(), tables[i])
        );

    }

    public static void inverseNttNegAcyclicHarveyLazy(PolyIter operand, int size, NttTables[] tables) {
        int coeffModulusSize = operand.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> inverseNttNegAcyclicHarveyLazy(operand.getRnsIter(i), coeffModulusSize, tables)
        );
    }

    public static void inverseNttNegAcyclicHarvey(RnsIter operand, int coeffModulusSize, NttTables[] tables) {

        IntStream.range(0, coeffModulusSize).parallel().forEach(
//                i -> inverseNttNegAcyclicHarvey(operand.getCoeffIter(i), tables[i])
                i -> inverseNttNegAcyclicHarvey(operand.coeffIter, i * tables[i].getCoeffCount(), tables[i])
        );

    }

    public static void inverseNttNegAcyclicHarvey(PolyIter operand, int size, NttTables[] tables) {
        int coeffModulusSize = operand.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> inverseNttNegAcyclicHarvey(operand.getRnsIter(i), coeffModulusSize, tables)
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

    public static void inverseNttNegAcyclicHarvey(long[] operandArray, int startIndex, NttTables tables) {

        inverseNttNegAcyclicHarveyLazy(operandArray, startIndex, tables);

        long modulus = tables.getModulus().getValue();
        int n = 1 << tables.getCoeffCountPower();
        IntStream.range(0, n).parallel().forEach(
                i -> {
                    if (operandArray[startIndex + i] >= modulus) {
                        operandArray[startIndex + i] -= modulus;
                    }
//                    if (Long.compareUnsigned(operand[i], modulus) >= 0) {
//                        operand[i] -= modulus;
//                    }
                }
        );
    }
}
