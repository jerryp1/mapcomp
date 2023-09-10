package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.iterator.PolyIter;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/20
 */
public class PolyArithmeticSmallModTest {


    @Test
    public void multiplyPolyScalarCoeffMod() {

        long[] poly = new long[]{1, 3, 4};
        long scalar = 3;
        Modulus mod = new Modulus(5);
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(poly, 3, scalar, mod, poly);
        Assert.assertArrayEquals(new long[]{3, 4, 2}, poly);

        long[][] poly1 = new long[][]{
                {1, 3, 4},
                {1, 0, 2},
        };
        RnsIter rnsIter = new RnsIter(poly1, 3);
        scalar = 2;
        Modulus[] mods = new Modulus[]{new Modulus(5), new Modulus(3)};
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(rnsIter, 2, scalar, mods, rnsIter);
        Assert.assertArrayEquals(new long[]{2, 1, 3}, rnsIter.getCoeffIter(0));
        Assert.assertArrayEquals(new long[]{2, 0, 1}, rnsIter.getCoeffIter(1));


        poly1 = new long[][]{
                {1, 3, 4},
                {1, 0, 2},
        };
        long[][] poly2 = new long[][]{
                {1, 3, 4},
                {1, 0, 2},
        };
        RnsIter[] rnsIters = new RnsIter[]{
                new RnsIter(poly1, 3),
                new RnsIter(poly2, 3),
        };
        PolyIter polyIter = new PolyIter(rnsIters, 3, 2);
        scalar = 2;
        mods = new Modulus[]{new Modulus(5), new Modulus(3)};
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(polyIter, 2, scalar, mods, polyIter);

        poly1 = new long[][]{
                {2, 1, 3},
                {2, 0, 1},
        };
        poly2 = new long[][]{
                {2, 1, 3},
                {2, 0, 1},
        };
        rnsIters = new RnsIter[]{
                new RnsIter(poly1, 3),
                new RnsIter(poly2, 3),
        };
        PolyIter polyIter1 = new PolyIter(rnsIters, 3, 2);
        Assert.assertEquals(polyIter, polyIter1);
    }


    @Test
    public void moduloPolyCoeffs() {

        long[] poly = new long[]{2, 15, 77};
        Modulus mod = new Modulus(15);
        PolyArithmeticSmallMod.moduloPolyCoeffs(poly, 3, mod, poly);
        Assert.assertArrayEquals(new long[]{2, 0, 2}, poly);


        long[][] poly1 = new long[][]{
                {2, 15, 77},
                {2, 15, 77},
        };
        RnsIter poly1Iter = new RnsIter(poly1, 3);
        Modulus[] mods = new Modulus[]{new Modulus(15), new Modulus(3)};
        PolyArithmeticSmallMod.moduloPolyCoeffs(poly1Iter, 2, mods, poly1Iter);
        // 2 mod 15 = 2 , 15 mod 15 = 0, 77 mod 15 = 2
        Assert.assertArrayEquals(new long[]{2, 0, 2}, poly1[0]);
        // 2 mod 3 = 2, 15 mod 3 = 0. 77 mod 3 = 2
        Assert.assertArrayEquals(new long[]{2, 0, 2}, poly1[1]);

        poly1 = new long[][]{
                {2, 15, 77},
                {2, 15, 77},
        };
        long[][] poly2 = new long[][]{
                {2, 15, 77},
                {2, 15, 77},
        };
        poly1Iter = new RnsIter(poly1, 3);
        RnsIter poly2Iter = new RnsIter(poly2, 3);

        PolyIter polyIter = new PolyIter(new RnsIter[]{poly1Iter, poly2Iter}, 3, 2);

        PolyArithmeticSmallMod.moduloPolyCoeffs(polyIter, 2, mods, polyIter);
        for (int i = 0; i < polyIter.getSize(); i++) {
            for (int j = 0; j < polyIter.getRnsIter(i).getRnsBaseSize(); j++) {
                Assert.assertArrayEquals(new long[]{2, 0, 2}, polyIter.getRnsIter(i).getCoeffIter(j));
            }
        }
    }


