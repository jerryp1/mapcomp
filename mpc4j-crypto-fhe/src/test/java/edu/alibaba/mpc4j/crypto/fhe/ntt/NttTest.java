package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * NTT tests.
 *
 * @author Qixian Zhou, Weiran Liu
 * @date 2023/8/27
 */
public class NttTest {
    /**
     * maximal number of loops in the test
     */
    private static final int MAX_LOOP_NUM = 1000;

    @Test
    public void testNttBasics() {
        int logN = 1;
        Modulus modulus = Numth.getPrime(2 << logN, 60);
        NttTables table = new NttTables(logN, modulus);
        Assert.assertEquals(1 << logN, table.getCoeffCount());
        Assert.assertEquals(logN, table.getCoeffCountPower());

        logN = 2;
        modulus = Numth.getPrime(2 << logN, 50);
        table = new NttTables(logN, modulus);
        Assert.assertEquals(1 << logN, table.getCoeffCount());
        Assert.assertEquals(logN, table.getCoeffCountPower());

        logN = 10;
        modulus = Numth.getPrime(2 << logN, 40);
        table = new NttTables(logN, modulus);
        Assert.assertEquals(1 << logN, table.getCoeffCount());
        Assert.assertEquals(logN, table.getCoeffCountPower());

        Modulus[] modulusArray = CoeffModulus.create(1 << logN, new int[]{20, 20, 20, 20, 20});
        int k = modulusArray.length;
        NttTables[] nttTables = new NttTables[5];
        NttTables.createNttTables(logN, modulusArray, nttTables);
        for (int j = 0; j < k; j++) {
            Assert.assertEquals(1 << logN, nttTables[j].getCoeffCount());
            Assert.assertEquals(logN, nttTables[j].getCoeffCountPower());
        }
    }

    @Test
    public void testNttPrimitiveRoot() {
        int logN = 1;
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        NttTables tables = new NttTables(logN, modulus);
        Assert.assertEquals(288794978602139552L, tables.getRootPowers(1).operand);
        long[] inv = new long[1];
        Numth.tryInvertUintMod(tables.getRootPowers(1).operand, modulus.getValue(), inv);
        Assert.assertEquals(tables.getInvRootPowers(1).operand, inv[0]);

        logN = 2;
        tables = new NttTables(logN, modulus);
        Assert.assertEquals(1, tables.getRootPowers(0).operand);
        Assert.assertEquals(288794978602139552L, tables.getRootPowers(1).operand);
        Assert.assertEquals(178930308976060547L, tables.getRootPowers(2).operand);
        Assert.assertEquals(748001537669050592L, tables.getRootPowers(3).operand);
    }

