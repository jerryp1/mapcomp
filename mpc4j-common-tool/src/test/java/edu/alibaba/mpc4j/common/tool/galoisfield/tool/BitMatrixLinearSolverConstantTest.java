package edu.alibaba.mpc4j.common.tool.galoisfield.tool;

import cc.redberry.rings.linear.LinearSolver;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * bit matrix linear solver test with constant inputs.
 *
 * @author Weiran Liu
 * @date 2023/6/17
 */
public class BitMatrixLinearSolverConstantTest {
    /**
     * l
     */
    private static final int L = CommonConstants.STATS_BIT_LENGTH;
    /**
     * l in byte
     */
    private static final int BYTE_L = CommonUtils.getByteLength(L);
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * GF(2^e) instance
     */
    private final Gf2e gf2e;
    /**
     * linear solver
     */
    private final BitMatrixLinearSolver linearSolver;

    public BitMatrixLinearSolverConstantTest() {
        gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, L);
        linearSolver = new BitMatrixLinearSolver(L);
    }

    @Test
    public void testFree1x1() {
        int m = 1;
        // A = | 1 |, b = 0, solve Ax = b.
        byte[][] matrixA = new byte[][]{
            new byte[]{0x01},
        };
        byte[] b0 = new byte[BYTE_L];
        byte[][] b = new byte[][]{
            BytesUtils.clone(b0),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));

        // A = | 1 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000001},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b0, x[0]));

        // A = | 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b0 = new byte[BYTE_L];
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));

        // A = | 0 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testFull1x1() {
        int m = 1;
        // A = | 1 |, b = 0, solve Ax = b.
        byte[][] matrixA = new byte[][]{
            new byte[]{0x01},
        };
        byte[] b0 = new byte[BYTE_L];
        byte[][] b = new byte[][]{
            BytesUtils.clone(b0),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));

        // A = | 1 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000001},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b0, x[0]));

        // A = | 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b0 = new byte[BYTE_L];
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));

        // A = | 0 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testFree1x2() {
        int m = 2;
        // A = | 1 1 |, b = 0, solve Ax = b.
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000011},
        };
        byte[] b0 = new byte[BYTE_L];
        byte[][] b = new byte[][]{
            BytesUtils.clone(b0),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 1 1 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000011},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b0, x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 0 1 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000001},
        };
        b0 = new byte[BYTE_L];
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000001},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isEqual(b0, x[1]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000010},
        };
        b0 = new byte[BYTE_L];
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000010},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b0, x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 0 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b0 = new byte[BYTE_L];
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[0]));

        // A = | 0 0 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][]{
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.freeSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testFull1x2() {
        int m = 2;
        // A = | 1 1 |, b = 0, solve Ax = b.
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000011},
        };
        byte[] b0 = new byte[BYTE_L];
        byte[][] b = new byte[][]{
            BytesUtils.clone(b0),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(gf2e.add(x[0], x[1])));

        // A = | 1 1 |, b = r, solve Ax = b.
        matrixA = new byte[][] {
            new byte[] {0b00000011},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][] {
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isEqual(b0, gf2e.add(x[0], x[1])));

        // A = | 0 1 |, b = 0, solve Ax = b.
        matrixA = new byte[][] {
            new byte[] {0b00000001},
        };
        b0 = new byte[BYTE_L];
        b = new byte[][] {
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        matrixA = new byte[][] {
            new byte[] {0b00000001},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][] {
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isEqual(b0, x[1]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][] {
            new byte[] {0b00000010},
        };
        b0 = new byte[BYTE_L];
        b = new byte[][] {
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        matrixA = new byte[][] {
            new byte[] {0b00000010},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][] {
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b0, x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 0 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][] {
            new byte[] {0b00000000},
        };
        b0 = new byte[BYTE_L];
        b = new byte[][] {
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[0]));

        // A = | 0 0 |, b = r, solve Ax = b.
        matrixA = new byte[][] {
            new byte[] {0b00000000},
        };
        b0 = BytesUtils.randomByteArray(BYTE_L, L, SECURE_RANDOM);
        b = new byte[][] {
            BytesUtils.clone(b0),
        };
        x = new byte[m][];
        systemInfo = linearSolver.fullSolve(matrixA, m, b, x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }
}