    @Test
    public void negatePolyCoeffMod() {

        {
            long[] poly1 = new long[]{2, 3, 4};
            long[] result = new long[3];

            Modulus mod = new Modulus(15);
            PolyArithmeticSmallMod.negatePolyCoeffMod(poly1, 3, mod, result);
            Assert.assertArrayEquals(new long[]{13, 12, 11}, result);


            mod = new Modulus(0xFFFFFFFFFFFFFFL);
            PolyArithmeticSmallMod.negatePolyCoeffMod(poly1, 3, mod, result);
            Assert.assertArrayEquals(
                    new long[]{
                            0xFFFFFFFFFFFFFDL,
                            0xFFFFFFFFFFFFFCL,
                            0xFFFFFFFFFFFFFBL},
                    result);
        }
        {
            long[][] poly1 = new long[][]{
                    {2, 3, 4},
                    {2, 0, 1}
            };
            RnsIter poly1Iter = new RnsIter(poly1, 3);

            Modulus[] mod = Modulus.createModulus(new long[]{15, 3});

            long[][] result = new long[2][3];
            RnsIter resultIter = new RnsIter(result, 3);

            PolyArithmeticSmallMod.negatePolyCoeffMod(poly1Iter, 2, mod, resultIter);

            Assert.assertArrayEquals(new long[]{13, 12, 11}, result[0]);
            Assert.assertArrayEquals(new long[]{1, 0, 2}, result[1]);
        }
        {
            Modulus[] mod = Modulus.createModulus(new long[]{15, 3});

            long[][] a = new long[][]{
                    {2, 3, 4},
                    {2, 0, 1}
            };
            long[][] b = new long[][]{
                    {2, 3, 4},
                    {2, 0, 1}
            };

            RnsIter[] iters1 = new RnsIter[]{new RnsIter(a, 3), new RnsIter(b, 3)};
            PolyIter polyIter1 = new PolyIter(iters1, 3, 2);

            PolyIter resultIter = PolyIter.createEmpty(2, 3, 2);
            PolyArithmeticSmallMod.negatePolyCoeffMod(polyIter1, 2, mod, resultIter);


            for (int i = 0; i < resultIter.getSize(); i++) {

                Assert.assertArrayEquals(
                        new long[][]{
                                {13, 12, 11},
                                {1, 0, 2},
                        },
                        resultIter.getRnsIter(i).getCoeffIter()
                );
            }
        }
    }

