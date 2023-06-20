package edu.alibaba.mpc4j.crypto.matrix.okve.tool;

import cc.redberry.rings.linear.LinearSolver;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * bit matrix linear solver test.
 *
 * @author Weiran Liu
 * @date 2023/6/16
 */
@RunWith(Parameterized.class)
public class BitMatrixLinearSolverTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 1000;
    /**
     * l
     */
    private static final int L = CommonConstants.STATS_BIT_LENGTH;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        int[] ds = new int[]{7, 8, 9, 15, 16, 17, 39, 40, 41, 128, 256};
        // add each l
        for (int d : ds) {
            configurations.add(new Object[]{"D = " + d + ")", d});
        }

        return configurations;
    }

    /**
     * dimension d
     */
    private final int d;
    /**
     * byte d
     */
    private final int byteD;
    /**
     * offset d
     */
    private final int offsetD;
    /**
     * GF(2^e) instance
     */
    private final Gf2e gf2e;
    /**
     * linear solver
     */
    private final BitMatrixLinearSolver linearSolver;

    public BitMatrixLinearSolverTest(String name, int d) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.d = d;
        byteD = CommonUtils.getByteLength(d);
        offsetD = byteD * Byte.SIZE - d;
        gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, L);
        linearSolver = new BitMatrixLinearSolver(L);
    }

    @Test
    public void testIdentitySquareFullRank() {
        List<byte[]> identityRows = IntStream.range(0, d)
            .mapToObj(rowIndex -> {
                byte[] row = new byte[byteD];
                BinaryUtils.setBoolean(row, rowIndex + offsetD, true);
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(identityRows, SECURE_RANDOM);
            byte[][] matrixA = identityRows.toArray(new byte[0][]);
            byte[][] b = new byte[d][];
            for (int row = 0; row < d; row++) {
                b[row] = gf2e.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, d, b);
        }
    }

    @Test
    public void testIdentitySquareNotFullRank() {
        List<byte[]> identityRows = IntStream.range(0, d)
            .mapToObj(rowIndex -> {
                byte[] row = new byte[byteD];
                BinaryUtils.setBoolean(row, rowIndex + offsetD, true);
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(identityRows, SECURE_RANDOM);
            byte[][] matrixA = BytesUtils.clone(identityRows.toArray(new byte[0][]));
            byte[][] b = new byte[d][];
            for (int row = 0; row < d; row++) {
                b[row] = gf2e.createRandom(SECURE_RANDOM);
            }
            // set a random row to be 0
            int r = SECURE_RANDOM.nextInt(d);
            matrixA[r] = new byte[byteD];
            b[r] = gf2e.createZero();
            testGaussianElimination(matrixA, d, b);
        }
    }

    @Test
    public void testRandomSquareFullRank() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // we choose a full rank matrix
            byte[][] matrixA = new byte[d][];
            for (int row = 0; row < d; row++) {
                matrixA[row] = BytesUtils.randomByteArray(byteD, d, SECURE_RANDOM);
            }
            try {
                ByteDenseBitMatrix bitMatrix = ByteDenseBitMatrix.createFromDense(d, matrixA);
                bitMatrix.inverse();
            } catch (ArithmeticException e) {
                continue;
            }
            byte[][] b = new byte[d][];
            for (int row = 0; row < d; row++) {
                b[row] = gf2e.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, d, b);
        }
    }

    @Test
    public void testRandomSquareNotFullRank() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            byte[][] matrixA = new byte[d][];
            byte[][] b = new byte[d][];
            // set random full-rank matrix
            for (int row = 0; row < d; row++) {
                matrixA[row] = BytesUtils.randomByteArray(byteD, d, SECURE_RANDOM);
                b[row] = gf2e.createRandom(SECURE_RANDOM);
            }
            try {
                ByteDenseBitMatrix bitMatrix = ByteDenseBitMatrix.createFromDense(d, matrixA);
                bitMatrix.inverse();
            } catch (ArithmeticException e) {
                continue;
            }
            // set a random row to be 0
            int r = SECURE_RANDOM.nextInt(d);
            matrixA[r] = new byte[byteD];
            b[r] = gf2e.createZero();
            testGaussianElimination(matrixA, d, b);
        }
    }

    @Test
    public void testRectangular() {
        for (int round = 0; round < RANDOM_ROUND; round++) {
            byte[][] matrixA = new byte[d][];
            int nDoubleColumn = d * 2;
            int nByteDoubleColumn = CommonUtils.getByteLength(nDoubleColumn);
            int nOffsetDoubleColumn = nByteDoubleColumn * Byte.SIZE - nDoubleColumn;
            for (int row = 0; row < d; row++) {
                matrixA[row] = new byte[nByteDoubleColumn];
                // the left-most and the right-most bits are set to true
                BinaryUtils.setBoolean(matrixA[row], nOffsetDoubleColumn + row, true);
                BinaryUtils.setBoolean(matrixA[row], nOffsetDoubleColumn + 2 * d - 1 - row, true);
            }
            byte[][] b = new byte[d][];
            for (int row = 0; row < d; row++) {
                b[row] = gf2e.createRandom(SECURE_RANDOM);
            }
            testGaussianElimination(matrixA, nDoubleColumn, b);
        }
    }

    private void testGaussianElimination(byte[][] matrixA, int nColumn, byte[][] b) {
        byte[][] x = new byte[nColumn][];
        LinearSolver.SystemInfo systemInfo;
        // free solve
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), nColumn, BytesUtils.clone(b), x);
        Assert.assertNotEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        assertCorrect(matrixA, nColumn, b, x);
        // full solve
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), nColumn, BytesUtils.clone(b), x);
        Assert.assertNotEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        assertCorrect(matrixA, nColumn, b, x);
        for (byte[] xi : x) {
            Assert.assertFalse(gf2e.isZero(xi));
        }
    }

    private void assertCorrect(byte[][] matrixA, int nColumn, byte[][] b, byte[][] x) {
        int nrow = b.length;
        int nByteColumn = CommonUtils.getByteLength(nColumn);
        int nOffsetColumn = nByteColumn * Byte.SIZE - nColumn;
        for (int rowIndex = 0; rowIndex < nrow; rowIndex++) {
            byte[] res = gf2e.createZero();
            for (int columnIndex = 0; columnIndex < nColumn; columnIndex++) {
                if (BinaryUtils.getBoolean(matrixA[rowIndex], nOffsetColumn + columnIndex)) {
                    gf2e.addi(res, x[columnIndex]);
                }
            }
            Assert.assertArrayEquals(b[rowIndex], res);
        }
    }
}
