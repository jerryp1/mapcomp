package edu.alibaba.mpc4j.crypto.matrix.okve.tool;

import cc.redberry.rings.linear.LinearSolver;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * binary linear solver test with constant inputs.
 *
 * @author Weiran Liu
 * @date 2023/6/17
 */
public class BinaryConstantLinearSolverTest {
    /**
     * l
     */
    private static final int L = CommonConstants.STATS_BIT_LENGTH;
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
    private final BinaryLinearSolver linearSolver;

    public BinaryConstantLinearSolverTest() {
        gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, L);
        linearSolver = new BinaryLinearSolver(L);
    }

    @Test
    public void test1x1() {
        int m = 1;
        // A = | 1 |, b = 0, solve Ax = b.
        byte[][] matrixA = new byte[][]{
            new byte[]{0x01},
        };
        byte[][] b = new byte[][]{
            gf2e.createZero(),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));

        // A = | 1 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000001},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));

        // A = | 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));

        // A = | 0 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void test1x2() {
        int m = 2;
        // A = | 1 1 |, b = 0, solve Ax = b.
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000011},
        };
        byte[][] b = new byte[][]{
            gf2e.createZero(),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(gf2e.add(x[0], x[1])));

        // A = | 1 1 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000011},
        };
        b = new byte[][]{
            gf2e.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isEqual(b[0], gf2e.add(x[0], x[1])));

        // A = | 0 1 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000001},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000001},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isEqual(b[0], x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isEqual(b[0], x[1]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000010},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000010},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 0 0 |, b = 0, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b = new byte[][]{
            gf2e.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[0]));

        // A = | 0 0 |, b = r, solve Ax = b.
        matrixA = new byte[][]{
            new byte[]{0b00000000},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testAllOne2x2() {
        int m = 2;
        // A = | 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 |      | 0 |
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000011},
            new byte[]{0b00000011},
        };
        byte[][] b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isEqual(b[0], gf2e.add(x[0], x[1])));

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | 0  |
        matrixA = new byte[][]{
            new byte[]{0b00000011},
            new byte[]{0b00000011},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 |      | r1 |
        matrixA = new byte[][]{
            new byte[]{0b00000011},
            new byte[]{0b00000011},
        };
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | r1 |
        matrixA = new byte[][]{
            new byte[]{0b00000011},
            new byte[]{0b00000011},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!gf2e.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
            systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = BytesUtils.clone(b[0]);
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isEqual(b[0], gf2e.add(x[0], x[1])));
    }

    @Test
    public void testAllZero2x2() {
        int m = 2;
        // A = | 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 |      | 0 |
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000000},
            new byte[]{0b00000000},
        };
        byte[][] b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | 0  |
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x2() {
        int m = 2;
        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000010},
            new byte[]{0b00000001},
        };
        byte[][] b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo;
        if (!gf2e.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
            Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
            Assert.assertTrue(gf2e.isEqual(b[1], x[1]));
            systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
            Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
            Assert.assertTrue(gf2e.isEqual(b[1], x[1]));
        }
        b[1] = BytesUtils.clone(b[0]);
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
        Assert.assertTrue(gf2e.isEqual(b[1], x[1]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
        Assert.assertTrue(gf2e.isEqual(b[1], x[1]));

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1 |
        matrixA = new byte[][]{
            new byte[]{0b00000000},
            new byte[]{0b00000010},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        matrixA = new byte[][]{
            new byte[]{0b00000001},
            new byte[]{0b00000010},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[1]));
        Assert.assertTrue(gf2e.isEqual(b[1], x[0]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[1]));
        Assert.assertTrue(gf2e.isEqual(b[1], x[0]));
    }

    @Test
    public void testAllOne2x3() {
        int m = 3;
        // A = | 1 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 1 |      | 0 |
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000111},
            new byte[]{0b00000111},
        };
        byte[][] b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertFalse(gf2e.isZero(x[2]));
        Assert.assertTrue(gf2e.isEqual(b[0], gf2e.add(gf2e.add(x[0], x[1]), x[2])));

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | 0  |
        matrixA = new byte[][]{
            new byte[]{0b00000111},
            new byte[]{0b00000111},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        matrixA = new byte[][]{
            new byte[]{0b00000111},
            new byte[]{0b00000111},
        };
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        matrixA = new byte[][]{
            new byte[]{0b000000111},
            new byte[]{0b000000111},
        };
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!gf2e.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
            systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = BytesUtils.clone(b[0]);
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[0], x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertFalse(gf2e.isZero(x[2]));
        Assert.assertTrue(gf2e.isEqual(b[0], gf2e.add(gf2e.add(x[0], x[1]), x[2])));
    }

    @Test
    public void testAllZero3x2() {
        int m = 3;
        // A = | 0 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 0 |      | 0 |
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000000},
            new byte[]{0b00000000},
        };
        byte[][] b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertFalse(gf2e.isZero(x[2]));

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | 0  |
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createZero(),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new byte[][]{
            gf2e.createNonZeroRandom(SECURE_RANDOM),
            gf2e.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x3() {
        int m = 3;
        // A = | 0 0 1 |, b = | 0 |, solve Ax = b.
        //     | 1 0 0 |      | 0 |
        byte[][] matrixA = new byte[][]{
            new byte[]{0b00000001},
            new byte[]{0b00000100},
        };
        byte[][] b = new byte[][]{
            gf2e.createZero(),
            gf2e.createZero(),
        };
        byte[][] x = new byte[m][];
        LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isZero(x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));

        // A = | 0 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 0 |      | r1 |
        matrixA = new byte[][]{
            new byte[]{0b00000001},
            new byte[]{0b00000100},
        };
        b = new byte[][]{
            gf2e.createRandom(SECURE_RANDOM),
            gf2e.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[1], x[0]));
        Assert.assertTrue(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isEqual(b[0], x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(b[1], x[0]));
        Assert.assertFalse(gf2e.isZero(x[1]));
        Assert.assertTrue(gf2e.isEqual(b[0], x[2]));

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 0 |      | r1 |
        matrixA = new byte[][]{
            new byte[]{0b00000011},
            new byte[]{0b00000110},
        };
        b = new byte[][]{
            gf2e.createRandom(SECURE_RANDOM),
            gf2e.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(gf2e.add(x[1], x[2]), b[0]));
        Assert.assertTrue(gf2e.isEqual(gf2e.add(x[0], x[1]), b[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(gf2e.add(x[1], x[2]), b[0]));
        Assert.assertTrue(gf2e.isEqual(gf2e.add(x[0], x[1]), b[1]));
        Assert.assertFalse(gf2e.isZero(x[2]));

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        matrixA = new byte[][]{
            new byte[]{0b00000011},
            new byte[]{0b00000111},
        };
        b = new byte[][]{
            gf2e.createRandom(SECURE_RANDOM),
            gf2e.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(gf2e.add(x[1], x[2]), b[0]));
        Assert.assertTrue(gf2e.isEqual(gf2e.add(gf2e.add(x[0], x[1]), x[2]), b[1]));
        Assert.assertTrue(gf2e.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BytesUtils.clone(matrixA), m, BytesUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(gf2e.isEqual(gf2e.add(x[1], x[2]), b[0]));
        Assert.assertTrue(gf2e.isEqual(gf2e.add(gf2e.add(x[0], x[1]), x[2]), b[1]));
        Assert.assertFalse(gf2e.isZero(x[2]));
    }
}