    @Test
    public void addPolyCoeffMod() {

        long[] poly1 = new long[]{1, 3, 4};
        long[] poly2 = new long[]{1, 2, 4};

        Modulus mod = new Modulus(5);
        PolyArithmeticSmallMod.addPolyCoeffMod(poly1, poly2, 3, mod, poly1);
        Assert.assertArrayEquals(new long[]{2, 0, 3}, poly1);


        RnsIter polyIter1 = new RnsIter(
                new long[][]{
                        {1, 3, 4},
                        {0, 1, 2}
                },
                3
        );
        RnsIter polyIter2 = new RnsIter(
                new long[][]{
                        {1, 2, 4},
                        {2, 1, 0}
                },
                3
        );

        Modulus[] mods = new Modulus[]{new Modulus(5), new Modulus(3)};
        PolyArithmeticSmallMod.addPolyCoeffMod(polyIter1, polyIter2, 2, mods, polyIter1);

        Assert.assertArrayEquals(new long[]{2, 0, 3}, polyIter1.getCoeffIter(0));
        Assert.assertArrayEquals(new long[]{2, 2, 2}, polyIter1.getCoeffIter(1));


        RnsIter a = new RnsIter(
                new long[][]{
                        {1, 3, 4},
                        {0, 1, 2}
                },
                3
        );
        RnsIter b = new RnsIter(
                new long[][]{
                        {2, 4, 0},
                        {1, 2, 0}
                },
                3
        );
        PolyIter p1 = new PolyIter(new RnsIter[]{a, b}, 3, 2);

        RnsIter c = new RnsIter(
                new long[][]{
                        {1, 2, 4},
                        {2, 1, 0}
                },
                3
        );
        RnsIter d = new RnsIter(
                new long[][]{
                        {2, 4, 0},
                        {0, 2, 1}
                },
                3
        );
        PolyIter p2 = new PolyIter(new RnsIter[]{c, d}, 3, 2);


        PolyArithmeticSmallMod.addPolyCoeffMod(p1, p2, 2, mods, p1);

        Assert.assertArrayEquals(new long[]{2, 0, 3}, p1.getRnsIter(0).getCoeffIter(0));
        Assert.assertArrayEquals(new long[]{2, 2, 2}, p1.getRnsIter(0).getCoeffIter(1));
        Assert.assertArrayEquals(new long[]{4, 3, 0}, p1.getRnsIter(1).getCoeffIter(0));
        Assert.assertArrayEquals(new long[]{1, 1, 1}, p1.getRnsIter(1).getCoeffIter(1));

    }

    @Test
    public void subPolyCoeffMod() {

        long[] poly1 = new long[]{4, 3, 2};
        long[] poly2 = new long[]{2, 3, 4};

        Modulus mod = new Modulus(5);
        PolyArithmeticSmallMod.subPolyCoeffMod(poly1, poly2, 3, mod, poly1);
        Assert.assertArrayEquals(new long[]{2, 0, 3}, poly1);


        RnsIter polyIter1 = new RnsIter(
                new long[][]{
                        {1, 3, 4},
                        {0, 1, 2}
                },
                3
        );
        RnsIter polyIter2 = new RnsIter(
                new long[][]{
                        {1, 2, 4},
                        {2, 1, 0}
                },
                3
        );

        Modulus[] mods = new Modulus[]{new Modulus(5), new Modulus(3)};
        PolyArithmeticSmallMod.subPolyCoeffMod(polyIter1, polyIter2, 2, mods, polyIter1);

        Assert.assertArrayEquals(new long[]{0, 1, 0}, polyIter1.getCoeffIter(0));
        Assert.assertArrayEquals(new long[]{1, 0, 2}, polyIter1.getCoeffIter(1));


        RnsIter a = new RnsIter(
                new long[][]{
                        {1, 3, 4},
                        {0, 1, 2}
                },
                3
        );
        RnsIter b = new RnsIter(
                new long[][]{
                        {2, 4, 0},
                        {1, 2, 0}
                },
                3
        );
        PolyIter p1 = new PolyIter(new RnsIter[]{a, b}, 3, 2);

        RnsIter c = new RnsIter(
                new long[][]{
                        {1, 2, 4},
                        {2, 1, 0}
                },
                3
        );
        RnsIter d = new RnsIter(
                new long[][]{
                        {2, 4, 0},
                        {0, 2, 1}
                },
                3
        );
        PolyIter p2 = new PolyIter(new RnsIter[]{c, d}, 3, 2);


        PolyArithmeticSmallMod.subPolyCoeffMod(p1, p2, 2, mods, p1);

        Assert.assertArrayEquals(new long[]{0, 1, 0}, p1.getRnsIter(0).getCoeffIter(0));
        Assert.assertArrayEquals(new long[]{1, 0, 2}, p1.getRnsIter(0).getCoeffIter(1));
        Assert.assertArrayEquals(new long[]{0, 0, 0}, p1.getRnsIter(1).getCoeffIter(0));
        Assert.assertArrayEquals(new long[]{1, 0, 2}, p1.getRnsIter(1).getCoeffIter(1));

    }

