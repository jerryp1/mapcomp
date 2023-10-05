package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Qixian Zhou
 * @date 2023/8/27
 */
public class NttTest {


    public static final int MAX_LOOP_NUM = 1000;


    @Test
    public void nttBasics() {

        int coeffCountPower = 1;
        Modulus modulus = Numth.getPrime(2 << coeffCountPower, 60);
        NttTables table = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(2, table.getCoeffCount());
        Assert.assertEquals(1, table.getCoeffCountPower());

        coeffCountPower = 2;
        modulus = Numth.getPrime(2 << coeffCountPower, 50);
        table = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(4, table.getCoeffCount());
        Assert.assertEquals(2, table.getCoeffCountPower());

        coeffCountPower = 10;
        modulus = Numth.getPrime(2 << coeffCountPower, 40);
        table = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(1024, table.getCoeffCount());
        Assert.assertEquals(10, table.getCoeffCountPower());


        CoeffModulus.create(1 << coeffCountPower, new int[]{20, 20, 20, 20, 20});

        NttTables[] nttTables = new NttTables[5];
        NttTablesCreateIter.createNttTables(
                coeffCountPower,
                CoeffModulus.create(1 << coeffCountPower, new int[]{20, 20, 20, 20, 20}),
                nttTables
        );
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(1024, nttTables[i].getCoeffCount());
            Assert.assertEquals(10, nttTables[i].getCoeffCountPower());
        }
    }


    @Test
    public void nttPrimitiveRoot() {

        int coeffCountPower = 1;
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        NttTables tables = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(1, tables.getCoeffCountPower());
        Assert.assertEquals(288794978602139552L, tables.getRootPowers(1).operand);
        long[] inv = new long[1];
        Numth.tryInvertUintMod(tables.getRootPowers(1).operand, modulus.getValue(), inv);
        Assert.assertEquals(tables.getInvRootPowers(1).operand, inv[0]);

        coeffCountPower = 2;
        tables = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(1, tables.getRootPowers(0).operand);
        Assert.assertEquals(288794978602139552L, tables.getRootPowers(1).operand);
        Assert.assertEquals(178930308976060547L, tables.getRootPowers(2).operand);
        Assert.assertEquals(748001537669050592L, tables.getRootPowers(3).operand);
    }


    @Test
    public void nttNegAcyclicHarvey() {

        int coeffCountPower = 1;
        Modulus modulus = new Modulus(0xffffffffffc0001L);

        NttTables tables = new NttTables(coeffCountPower, modulus);
        long[] poly = new long[]{0, 0};
        NttTool.nttNegAcyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[2], poly);

        poly = new long[]{1, 0};
        NttTool.nttNegAcyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{1, 1}, poly);

        poly = new long[]{1, 1};
        NttTool.nttNegAcyclicHarvey(poly, tables);
        Assert.assertArrayEquals(
                new long[]{
                        288794978602139553L,
                        864126526004445282L},
                poly);
    }

    @Test
    public void inverseNttNegAcyclicHarvey() {

        int coeffCountPower = 3;
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        NttTables tables = new NttTables(coeffCountPower, modulus);
        long[] poly = new long[800];
        long[] temp = new long[800];
        NttTool.inverseNttNegAcyclicHarvey(poly, tables);
        Assert.assertArrayEquals(temp, poly);

        Random random = new Random();
        for (int i = 0; i < 800; i++) {
            poly[i] = Math.abs(random.nextLong()) % modulus.getValue();
            temp[i] = poly[i];
        }

        NttTool.nttNegAcyclicHarvey(poly, tables);
        NttTool.inverseNttNegAcyclicHarvey(poly, tables);

        Assert.assertArrayEquals(temp, poly);
    }

    @Test
    public void randomTest() {

        Modulus modulus = new Modulus(0xffffffffffc0001L);
        int[] coeffCountPowers = new int[]{1, 3, 6, 8, 10, 11, 12, 13, 14, 15};
        Random random = new Random();

        for (int coeffCountPower : coeffCountPowers) {
            int n = 1 << coeffCountPower;
            NttTables tables = new NttTables(coeffCountPower, modulus);

            long[] poly = new long[n];
            long[] temp = new long[n];

            for (int i = 0; i < MAX_LOOP_NUM; i++) {
                for (int j = 0; j < n; j++) {
                    poly[j] = Math.abs(random.nextLong()) % modulus.getValue();
                    temp[j] = poly[j];
                }
                NttTool.nttNegAcyclicHarvey(poly,0,  tables);
                NttTool.inverseNttNegAcyclicHarvey(poly,0,  tables);

                Assert.assertArrayEquals(temp, poly);
            }
        }
    }

    @Test
    public void randomRnsIterTest() {


        Modulus[] mod = Modulus.createModulus(new long[]{0xffffee001L, 0xffffc4001L, 0x1ffffe0001L});


        // N = 4096
        int[] coeffCountPowers = new int[]{12};
        Random random = new Random();


        for (int coeffCountPower : coeffCountPowers) {

            NttTables[] nttTables = new NttTables[mod.length];

            int n = 1 << coeffCountPower;
            NttTablesCreateIter.createNttTables(coeffCountPower, mod, nttTables);

            long[] poly = new long[n * mod.length];
            long[] groundTruth = new long[n * mod.length];

            for (int i = 0; i < MAX_LOOP_NUM; i++) {

                for (int j = 0; j < mod.length; j++) {
                    for (int k = 0; k < n; k++) {
                        poly[j * n + k] = Math.abs(random.nextLong()) % mod[j].getValue();
                    }
                }

                System.arraycopy(poly, 0, groundTruth, 0, n * mod.length);

                RnsIter rnsIter = new RnsIter(poly, n);


                NttTool.nttNegAcyclicHarvey(rnsIter, mod.length, nttTables);
                NttTool.inverseNttNegAcyclicHarvey(rnsIter, mod.length, nttTables);

                Assert.assertArrayEquals(groundTruth, poly);
            }
        }
    }

    @Test
    public void randomRnsIterTest2() {


        Modulus[] mod = Modulus.createModulus(new long[]{0xffffee001L, 0xffffc4001L, 0x1ffffe0001L});


        // N = 4096
        int[] coeffCountPowers = new int[]{12};
        Random random = new Random();


        for (int coeffCountPower : coeffCountPowers) {

            NttTables[] nttTables = new NttTables[mod.length];

            int n = 1 << coeffCountPower;
            NttTablesCreateIter.createNttTables(coeffCountPower, mod, nttTables);

            long[] poly = new long[n * mod.length];
            long[] groundTruth = new long[n * mod.length];

            for (int i = 0; i < MAX_LOOP_NUM; i++) {

                for (int j = 0; j < mod.length; j++) {
                    for (int k = 0; k < n; k++) {
                        poly[j * n + k] = Math.abs(random.nextLong()) % mod[j].getValue();
                    }
                }

                System.arraycopy(poly, 0, groundTruth, 0, n * mod.length);

                NttTool.nttNegAcyclicHarveyRnsIter(poly, n, mod.length, nttTables);
                NttTool.inverseNttNegAcyclicHarveyRnsIter(poly, n, mod.length, nttTables);

                Assert.assertArrayEquals(groundTruth, poly);
            }
        }
    }


}
