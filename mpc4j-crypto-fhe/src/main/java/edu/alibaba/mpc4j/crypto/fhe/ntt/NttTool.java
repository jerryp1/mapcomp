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

        for (int i = 0; i < coeffModulusSize; i++) {
            nttNegAcyclicHarvey(operand.coeffIter, i * tables[i].getCoeffCount(), tables[i]);
        }
    }


    public static void nttNegAcyclicHarveyLazy(PolyIter operand, int size, NttTables[] tables) {
        int coeffModulusSize = operand.getCoeffModulusSize();


        IntStream.range(0, size).forEach(
                i -> nttNegAcyclicHarveyLazy(operand.getRnsIter(i), coeffModulusSize, tables)
        );
    }

    public static void nttNegAcyclicHarvey(RnsIter operand, int coeffModulusSize, NttTables[] tables) {


        for (int i = 0; i < coeffModulusSize; i++) {
            nttNegAcyclicHarvey(operand.coeffIter, i * tables[i].getCoeffCount(), tables[i]);
        }

    }

    /**
     * 对一个 RnsIter 整体进行处理, long[] + rnsIterCoeffCount 来构成一个 RnsIter 对象，避免了 new RnsIter 带来的额外开销
     *
     * @param rnsIter           k * N
     * @param rnsIterCoeffCount
     * @param coeffModulusSize
     * @param tables
     */
    public static void nttNegAcyclicHarveyRnsIter(long[] rnsIter, int rnsIterCoeffCount, int coeffModulusSize, NttTables[] tables) {
        //todo: consider remove
        assert rnsIterCoeffCount == tables[0].getCoeffCount();

        for (int i = 0; i < coeffModulusSize; i++) {
            nttNegAcyclicHarvey(rnsIter, i * rnsIterCoeffCount, tables[i]);
        }

//        IntStream.range(0, coeffModulusSize).parallel().forEach(
//                i -> nttNegAcyclicHarvey(rnsIter, i * rnsIterCoeffCount, tables[i])
//        );
    }

    /**
     * [0, N) ---> NttForward, [N, 2N) ---> NttForward
     *
     * @param rnsIter          k * N
     * @param coeffModulusSize k
     * @param tables
     */
    public static void nttNegAcyclicHarvey(long[] rnsIter, int coeffModulusSize, NttTables[] tables) {

        assert rnsIter.length == coeffModulusSize * tables[0].getCoeffCount();
        for (int i = 0; i < coeffModulusSize; i++) {
            nttNegAcyclicHarvey(rnsIter, i * tables[i].getCoeffCount(), tables[i]);
        }

//        IntStream.range(0, coeffModulusSize).parallel().forEach(
//                i -> nttNegAcyclicHarvey(rnsIter, i * tables[i].getCoeffCount(), tables[i])
//        );
    }

    public static void nttNegAcyclicHarveyLazy(long[] rnsIter, int coeffModulusSize, NttTables[] tables) {

        assert rnsIter.length == coeffModulusSize * tables[0].getCoeffCount();

        IntStream.range(0, coeffModulusSize).forEach(
                i -> nttNegAcyclicHarveyLazy(rnsIter, i * tables[i].getCoeffCount(), tables[i])
        );
    }

    /**
     * 处理一个 RnsIter = long[] + startIndex , 不关心 long[] 到底是什么，只要 long[] + startIndex 指向一个RnsIter 的起点即可
     *
     * @param polyIter
     * @param startIndex
     * @param coeffModulusSize
     * @param tables
     */
    public static void nttNegAcyclicHarveyLazyRnsIter(
            long[] polyIter,
            int startIndex,
            int coeffModulusSize,
            NttTables[] tables) {

        assert startIndex % (coeffModulusSize * tables[0].getCoeffCount()) == 0;
        assert coeffModulusSize == tables.length;

        int coeffCount = tables[0].getCoeffCount();

        for (int i = 0; i < coeffModulusSize; i++) {
            nttNegAcyclicHarveyLazy(
                    polyIter,
                    startIndex + i * coeffCount,
                    tables[i]);
        }

//        IntStream.range(0, coeffModulusSize).parallel().forEach(
//                // 依次处理单个 CoeffCount
//                i -> nttNegAcyclicHarveyLazy(
//                        polyIter,
//                        startIndex +  i * coeffCount,
//                        tables[i])
//        );
    }

    public static void inverseNttNegAcyclicHarveyLazyRnsIter(
            long[] polyIter,
            int startIndex,
            int coeffModulusSize,
            NttTables[] tables) {

        assert startIndex % (coeffModulusSize * tables[0].getCoeffCount()) == 0;

        IntStream.range(0, coeffModulusSize).forEach(
                i -> inverseNttNegAcyclicHarveyLazy(polyIter, startIndex + i * tables[i].getCoeffCount(), tables[i])
        );
    }

    /**
     * polyIter + startIndx = RnsIter
     *
     * @param polyIter
     * @param startIndex
     * @param coeffModulusSize
     * @param tables
     */
    public static void nttNegAcyclicHarvey(long[] polyIter, int startIndex, int coeffModulusSize, NttTables[] tables) {

        assert startIndex % coeffModulusSize * tables[0].getCoeffCount() == 0;

        for (int i = 0; i < coeffModulusSize; i++) {
            // RnsIter + startIndex = CoeffIter , 最底层这里仍然只处理 单个 CoeffIter
            nttNegAcyclicHarvey(polyIter, startIndex + i * tables[i].getCoeffCount(), tables[i]);
        }
    }


    public static void nttNegAcyclicHarvey(PolyIter operand, int size, NttTables[] tables) {
        int coeffModulusSize = operand.getCoeffModulusSize();

        IntStream.range(0, size).forEach(
                i -> nttNegAcyclicHarvey(operand.getRnsIter(i), coeffModulusSize, tables)
        );
    }

    /**
     * 处理一个完整的 polyIter = long[] + N + k
     *
     * @param operand
     * @param operandCoeffCount
     * @param operandCoeffModulusSize
     * @param size
     * @param tables
     */
    public static void nttNegAcyclicHarveyPolyIter(
            long[] operand,
            int operandCoeffCount,
            int operandCoeffModulusSize,
            int size,
            NttTables[] tables) {

        // 遍历每一个密文多项式
        for (int i = 0; i < size; i++) {
            int rnsStartIndex = i * operandCoeffCount * operandCoeffModulusSize;
            // 遍历每一个 rns 下的 多项式
            for (int j = 0; j < operandCoeffModulusSize; j++) {
                // 处理每一个 CoeffIter
                nttNegAcyclicHarvey(
                        operand,
                        rnsStartIndex + j * operandCoeffCount,
                        tables[j]
                );
            }
        }
    }

    public static void inverseNttNegAcyclicHarveyPolyIter(
            long[] operand,
            int operandCoeffCount,
            int operandCoeffModulusSize,
            int size,
            NttTables[] tables) {

        // 遍历每一个密文多项式
        for (int i = 0; i < size; i++) {
            int rnsStartIndex = i * operandCoeffCount * operandCoeffModulusSize;
            // 遍历每一个 rns 下的 多项式
            for (int j = 0; j < operandCoeffModulusSize; j++) {
                // 处理每一个 CoeffIter
                inverseNttNegAcyclicHarvey(
                        operand,
                        rnsStartIndex + j * operandCoeffCount,
                        tables[j]
                );
            }
        }
    }

    public static void inverseNttNegAcyclicHarveyLazyPolyIter(
            long[] operand,
            int operandCoeffCount,
            int operandCoeffModulusSize,
            int size,
            NttTables[] tables) {

        // 遍历每一个密文多项式
        for (int i = 0; i < size; i++) {
            int rnsStartIndex = i * operandCoeffCount * operandCoeffModulusSize;
            // 遍历每一个 rns 下的 多项式
            for (int j = 0; j < operandCoeffModulusSize; j++) {
                // 处理每一个 CoeffIter
                inverseNttNegAcyclicHarveyLazy(
                        operand,
                        rnsStartIndex + j * operandCoeffCount,
                        tables[j]
                );
            }
        }
    }


    public static void nttNegAcyclicHarvey(long[] operand, NttTables tables) {


        nttNegAcyclicHarveyLazy(operand, tables);
        // Finally maybe we need to reduce every coefficient modulo q, but we
        // know that they are in the range [0, 4q).
        // Since word size is controlled this is fast.
        long modulus = tables.getModulus().getValue();
        long twoTimesModulus = modulus * 2;
        int n = 1 << tables.getCoeffCountPower();

        for (int i = 0; i < n; i++) {
            // Note: I must be passed to the lambda by reference.
            if (operand[i] >= twoTimesModulus) {
                operand[i] -= twoTimesModulus;
            }
            if (operand[i] >= modulus) {
                operand[i] -= modulus;
            }
        }
    }

    public static void nttNegAcyclicHarvey(long[] operandArray, int startIndex, NttTables tables) {


        nttNegAcyclicHarveyLazy(operandArray, startIndex, tables);
        // Finally maybe we need to reduce every coefficient modulo q, but we
        // know that they are in the range [0, 4q).
        // Since word size is controlled this is fast.
        long modulus = tables.getModulus().getValue();
        long twoTimesModulus = modulus * 2;
        int n = 1 << tables.getCoeffCountPower();

        for (int i = 0; i < n; i++) {
            // Note: I must be passed to the lambda by reference.
            if (operandArray[startIndex + i] >= twoTimesModulus) {
                operandArray[startIndex + i] -= twoTimesModulus;
            }
            if (operandArray[startIndex + i] >= modulus) {
                operandArray[startIndex + i] -= modulus;
            }
        }
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

        for (int i = 0; i < coeffModulusSize; i++) {
            inverseNttNegAcyclicHarveyLazy(operand.coeffIter, i * tables[i].getCoeffCount(), tables[i]);

        }


    }

    public static void inverseNttNegAcyclicHarveyLazy(PolyIter operand, int size, NttTables[] tables) {
        int coeffModulusSize = operand.getCoeffModulusSize();

        IntStream.range(0, size).forEach(
                i -> inverseNttNegAcyclicHarveyLazy(operand.getRnsIter(i), coeffModulusSize, tables)
        );
    }

    public static void inverseNttNegAcyclicHarvey(RnsIter operand, int coeffModulusSize, NttTables[] tables) {

        for (int i = 0; i < coeffModulusSize; i++) {
            inverseNttNegAcyclicHarvey(operand.coeffIter, i * tables[i].getCoeffCount(), tables[i]);
        }
    }


    public static void inverseNttNegAcyclicHarveyRnsIter(long[] rnsIter, int rnsIterCoeffCount, int coeffModulusSize, NttTables[] tables) {

        assert rnsIterCoeffCount == tables[0].getCoeffCount();

        for (int i = 0; i < coeffModulusSize; i++) {
            inverseNttNegAcyclicHarvey(rnsIter, i * tables[i].getCoeffCount(), tables[i]);
        }

    }


    public static void inverseNttNegAcyclicHarvey(PolyIter operand, int size, NttTables[] tables) {
        int coeffModulusSize = operand.getCoeffModulusSize();

        IntStream.range(0, size).forEach(
                i -> inverseNttNegAcyclicHarvey(operand.getRnsIter(i), coeffModulusSize, tables)
        );
    }

    public static void inverseNttNegAcyclicHarvey(long[] operand, NttTables tables) {

        inverseNttNegAcyclicHarveyLazy(operand, tables);

        long modulus = tables.getModulus().getValue();
        int n = 1 << tables.getCoeffCountPower();
        for (int i = 0; i < n; i++) {
            if (operand[i] >= modulus) {
                operand[i] -= modulus;
            }
        }
    }

    public static void inverseNttNegAcyclicHarvey(long[] operandArray, int startIndex, NttTables tables) {

        inverseNttNegAcyclicHarveyLazy(operandArray, startIndex, tables);

        long modulus = tables.getModulus().getValue();
        int n = 1 << tables.getCoeffCountPower();
        for (int i = 0; i < n; i++) {
            if (operandArray[startIndex + i] >= modulus) {
                operandArray[startIndex + i] -= modulus;
            }
        }
    }
}