    @Test
    public void dyadicProductCoeffMod() {
        {
            long[] poly1 = new long[]{1, 1, 1};
            long[] poly2 = new long[]{2, 3, 4};
            long[] result = new long[3];

            Modulus mod = new Modulus(13);
            PolyArithmeticSmallMod.dyadicProductCoeffMod(poly1, poly2, 3, mod, result);
            Assert.assertArrayEquals(new long[]{2, 3, 4}, result);
        }

        {
            long[][] poly1 = new long[][]{
                    {1, 2, 1},
                    {2, 1, 2}
            };
            long[][] poly2 = new long[][]{
                    {2, 3, 4},
                    {2, 3, 4}
            };

            RnsIter poly1Iter = new RnsIter(poly1, 3);
            RnsIter poly2Iter = new RnsIter(poly2, 3);
            Modulus[] mod = Modulus.createModulus(new long[]{13, 7});

            long[][] result = new long[2][3];
            RnsIter resultIter = new RnsIter(result, 3);

            PolyArithmeticSmallMod.dyadicProductCoeffMod(poly1Iter, poly2Iter, 2, mod, resultIter);

            Assert.assertArrayEquals(new long[]{2, 6, 4}, result[0]);
            Assert.assertArrayEquals(new long[]{4, 3, 1}, result[1]);
        }
        {
            Modulus[] mod = Modulus.createModulus(new long[]{13, 7});

            long[][] a = new long[][]{
                    {1, 2, 1},
                    {2, 1, 2}
            };
            long[][] b = new long[][]{
                    {1, 2, 1},
                    {2, 1, 2}
            };

            RnsIter[] iters1 = new RnsIter[]{new RnsIter(a, 3), new RnsIter(b, 3)};

            PolyIter polyIter1 = new PolyIter(iters1, 3, 2);

            long[][] c = new long[][]{
                    {2, 3, 4},
                    {2, 3, 4}
            };
            long[][] d = new long[][]{
                    {2, 3, 4},
                    {2, 3, 4}
            };
            RnsIter[] iters2 = new RnsIter[]{new RnsIter(c, 3), new RnsIter(d, 3)};
            PolyIter polyIter2 = new PolyIter(iters2, 3, 2);

            PolyIter resultIter = PolyIter.createEmpty(2, 3, 2);
            PolyArithmeticSmallMod.dyadicProductCoeffMod(polyIter1, polyIter2, 2, mod, resultIter);


            for (int i = 0; i < resultIter.getSize(); i++) {

                Assert.assertArrayEquals(
                        new long[][]{
                                {2, 6, 4},
                                {4, 3, 1},
                        },
                        resultIter.getRnsIter(i).getCoeffIter()
                );
            }
        }
    }

    @Test
    public void polyInftyNormCoeffMod() {

        long[] poly = new long[]{0, 1, 2, 3};
        Modulus mod = new Modulus(10);
        Assert.assertEquals(3, PolyArithmeticSmallMod.polyInftyNormCoeffMod(poly, 4, mod));

        // (10 + 1)/2 = 5 is the first neg
        // 10 - 8 = 2
        poly = new long[]{0, 1, 2, 8};
        Assert.assertEquals(2, PolyArithmeticSmallMod.polyInftyNormCoeffMod(poly, 4, mod));
    }


