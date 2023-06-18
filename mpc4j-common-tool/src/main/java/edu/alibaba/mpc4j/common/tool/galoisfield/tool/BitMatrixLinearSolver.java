package edu.alibaba.mpc4j.common.tool.galoisfield.tool;

import cc.redberry.rings.linear.LinearSolver;
import cc.redberry.rings.util.ArraysUtil;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import gnu.trove.list.array.TIntArrayList;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cc.redberry.rings.linear.LinearSolver.SystemInfo.*;

/**
 * Solving the linear equation Ax = b, where A is a bit matrix represented in a compact form (using byte[][]), x is
 * vector containing elements for which addition and subtraction are all XOR.
 *
 * @author Weiran Liu
 * @date 2023/6/16
 */
public class BitMatrixLinearSolver {
    /**
     * l
     */
    private final int l;
    /**
     * byte l
     */
    private final int byteL;
    /**
     * zero, only use for comparison
     */
    private final byte[] zeroElement;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    public BitMatrixLinearSolver(int l) {
        this(l, new SecureRandom());
    }

    public BitMatrixLinearSolver(int l, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        zeroElement = new byte[byteL];
        Arrays.fill(zeroElement, (byte) 0x00);
        this.secureRandom = secureRandom;
    }

    /**
     * the output of the function rowEchelonFrom.
     */
    private static class RowEchelonFromInfo {
        /**
         * number of zero columns
         */
        private final int nZeroColumns;
        /**
         * max linear independent columns
         */
        private final Set<Integer> maxLisColumns;

        private RowEchelonFromInfo(int nZeroColumns, Set<Integer> maxLisColumns) {
            this.nZeroColumns = nZeroColumns;
            this.maxLisColumns = maxLisColumns;
        }
    }

