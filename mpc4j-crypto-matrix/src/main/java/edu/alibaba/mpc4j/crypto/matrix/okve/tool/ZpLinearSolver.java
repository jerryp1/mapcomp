package edu.alibaba.mpc4j.crypto.matrix.okve.tool;

import cc.redberry.rings.linear.LinearSolver;
import cc.redberry.rings.util.ArraysUtil;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import static cc.redberry.rings.linear.LinearSolver.SystemInfo.*;

/**
 * Solving the linear equation Ax = b, where A is a bit matrix represented in a compact from (using byte[][]), x is a
 * vector containing Zp elements.
 * <p>
 * 此线性求解器代码参考了Rings中的实现（参见cc.redberry.rings.linear.LinearSolver）。
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/7/6
 */
public class ZpLinearSolver {
    /**
     * Zp instance
     */
    private final Zp zp;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    public ZpLinearSolver(Zp zp) {
        this(zp, new SecureRandom());
    }

    public ZpLinearSolver(Zp zp, SecureRandom secureRandom) {
        this.zp = zp;
        this.secureRandom = secureRandom;
    }

    /**
     * Gives the row echelon form of the linear system {@code lhs.x = rhs}. Note that here we only allow
     * <p> m (number of columns) >= n (number of rows) </p>
     *
     * @param lhs the lhs of the system.
     * @param rhs the rhs of the system.
     * @return the information for row Echelon form.
     */
    private RowEchelonFormInfo rowEchelonForm(BigInteger[][] lhs, BigInteger[] rhs) {
        MathPreconditions.checkEqual("lhs.length", "rhs.length", lhs.length, rhs.length);
        int nRows = lhs.length;
        TIntSet maxLisColumns = new TIntHashSet(nRows);
        // do not need to solve when nRows = 0
        if (nRows == 0) {
            return new RowEchelonFormInfo(0, maxLisColumns);
        }
        // m >= n
        MathPreconditions.checkGreaterOrEqual("m", lhs[0].length, nRows);
        int nColumns = lhs[0].length;
        // verify each row has m elements
        Arrays.stream(lhs).forEach(row ->
            MathPreconditions.checkEqual("m", "lsh[i].length", nColumns, row.length)
        );
        // number of zero columns, here we consider if some columns are 0.
        int nZeroColumns = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            int row = iColumn - nZeroColumns;
            int max = row;
            if (zp.isZero(lhs[row][iColumn])) {
                for (int iRow = row + 1; iRow < nRows; ++iRow) {
                    if (!zp.isZero(lhs[iRow][iColumn])) {
                        max = iRow;
                        break;
                    }
                }
                ArraysUtil.swap(lhs, row, max);
                ArraysUtil.swap(rhs, row, max);
            }
            // if we cannot find one, it means this column is free, nothing to do on this column
            if (zp.isZero(lhs[row][iColumn])) {
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // add that column into the set
            maxLisColumns.add(iColumn);
            // forward Gaussian elimination
            for (int iRow = row + 1; iRow < nRows; ++iRow) {
                BigInteger alpha = zp.div(lhs[iRow][iColumn], lhs[row][iColumn]);
                rhs[iRow] = zp.sub(rhs[iRow], zp.mul(rhs[row], alpha));
                if (!zp.isZero(alpha)) {
                    for (int iCol = iColumn; iCol < nColumns; ++iCol) {
                        lhs[iRow][iCol] = zp.sub(lhs[iRow][iCol], zp.mul(alpha, lhs[row][iCol]));
                    }
                }
            }
        }
        return new RowEchelonFormInfo(nZeroColumns, maxLisColumns);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should be of the enough length). Here rhs and result are represented as ECPoint.
     *
     * @param lhs                    the lhs of the system (will be reduced to row echelon form).
     * @param rhs                    the rhs of the system.
     * @param result                 where to place the result.
     * @param solveIfUnderDetermined give some solution even if the system is under determined.
     * @return system information (inconsistent, under-determined or consistent).
     */
    public LinearSolver.SystemInfo solve(BigInteger[][] lhs, BigInteger[] rhs, BigInteger[] result, boolean solveIfUnderDetermined) {
        if (lhs.length != rhs.length) {
            throw new IllegalArgumentException("lhs.length != rhs.length");
        }
        if (rhs.length == 0) {
            return Consistent;
        }
        if (rhs.length == 1) {
            // 如果只有一个约束条件，则方程变为ax = b，此时x = b * (a^-1)
            if (lhs[0].length == 1) {
                result[0] = zp.div(rhs[0], lhs[0][0]);
                return Consistent;
            }
            if (solveIfUnderDetermined) {
                Arrays.fill(result, BigInteger.ZERO);
                // 如果b = 0，则方程组的形式全部为ax = 0，此时x = 0
                if (rhs[0].equals(BigInteger.ZERO)) {
                    return Consistent;
                }
                // 如果b != 0，则只需要考虑第一个约束条件
                for (int i = 0; i < result.length; ++i) {
                    if (!lhs[0][i].equals(BigInteger.ZERO)) {
                        result[i] = zp.div(rhs[0], lhs[0][i]);
                        return Consistent;
                    }
                }
                return Inconsistent;
            }
            if (lhs[0].length > 1) {
                return UnderDetermined;
            }
            return Inconsistent;
        }
        // if n > 1, transform lsh to Echelon form.
        RowEchelonFormInfo info = rowEchelonForm(lhs, rhs);
        int nUnderDetermined = info.getZeroColumnNum();
        if (!solveIfUnderDetermined && nUnderDetermined > 0) {
            // under-determined system
            return UnderDetermined;
        }

        int nRows = rhs.length;
        int nColumns = lhs[0].length;

        if (!solveIfUnderDetermined && nColumns > nRows) {
            // under-determined system
            return UnderDetermined;
        }

        if (nRows > nColumns) {
            // over-determined system
            // check that all rhs are zero
            for (int i = nColumns; i < nRows; ++i) {
                if (!rhs[i].equals(BigInteger.ZERO)) {
                    // inconsistent system
                    return Inconsistent;
                }
            }
        }

        if (nRows > nColumns) {
            for (int i = nColumns + 1; i < nRows; ++i) {
                if (!rhs[i].equals(BigInteger.ZERO)) {
                    return Inconsistent;
                }
            }
        }

        Arrays.fill(result, BigInteger.ZERO);
        // back substitution in case of determined system
        if (nUnderDetermined == 0 && nColumns <= nRows) {
            for (int i = nColumns - 1; i >= 0; i--) {
                BigInteger sum = BigInteger.ZERO;
                for (int j = i + 1; j < nColumns; j++) {
                    sum = zp.add(sum, zp.mul(result[j], lhs[i][j]));
                }
                result[i] = zp.div(zp.sub(rhs[i], sum), lhs[i][i]);
            }
            return Consistent;
        }

        // back substitution in case of underdetermined system
        TIntArrayList nzColumns = new TIntArrayList(), nzRows = new TIntArrayList();
        //number of zero columns
        int nZeroColumns = 0;
        int iRow = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            iRow = iColumn - nZeroColumns;
            if (lhs[iRow][iColumn].equals(BigInteger.ZERO)) {
                if (iColumn == (nColumns - 1) && !rhs[iRow].equals(BigInteger.ZERO)) {
                    return Inconsistent;
                }
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }

            // scale current row
            BigInteger[] row = lhs[iRow];
            BigInteger val = row[iColumn];
            BigInteger valInv = zp.inv(val);

            for (int i = iColumn; i < nColumns; i++) {
                row[i] = zp.mul(valInv, row[i]);
            }

            rhs[iRow] = zp.mul(rhs[iRow], valInv);

            // scale all rows before
            for (int i = 0; i < iRow; i++) {
                BigInteger[] pRow = lhs[i];
                BigInteger v = pRow[iColumn];
                if (v.equals(BigInteger.ZERO)) {
                    continue;
                }
                for (int j = iColumn; j < nColumns; ++j) {
                    pRow[j] = zp.sub(pRow[j], zp.mul(v, row[j]));
                }
                rhs[i] = zp.sub(rhs[i], zp.mul(rhs[iRow], v));
            }
            if (!rhs[iRow].equals(BigInteger.ZERO) && lhs[iRow][iColumn].equals(BigInteger.ZERO)) {
                return Inconsistent;
            }
            nzColumns.add(iColumn);
            nzRows.add(iRow);
        }

        ++iRow;
        if (iRow < nRows) {
            for (; iRow < nRows; ++iRow) {
                if (!rhs[iRow].equals(BigInteger.ZERO)) {
                    return Inconsistent;
                }
            }
        }

        for (int i = 0; i < nzColumns.size(); ++i) {
            result[nzColumns.get(i)] = rhs[nzRows.get(i)];
        }

        return Consistent;
    }
}