    @Test
    public void negAcyclicShiftPolyCoeffMod() {

        {
            long[] poly1 = new long[4];
            long[] result = new long[4];
            long[] zeros = new long[4];

            Modulus mod = new Modulus(10);

            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 4, 0, mod, result);
            Assert.assertArrayEquals(zeros, result);
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 4, 1, mod, result);
            Assert.assertArrayEquals(zeros, result);
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 4, 2, mod, result);
            Assert.assertArrayEquals(zeros, result);
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 4, 3, mod, result);
            Assert.assertArrayEquals(zeros, result);

            // 1 + 2x + 3x^2 + 4x^3
            poly1 = new long[]{1, 2, 3, 4};
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 4, 0, mod, result);
            Assert.assertArrayEquals(new long[]{1, 2, 3, 4}, result);
            // 1 + 2x + 3x^2 + 4x^3 ----> 1 ----> (-4) mod 10 + x + 2x^2 + 3x^3
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 4, 1, mod, result);
            Assert.assertArrayEquals(new long[]{6, 1, 2, 3}, result);
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 4, 2, mod, result);
            Assert.assertArrayEquals(new long[]{7, 6, 1, 2}, result);
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 4, 3, mod, result);
            Assert.assertArrayEquals(new long[]{8, 7, 6, 1}, result);


            poly1 = new long[]{1, 2, 3, 4};
            // 1 + 2x + 3x^2 + 4x^3
            // 1 + 2x ---> 1   ---> 8 + x --> [8, 1]
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1, 2, 1, mod, result);
            Assert.assertEquals(result[0], 8);
            Assert.assertEquals(result[1], 1);

            long[] poly2 = new long[2];
            System.arraycopy(poly1, 2, poly2, 0, 2);
            // 3 + 4x ---> 1 ---> 6 + 3x
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly2, 2, 1, mod, result);
            Assert.assertEquals(result[0], 6);
            Assert.assertEquals(result[1], 3);
        }
        {
            long[][] poly1 = new long[][]{
                    {1, 2, 3, 4},
                    {1, 2, 3, 4},
            };

            RnsIter poly1Iter = new RnsIter(poly1, 4);
            Modulus[] mod = Modulus.createModulus(new long[]{10, 11});

            long[][] result = new long[2][4];
            RnsIter resultIter = new RnsIter(result, 4);

            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1Iter, 2, 0, mod, resultIter);
            Assert.assertArrayEquals(poly1[0], result[0]);
            Assert.assertArrayEquals(poly1[1], result[1]);

            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1Iter, 2, 1, mod, resultIter);
            Assert.assertArrayEquals(new long[]{6, 1, 2, 3}, result[0]);
            Assert.assertArrayEquals(new long[]{7, 1, 2, 3}, result[1]);


            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1Iter, 2, 2, mod, resultIter);
            Assert.assertArrayEquals(new long[]{7, 6, 1, 2}, result[0]);
            Assert.assertArrayEquals(new long[]{8, 7, 1, 2}, result[1]);

            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(poly1Iter, 2, 3, mod, resultIter);
            Assert.assertArrayEquals(new long[]{8, 7, 6, 1}, result[0]);
            Assert.assertArrayEquals(new long[]{9, 8, 7, 1}, result[1]);
        }
        {
            Modulus[] mod = Modulus.createModulus(new long[]{10, 11});

            long[][] a = new long[][]{
                    {1, 2, 3, 4},
                    {1, 2, 3, 4},
            };
            long[][] b = new long[][]{
                    {1, 2, 3, 4},
                    {1, 2, 3, 4},
            };

            RnsIter[] iters1 = new RnsIter[]{new RnsIter(a, 4), new RnsIter(b, 4)};
            PolyIter polyIter1 = new PolyIter(iters1, 4, 2);
            PolyIter resultIter = PolyIter.createEmpty(2, 4, 2);
            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(polyIter1, 2, 0, mod, resultIter);
            for (int i = 0; i < resultIter.getSize(); i++) {
                for (int j = 0; j < resultIter.getRnsIter(i).getCoeffModulusSize(); j++) {
                    Assert.assertArrayEquals(new long[]{1, 2, 3, 4}, resultIter.getRnsIter(i).getCoeffIter(j));
                }
            }

            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(polyIter1, 2, 1, mod, resultIter);
            for (int i = 0; i < resultIter.getSize(); i++) {
                Assert.assertArrayEquals(
                        new long[][]{
                                {6, 1, 2, 3},
                                {7, 1, 2, 3}
                        },
                        resultIter.getRnsIter(i).getCoeffIter()
                );
            }

            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(polyIter1, 2, 2, mod, resultIter);
            for (int i = 0; i < resultIter.getSize(); i++) {
                Assert.assertArrayEquals(
                        new long[][]{
                                {7, 6, 1, 2},
                                {8, 7, 1, 2}
                        },
                        resultIter.getRnsIter(i).getCoeffIter()
                );
            }

            PolyArithmeticSmallMod.negAcyclicShiftPolyCoeffMod(polyIter1, 2, 3, mod, resultIter);
            for (int i = 0; i < resultIter.getSize(); i++) {
                Assert.assertArrayEquals(
                        new long[][]{
                                {8, 7, 6, 1},
                                {9, 8, 7, 1}
                        },
                        resultIter.getRnsIter(i).getCoeffIter()
                );
            }
        }

    }

    @Test
    public void negAcyclicMultiplyPolyMonoCoeffMod() {

        {
            long[] poly = new long[]{1, 3, 4, 2};
            long monoCoeff = 3;
            long[] result = new long[4];
            Modulus mod = new Modulus(5);

            // 3 x^0
            int monoExponent = 0;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 1, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(new long[]{3, 0, 0, 0}, result);

            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 2, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(new long[]{3, 4, 0, 0}, result);

            monoExponent = 1;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 2, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(new long[]{1, 3, 0, 0}, result);

            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 4, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(new long[]{4, 3, 4, 2}, result);

            monoCoeff = 1;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 4, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(new long[]{3, 1, 3, 4}, result);

            monoCoeff = 4;
            monoExponent = 3;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 4, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(new long[]{3, 4, 2, 4}, result);

            monoCoeff = 1;
            monoExponent = 0;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 4, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(new long[]{1, 3, 4, 2}, result);
        }
        {
            long[][] poly1 = new long[][]{
                    {1, 3, 4, 2},
                    {1, 3, 4, 2},
            };

            RnsIter poly1Iter = new RnsIter(poly1, 4);
            Modulus[] mod = Modulus.createModulus(new long[]{5, 7});

            long[][] result = new long[2][4];
            RnsIter resultIter = new RnsIter(result, 4);

            long monoCoeff = 4;
            int monoExponent = 2;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly1Iter, 2, monoCoeff, monoExponent, mod, resultIter);
            Assert.assertArrayEquals(new long[]{4, 2, 4, 2}, result[0]);
            Assert.assertArrayEquals(new long[]{5, 6, 4, 5}, result[1]);
        }
        {
            Modulus[] mod = Modulus.createModulus(new long[]{5, 7});

            long[][] a = new long[][]{
                    {1, 3, 4, 2},
                    {1, 3, 4, 2},
            };
            long[][] b = new long[][]{
                    {1, 3, 4, 2},
                    {1, 3, 4, 2},
            };

            RnsIter[] iters1 = new RnsIter[]{new RnsIter(a, 4), new RnsIter(b, 4)};
            PolyIter polyIter1 = new PolyIter(iters1, 4, 2);
            PolyIter resultIter = PolyIter.createEmpty(2, 4, 2);

            long monoCoeff = 4;
            int monoExponent = 2;

            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(polyIter1, 2, monoCoeff, monoExponent, mod, resultIter);

            Assert.assertArrayEquals(new long[]{4, 2, 4, 2}, resultIter.getRnsIter(0).getCoeffIter(0));
            Assert.assertArrayEquals(new long[]{5, 6, 4, 5}, resultIter.getRnsIter(0).getCoeffIter(1));
            Assert.assertArrayEquals(new long[]{4, 2, 4, 2}, resultIter.getRnsIter(1).getCoeffIter(0));
            Assert.assertArrayEquals(new long[]{5, 6, 4, 5}, resultIter.getRnsIter(1).getCoeffIter(1));
        }
    }
}