    @Test
    public void testNttNegacyclicHarvey() {
        int logN = 1;
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        NttTables tables = new NttTables(logN, modulus);
        long[] poly = new long[]{0L, 0L};
        NttTool.nttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{0L, 0L}, poly);
        NttTool.inverseNttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{0L, 0L}, poly);

        poly = new long[]{1L, 0L};
        NttTool.nttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{1, 1}, poly);
        NttTool.inverseNttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{1, 0}, poly);

        poly = new long[]{1L, 1L};
        NttTool.nttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{288794978602139553L, 864126526004445282L}, poly);
        NttTool.inverseNttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{1L, 1L}, poly);
    }

    @Test
    public void testInverseNttNegacyclicHarvey() {
        int logN = 3;
        int n = 800;
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        NttTables tables = new NttTables(logN, modulus);
        long[] poly = new long[n];
        long[] groundTruth = new long[n];
        NttTool.inverseNttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(groundTruth, poly);

        Random random = new Random();
        for (int index = 0; index < n; index++) {
            poly[index] = Math.abs(random.nextLong()) % modulus.getValue();
        }
        System.arraycopy(poly, 0, groundTruth, 0, n);

        NttTool.nttNegacyclicHarvey(poly, tables);
        NttTool.inverseNttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(groundTruth, poly);
    }

    @Test
    public void testRandomNttNegacyclicHarvey() {
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        int[] logNs = new int[]{1, 3, 6, 8, 10, 11, 12, 13, 14, 15};
        Random random = new Random();

        for (int logN : logNs) {
            int n = 1 << logN;
            NttTables tables = new NttTables(logN, modulus);
            long[] poly = new long[n];
            long[] groundTruth = new long[n];
            for (int round = 0; round < MAX_LOOP_NUM; round++) {
                for (int index = 0; index < n; index++) {
                    poly[index] = Math.abs(random.nextLong()) % modulus.getValue();
                }
                System.arraycopy(poly, 0, groundTruth, 0, n);

                NttTool.nttNegacyclicHarvey(poly, 0, tables);
                NttTool.inverseNttNegacyclicHarvey(poly, 0, tables);
                Assert.assertArrayEquals(groundTruth, poly);
            }
        }
    }

    @Test
    public void testRandomNttNegacyclicHarveyRns() {
        Modulus[] modulusArray = Modulus.createModulus(new long[]{0xffffee001L, 0xffffc4001L, 0x1ffffe0001L});
        int k = modulusArray.length;
        int[] logNs = new int[]{12};
        Random random = new Random();

        for (int logN : logNs) {
            NttTables[] nttTables = new NttTables[k];
            int n = 1 << logN;
            NttTables.createNttTables(logN, modulusArray, nttTables);
            long[] rns = new long[n * k];
            long[] groundTruth = new long[n * k];

            for (int round = 0; round < MAX_LOOP_NUM; round++) {
                for (int j = 0; j < k; j++) {
                    int offset = j * n;
                    for (int index = 0; index < n; index++) {
                        rns[offset + index] = Math.abs(random.nextLong()) % modulusArray[j].getValue();
                    }
                    System.arraycopy(rns, 0, groundTruth, 0, n * k);

                    NttTool.nttNegacyclicHarveyRns(rns, n, k, j, nttTables);
                    NttTool.inverseNttNegacyclicHarveyRns(rns, n, k, j, nttTables);
                    Assert.assertArrayEquals(groundTruth, rns);
                }
                System.arraycopy(rns, 0, groundTruth, 0, n * k);

                NttTool.nttNegacyclicHarveyRns(rns, n, k, nttTables);
                NttTool.inverseNttNegacyclicHarveyRns(rns, n, k, nttTables);
                Assert.assertArrayEquals(groundTruth, rns);
            }
        }
    }

    @Test
    public void testRandomNttNegacyclicHarveyPoly() {
        Modulus[] modulusArray = Modulus.createModulus(new long[]{0xffffee001L, 0xffffc4001L, 0x1ffffe0001L});
        int m = 3;
        int k = modulusArray.length;
        int[] logNs = new int[]{12};
        Random random = new Random();

        for (int logN : logNs) {
            NttTables[] nttTables = new NttTables[k];
            int n = 1 << logN;
            NttTables.createNttTables(logN, modulusArray, nttTables);
            long[] poly = new long[m * n * k];
            long[] groundTruth = new long[m * n * k];

            for (int round = 0; round < MAX_LOOP_NUM; round++) {
                for (int i = 0; i < m; i++) {
                    int iOffset = i * k * n;
                    for (int j = 0; j < k; j++) {
                        int jOffset = j * n;
                        for (int index = 0; index < n; index++) {
                            poly[iOffset + jOffset + index] = Math.abs(random.nextLong()) % modulusArray[j].getValue();
                        }
                    }
                    System.arraycopy(poly, 0, groundTruth, 0, m * n * k);

                    NttTool.nttNegacyclicHarveyPoly(poly, m, n, k, i, nttTables);
                    NttTool.inverseNttNegacyclicHarveyPoly(poly, m, n, k, i, nttTables);
                    Assert.assertArrayEquals(groundTruth, poly);
                }
                System.arraycopy(poly, 0, groundTruth, 0, m * n * k);

                NttTool.nttNegacyclicHarveyPoly(poly, m, n, k, nttTables);
                NttTool.inverseNttNegacyclicHarveyPoly(poly, m, n, k, nttTables);
                Assert.assertArrayEquals(groundTruth, poly);
            }
        }
    }
}