    /**
     * Gives the row echelon form of the linear system {@code lhs.x = rhs}. Note that here we only allow
     * <p> m (number of columns) >= n (number of rows) </p>
     *
     * @param lhs      the lhs of the system.
     * @param nColumns number of columns.
     * @param rhs      the rhs of the system.
     * @return the number of free variables.
     */
    private RowEchelonFromInfo rowEchelonForm(byte[][] lhs, int nColumns, byte[][] rhs) {
        MathPreconditions.checkEqual("lhs.length", "rhs.length", lhs.length, rhs.length);
        int nRows = lhs.length;
        Set<Integer> maxLisColumns = new HashSet<>(nRows);
        // m >= n
        MathPreconditions.checkGreaterOrEqual("m", nColumns, nRows);
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // verify each row has at most nColumns valid bits
        Arrays.stream(lhs).forEach(row ->
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(row, nByteColumns, nColumns))
        );
        // do not need to solve when nRows = 0
        if (nRows == 0) {
            return new RowEchelonFromInfo(0, maxLisColumns);
        }
        // number of zero columns, here we consider if the leading row is 0
        int nZeroColumns = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            int row = iColumn - nZeroColumns;
            int max = row;
            // find the row where the first bit is not 0
            if (!BinaryUtils.getBoolean(lhs[row], iColumn + nOffsetColumns)) {
                // if we find one, swap
                for (int iRow = row + 1; iRow < nRows; ++iRow) {
                    if (BinaryUtils.getBoolean(lhs[iRow], iColumn + nOffsetColumns)) {
                        max = iRow;
                        break;
                    }
                }
                ArraysUtil.swap(lhs, row, max);
                ArraysUtil.swap(rhs, row, max);
            }
            // if we cannot find one, it means this column is free, nothing to do on this column
            if (!BinaryUtils.getBoolean(lhs[row], iColumn + nOffsetColumns)) {
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // add that column into the set
            maxLisColumns.add(iColumn);
            // forward Gaussian elimination
            for (int iRow = row + 1; iRow < nRows; ++iRow) {
                boolean alpha = BinaryUtils.getBoolean(lhs[iRow], iColumn + nOffsetColumns);
                if (alpha) {
                    BytesUtils.xori(rhs[iRow], rhs[row]);
                    BytesUtils.xori(lhs[iRow], lhs[row]);
                }
            }
        }
        return new RowEchelonFromInfo(nZeroColumns, maxLisColumns);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should have enough length). Free variables are set as zero. Note that lsh is modified when solving
     * the system.
     *
     * @param lhs      the lhs of the system (will be reduced to row echelon form).
     * @param nColumns number of columns.
     * @param rhs      the rhs of the system.
     * @param result   where to place the result.
     * @return system information (inconsistent, under-determined or consistent).
     */
    public LinearSolver.SystemInfo freeSolve(byte[][] lhs, int nColumns, byte[][] rhs, byte[][] result) {
        return solve(lhs, nColumns, rhs, result, false);
    }

    public LinearSolver.SystemInfo fullSolve(byte[][] lhs, int nColumns, byte[][] rhs, byte[][] result) {
        return solve(lhs, nColumns, rhs, result, true);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should be of the enough length). Note that lsh is modified when solving the system.
     *
     * @param lhs      the lhs of the system (will be reduced to row echelon form).
     * @param nColumns number of columns.
     * @param rhs      the rhs of the system.
     * @param isFull   if full free variables with random elements.
     * @param result   where to place the result.
     * @return system information (inconsistent, under-determined or consistent).
     */
    private LinearSolver.SystemInfo solve(byte[][] lhs, int nColumns, byte[][] rhs, byte[][] result, boolean isFull) {
        MathPreconditions.checkEqual("lhs.length", "rhs.length", lhs.length, rhs.length);
        int nRows = lhs.length;
        // m >= n
        MathPreconditions.checkGreaterOrEqual("m", nColumns, nRows);
        MathPreconditions.checkEqual("result.length", "m", result.length, nColumns);
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // verify each row has at most nColumns valid bits
        Arrays.stream(lhs).forEach(row ->
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(row, nByteColumns, nColumns))
        );
        if (nRows == 0) {
            // if n = 0, all solutions are good.
            return Consistent;
        }
        if (nRows == 1) {
            return solveOneRow(lhs, nColumns, rhs, result, isFull);
        }
        // if n > 1, transform lsh to Echelon form.
        RowEchelonFromInfo info = rowEchelonForm(lhs, nColumns, rhs);
        int nUnderDetermined = info.nZeroColumns;
        Arrays.fill(result, createZero());
        // for determined system, free and full solution are the same
        if (nUnderDetermined == 0 && nColumns == nRows) {
            for (int i = nColumns - 1; i >= 0; i--) {
                byte[] sum = createZero();
                for (int j = i + 1; j < nColumns; j++) {
                    if (BinaryUtils.getBoolean(lhs[i], nOffsetColumns + j)) {
                        addi(sum, result[j]);
                    }
                }
                result[i] = sub(rhs[i], sum);
            }
            return Consistent;
        }
        return solveUnderDeterminedRows(lhs, nColumns, rhs, result, info, isFull);
    }

    private LinearSolver.SystemInfo solveOneRow(
        byte[][] lhs, int nColumns, byte[][] rhs, byte[][] result, boolean isFull) {
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        int iRow = 0;
        // if n = 1, then the linear system only has one equation ax = b, therefore x = b * (a^-1)
        if (nColumns == 1) {
            // if m = 1, then we directly compute x = b
            if (BinaryUtils.getBoolean(lhs[iRow], nOffsetColumns)) {
                // if a_0 = 1, then x_0 = b_0
                result[0] = BytesUtils.clone(rhs[iRow]);
                return Consistent;
            } else {
                // if a_0 = 0, it can be solved only if b_0 = 0
                if (isZero(rhs[iRow])) {
                    result[0] = isFull ? BytesUtils.randomByteArray(byteL, l, secureRandom) : new byte[byteL];
                    return Consistent;
                } else {
                    return Inconsistent;
                }
            }
        }
        // if m > 1, the linear system contains free variables.
        Arrays.fill(result, new byte[byteL]);
        // find the first non-zero equation a[i]x = b[i]
        int firstNonZeroColumn = -1;
        for (int i = 0; i < nColumns; ++i) {
            if (BinaryUtils.getBoolean(lhs[iRow], nOffsetColumns + i)) {
                firstNonZeroColumn = i;
                break;
            }
        }
        // if all a[i] = 0, we have solution only if b = 0
        if (firstNonZeroColumn == -1) {
            if (isZero(rhs[iRow])) {
                if (isFull) {
                    // full random variables
                    for (int i = 0; i < nColumns; i++) {
                        result[i] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                    }
                }
                return Consistent;
            } else {
                // if all a[i] = 0 and b != 0, this means we do not have any solution.
                return Inconsistent;
            }
        } else {
            // b != 0, we need to consider the first non-zero equation a[i]x = b[i].
            if (isFull) {
                // set random variables
                for (int i = 0; i < nColumns; ++i) {
                    if (i == firstNonZeroColumn) {
                        continue;
                    }
                    result[i] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                    if (BinaryUtils.getBoolean(lhs[iRow], nOffsetColumns + i)) {
                        // zero position, set random variable, and r[0] = r[0] - r[i]).
                        subi(rhs[iRow], result[i]);
                    }
                }
            }
            // b != 0, we only need to consider the first non-zero equation a[i]x = b[i],
            // and set x[i] = b, leaving other x[j] as 0.
            result[firstNonZeroColumn] = BytesUtils.clone(rhs[iRow]);
            return Consistent;
        }
    }

    private LinearSolver.SystemInfo solveUnderDeterminedRows(byte[][] lhs, int nColumns, byte[][] rhs, byte[][] result,
                                                             RowEchelonFromInfo info, boolean isFull) {
        int nRows = lhs.length;
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // back substitution in case of under-determined system
        TIntArrayList nzColumns = new TIntArrayList(), nzRows = new TIntArrayList();
        // number of zero columns
        int nZeroColumns = 0;
        int iRow = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            iRow = iColumn - nZeroColumns;
            if (!BinaryUtils.getBoolean(lhs[iRow], nOffsetColumns + iColumn)) {
                if (iColumn == (nColumns - 1) && !isZero(rhs[iRow])) {
                    return Inconsistent;
                }
                ++nZeroColumns;
                // full solution needs to set the corresponding result[iColumn] as a random variable
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // scale current row, that is, make lhs[iRow][iColumn] = 1, and scale other entries in this row.
            // it should be as follows, but here val == 1, valInv = 1, so we can ignore these steps
            // val = row[iColumn]; valInv = 1 / val;
            // for (int i = iColumn; i < nColumns; i++) { row[i] = valInv * row[i] }
            byte[] row = lhs[iRow];
            // scale all rows before
            for (int i = 0; i < iRow; i++) {
                byte[] pRow = lhs[i];
                boolean v = BinaryUtils.getBoolean(pRow, nOffsetColumns + iColumn);
                if (!v) {
                    continue;
                }
                BytesUtils.xori(pRow, row);
                subi(rhs[i], rhs[iRow]);
            }
            if (!isZero(rhs[iRow]) && !BinaryUtils.getBoolean(lhs[iRow], nOffsetColumns + iColumn)) {
                return Inconsistent;
            }
            // label that column and its corresponding row for the solution b[row].
            nzColumns.add(iColumn);
            nzRows.add(iRow);
        }
        ++iRow;
        if (iRow < nRows) {
            for (; iRow < nRows; ++iRow) {
                if (!isZero(rhs[iRow])) {
                    return Inconsistent;
                }
            }
        }
        for (int i = 0; i < nzColumns.size(); ++i) {
            result[nzColumns.get(i)] = rhs[nzRows.get(i)];
        }
        if (isFull) {
            Set<Integer> maxLisColumns = info.maxLisColumns;
            Set<Integer> nonMaxLisColumns = IntStream.range(0, nColumns).boxed().collect(Collectors.toSet());
            nonMaxLisColumns.removeAll(maxLisColumns);
            // set result[iColumn] corresponding to the non-maxLisColumns as random variables
            for (int nonMaxLisColumn : nonMaxLisColumns) {
                result[nonMaxLisColumn] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
            for (int i = 0; i < nzColumns.size(); ++i) {
                int iNzColumn = nzColumns.get(i);
                int iNzRow = nzRows.get(i);
                // subtract other free variables
                for (int nonMaxLisColumn : nonMaxLisColumns) {
                    if (BinaryUtils.getBoolean(lhs[iNzRow], nOffsetColumns + nonMaxLisColumn)) {
                        subi(result[iNzColumn], result[nonMaxLisColumn]);
                    }
                }
            }
        }
        return Consistent;
    }

    private boolean isZero(byte[] element) {
        return Arrays.equals(zeroElement, element);
    }

    private byte[] createZero() {
        return new byte[byteL];
    }

    private void addi(byte[] p, byte[] q) {
        BytesUtils.xori(p, q);
    }

    private byte[] sub(byte[] p, byte[] q) {
        return BytesUtils.xor(p, q);
    }

    private void subi(byte[] p, byte[] q) {
        BytesUtils.xori(p, q);
    }
}
