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
    public void moduloPolyCoeffs() {

        {
            Modulus mod = new Modulus(15);
            long[] poly = new long[]{2, 15, 77};

            PolyArithmeticSmallMod.moduloPolyCoeff(poly, 3, mod, poly);
            Assert.assertArrayEquals(new long[]{2, 0, 2}, poly);
        }

        {
            long[][] data = new long[][]{
                {2, 15, 77},
                {2, 15, 77}
            };
            RnsIter poly = RnsIter.from2dArray(data);
            Modulus[] mod = Modulus.createModulus(new long[]{15, 3});
            PolyArithmeticSmallMod.moduloPolyCoeff(poly, 2, mod, poly);

            Assert.assertArrayEquals(
                new long[][]{
                    {2, 0, 2},
                    {2, 0, 2}},
                RnsIter.to2dArray(poly)
            );
        }

        {
            long[][][] data = new long[][][]{
                {
                    {2, 15, 77},
                    {2, 15, 77},
                },
                {
                    {2, 15, 77},
                    {2, 15, 77},
                }
            };
            PolyIter poly = PolyIter.from3dArray(data);
            Modulus[] mod = Modulus.createModulus(new long[]{15, 3});
            PolyArithmeticSmallMod.moduloPolyCoeff(poly, 2, mod, poly);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {2, 0, 2},
                        {2, 0, 2},
                    },
                    {
                        {2, 0, 2},
                        {2, 0, 2},
                    }
                },
                PolyIter.to3dArray(poly)
            );

        }
    }


    @Test
    public void negatePolyCoeffs() {

        {
            Modulus mod = new Modulus(15);
            long[] poly = new long[]{2, 3, 4};

            PolyArithmeticSmallMod.negatePolyCoeffMod(poly, 3, mod, poly);
            Assert.assertArrayEquals(new long[]{13, 12, 11}, poly);

            poly = new long[]{2, 3, 4};
            mod = new Modulus(0xFFFFFFFFFFFFFFL);
            PolyArithmeticSmallMod.negatePolyCoeffMod(poly, 3, mod, poly);
            Assert.assertArrayEquals(
                new long[]{0xFFFFFFFFFFFFFDL,
                    0xFFFFFFFFFFFFFCL,
                    0xFFFFFFFFFFFFFBL}
                ,
                poly
            );
        }
        {

            long[][] data = new long[][]{
                {2, 3, 4},
                {2, 0, 1}
            };
            RnsIter poly = RnsIter.from2dArray(data);
            Modulus[] mod = Modulus.createModulus(new long[]{15, 3});
            PolyArithmeticSmallMod.negatePolyCoeffMod(poly, 2, mod, poly);

            Assert.assertArrayEquals(
                new long[][]{
                    {13, 12, 11},
                    {1, 0, 2}},
                RnsIter.to2dArray(poly)
            );
        }

        {
            long[][][] data = new long[][][]{
                {
                    {2, 3, 4},
                    {2, 0, 1}
                },
                {
                    {2, 3, 4},
                    {2, 0, 1}
                }
            };
            PolyIter poly = PolyIter.from3dArray(data);
            Modulus[] mod = Modulus.createModulus(new long[]{15, 3});
            PolyArithmeticSmallMod.negatePolyCoeffMod(poly, 2, mod, poly);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {13, 12, 11},
                        {1, 0, 2}
                    },
                    {
                        {13, 12, 11},
                        {1, 0, 2}
                    }
                },
                PolyIter.to3dArray(poly)
            );
        }
    }

    @Test
    public void addPolyCoeffMod() {

        {
            long[] poly1 = new long[]{1, 3, 4};
            long[] poly2 = new long[]{1, 2, 4};

            Modulus mod = new Modulus(5);
            PolyArithmeticSmallMod.addPolyCoeffMod(poly1, poly2, 3, mod, poly1);
            Assert.assertArrayEquals(
                new long[]{2, 0, 3},
                poly1
            );
        }
        {

            long[][] data1 = new long[][]{
                {1, 3, 4},
                {0, 1, 2}
            };
            long[][] data2 = new long[][]{
                {1, 2, 4},
                {2, 1, 0}
            };

            RnsIter poly1 = RnsIter.from2dArray(data1);
            RnsIter poly2 = RnsIter.from2dArray(data2);

            Modulus[] mod = Modulus.createModulus(new long[]{5, 3});
            PolyArithmeticSmallMod.addPolyCoeffMod(poly1, poly2, 2, mod, poly1);

            Assert.assertArrayEquals(
                new long[][]{
                    {2, 0, 3},
                    {2, 2, 2}
                },
                RnsIter.to2dArray(poly1)
            );
        }
        {
            long[][][] data1 = new long[][][]{
                {
                    {1, 3, 4},
                    {0, 1, 2}
                },
                {
                    {2, 4, 0},
                    {1, 2, 0}
                }
            };
            long[][][] data2 = new long[][][]{
                {
                    {1, 2, 4},
                    {2, 1, 0}
                },
                {
                    {2, 4, 0},
                    {0, 2, 1}
                }
            };

            PolyIter poly1 = PolyIter.from3dArray(data1);
            PolyIter poly2 = PolyIter.from3dArray(data2);
            Modulus[] mod = Modulus.createModulus(new long[]{5, 3});

            PolyArithmeticSmallMod.addPolyCoeffMod(poly1, poly2, 2, mod, poly1);

            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {2, 0, 3},
                        {2, 2, 2}
                    },
                    {
                        {4, 3, 0},
                        {1, 1, 1}
                    }
                },
                PolyIter.to3dArray(poly1)
            );
        }
    }


    @Test
    public void subPolyCoeffMod() {

        {
            long[] poly1 = new long[]{4, 3, 2};
            long[] poly2 = new long[]{2, 3, 4};

            Modulus mod = new Modulus(5);
            PolyArithmeticSmallMod.subPolyCoeffMod(poly1, poly2, 3, mod, poly1);
            Assert.assertArrayEquals(
                new long[]{2, 0, 3},
                poly1
            );
        }
        {

            long[][] data1 = new long[][]{
                {1, 3, 4},
                {0, 1, 2}
            };
            long[][] data2 = new long[][]{
                {1, 2, 4},
                {2, 1, 0}
            };

            RnsIter poly1 = RnsIter.from2dArray(data1);
            RnsIter poly2 = RnsIter.from2dArray(data2);

            Modulus[] mod = Modulus.createModulus(new long[]{5, 3});
            PolyArithmeticSmallMod.subPolyCoeffMod(poly1, poly2, 2, mod, poly1);

            Assert.assertArrayEquals(
                new long[][]{
                    {0, 1, 0},
                    {1, 0, 2}
                },
                RnsIter.to2dArray(poly1)
            );
        }
        {
            long[][][] data1 = new long[][][]{
                {
                    {1, 3, 4},
                    {0, 1, 2}
                },
                {
                    {2, 4, 0},
                    {1, 2, 0}
                }
            };
            long[][][] data2 = new long[][][]{
                {
                    {1, 2, 4},
                    {2, 1, 0}
                },
                {
                    {2, 4, 0},
                    {0, 2, 1}
                }
            };

            PolyIter poly1 = PolyIter.from3dArray(data1);
            PolyIter poly2 = PolyIter.from3dArray(data2);
            Modulus[] mod = Modulus.createModulus(new long[]{5, 3});

            PolyArithmeticSmallMod.subPolyCoeffMod(poly1, poly2, 2, mod, poly1);

            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {0, 1, 0},
                        {1, 0, 2}
                    },
                    {
                        {0, 0, 0},
                        {1, 0, 2}
                    }
                },
                PolyIter.to3dArray(poly1)
            );
        }
    }

    @Test
    public void multiplyPolyScalarCoeffMod() {

        {
            long[] poly = new long[]{1, 3, 4};
            long scalar = 3;
            Modulus mod = new Modulus(5);

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(poly, 3, scalar, mod, poly);
            Assert.assertArrayEquals(
                new long[]{3, 4, 2},
                poly
            );
        }
        {

            long[][] data = new long[][]{
                {1, 3, 4},
                {1, 0, 2}
            };

            RnsIter poly = RnsIter.from2dArray(data);
            long scalar = 2;


            Modulus[] mod = Modulus.createModulus(new long[]{5, 3});
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(poly, 2, scalar, mod, poly);

            Assert.assertArrayEquals(
                new long[][]{
                    {2, 1, 3},
                    {2, 0, 1}
                },
                RnsIter.to2dArray(poly)
            );
        }
        {
            long[][][] data = new long[][][]{
                {
                    {1, 3, 4},
                    {1, 0, 2}
                },
                {
                    {1, 3, 4},
                    {1, 0, 2}
                }
            };

            PolyIter poly = PolyIter.from3dArray(data);
            Modulus[] mod = Modulus.createModulus(new long[]{5, 3});
            long scalar = 2;

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(poly, 2, scalar, mod, poly);

            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {2, 1, 3},
                        {2, 0, 1}
                    },
                    {
                        {2, 1, 3},
                        {2, 0, 1}
                    }
                },
                PolyIter.to3dArray(poly)
            );
        }
    }

    @Test
    public void multiplyPolyMonoCoeffMod() {

        {
            long[] poly = new long[]{1, 3, 4, 2};
            long monoCoeff = 3;
            Modulus mod = new Modulus(5);
            long[] result = new long[4];

            int monoExponent = 0;
            // poly * 3 mod 5
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 1, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(
                new long[]{3, 0, 0, 0},
                result
            );


            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 2, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(
                new long[]{3, 4, 0, 0},
                result
            );

            monoExponent = 1;

            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 2, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(
                new long[]{1, 3, 0, 0},
                result
            );

            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 4, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(
                new long[]{4, 3, 4, 2},
                result
            );

            monoCoeff = 1;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 4, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(
                new long[]{3, 1, 3, 4},
                result
            );

            monoCoeff = 4;
            monoExponent = 3;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 4, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(
                new long[]{3, 4, 2, 4},
                result
            );

            monoCoeff = 1;
            monoExponent = 0;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 4, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(
                new long[]{1, 3, 4, 2},
                result
            );
        }
        {
            long[][] data = new long[][]{
                {1, 3, 4, 2},
                {1, 3, 4, 2}
            };

            RnsIter poly = RnsIter.from2dArray(data);
            RnsIter result = new RnsIter(2, 4);
            Modulus[] mod = Modulus.createModulus(new long[]{5, 7});

            long monoCoeff = 4;
            int monoExponent = 2;
            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 2, monoCoeff, monoExponent, mod, result);
            Assert.assertArrayEquals(
                new long[][]{
                    {4, 2, 4, 2},
                    {5, 6, 4, 5}
                },
                RnsIter.to2dArray(result)
            );
        }

        {
            long[][][] data = new long[][][]{
                {
                    {1, 3, 4, 2},
                    {1, 3, 4, 2},
                },
                {
                    {1, 3, 4, 2},
                    {1, 3, 4, 2},
                }
            };

            PolyIter poly = PolyIter.from3dArray(data);
            PolyIter result = new PolyIter(2, 2, 4);
            Modulus[] mod = Modulus.createModulus(new long[]{5, 7});
            long monoCoeff = 4;
            int monoExponent = 2;

            PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffMod(poly, 2, monoCoeff, monoExponent, mod, result);

            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {4, 2, 4, 2},
                        {5, 6, 4, 5}
                    },
                    {
                        {4, 2, 4, 2},
                        {5, 6, 4, 5}
                    }
                },
                PolyIter.to3dArray(result)
            );
        }
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
            long[][] data1 = new long[][]{
                {1, 2, 1},
                {2, 1, 2}
            };
            long[][] data2 = new long[][]{
                {2, 3, 4},
                {2, 3, 4}
            };

            RnsIter poly1 = RnsIter.from2dArray(data1);
            RnsIter poly2 = RnsIter.from2dArray(data2);
            RnsIter result = new RnsIter(2, 3);
            Modulus[] mod = Modulus.createModulus(new long[]{13, 7});

            PolyArithmeticSmallMod.dyadicProductCoeffMod(poly1, poly2, 2, mod, result);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 6, 4},
                    {4, 3, 1}
                },
                RnsIter.to2dArray(result)
            );


            long[] data11 = new long[]{1, 2, 1, 2, 1, 2};
            long[] data21 = new long[]{2, 3, 4, 2, 3, 4};
            long[] result1 = new long[6];

            for (int i = 0; i < 2; i++) {
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    data11,
                    i * 3,
                    data21,
                    i * 3,
                    3,
                    mod[i],
                    result1,
                    i * 3
                );

            }
            Assert.assertArrayEquals(new long[]{2, 6, 4, 4, 3, 1}, result1);


            Arrays.fill(result1, 0);
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(data11, 0, 3, 2, data21, 0, 3, 2, mod, result1, 0, 3, 2);

            Assert.assertArrayEquals(new long[]{2, 6, 4, 4, 3, 1}, result1);

        }


        {
            long[][][] data1 = new long[][][]{
                {
                    {1, 2, 1},
                    {2, 1, 2}
                },
                {
                    {1, 2, 1},
                    {2, 1, 2}
                }
            };
            long[][][] data2 = new long[][][]{
                {
                    {2, 3, 4},
                    {2, 3, 4}
                },
                {
                    {2, 3, 4},
                    {2, 3, 4}
                }
            };

            PolyIter poly1 = PolyIter.from3dArray(data1);
            PolyIter poly2 = PolyIter.from3dArray(data2);
            PolyIter result = new PolyIter(2, 2, 3);

            Modulus[] mod = Modulus.createModulus(new long[]{13, 7});
            PolyArithmeticSmallMod.dyadicProductCoeffMod(poly1, poly2, 2, mod, result);
            Assert.assertArrayEquals(

                new long[][][]{
                    {
                        {2, 6, 4},
                        {4, 3, 1}
                    },
                    {
                        {2, 6, 4},
                        {4, 3, 1}
                    }
                },
                PolyIter.to3dArray(result)
            );
        }
    }

    @Test
    public void polyInftyNormCoeffMod() {

        long[] poly = new long[]{0, 1, 2, 3};
        Modulus mod = new Modulus(10);

        Assert.assertEquals(3,
            PolyArithmeticSmallMod.polyInftyNormCoeffMod(poly, 4, mod)
        );

        poly = new long[]{0, 1, 2, 8};
        Assert.assertEquals(2,
            PolyArithmeticSmallMod.polyInftyNormCoeffMod(poly, 4, mod)
        );
    }


    @Test
    public void negacyclicShiftPolyCoeffMod() {

        {
            long[] poly = new long[4];
            long[] result = new long[4];

            Modulus mod = new Modulus(10);

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 4, 0, mod, result);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, result);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 4, 1, mod, result);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, result);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 4, 2, mod, result);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, result);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 4, 3, mod, result);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, result);


            poly = new long[]{1, 2, 3, 4};
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 4, 0, mod, result);
            Assert.assertArrayEquals(new long[]{1, 2, 3, 4}, result);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 4, 1, mod, result);
            Assert.assertArrayEquals(new long[]{6, 1, 2, 3}, result);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 4, 2, mod, result);
            Assert.assertArrayEquals(new long[]{7, 6, 1, 2}, result);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 4, 3, mod, result);
            Assert.assertArrayEquals(new long[]{8, 7, 6, 1}, result);


            poly = new long[]{1, 2, 3, 4};
            // 1 + 2x + 3x^2 + 4x^3
            // 1 + 2x ---> 1   ---> (-2 mod 10) + x --> [8, 1]
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 1, mod, result);
            Assert.assertEquals(result[0], 8);
            Assert.assertEquals(result[1], 1);
            long[] poly1 = new long[2];
            System.arraycopy(poly, 2, poly1, 0, 2);
            // 3 + 4x ---> 1 ---> (-4 mod 10) + 3x
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly1, 2, 1, mod, result);
            Assert.assertEquals(result[0], 6);
            Assert.assertEquals(result[1], 3);
        }
        {

            long[][] data = new long[][]{
                {1, 2, 3, 4},
                {1, 2, 3, 4}
            };
            Modulus[] mod = Modulus.createModulus(new long[]{10, 11});
            RnsIter poly = RnsIter.from2dArray(data);
            RnsIter result = new RnsIter(2, 4);

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 0, mod, result);
            Assert.assertArrayEquals(
                new long[][]{
                    {1, 2, 3, 4},
                    {1, 2, 3, 4}
                },
                RnsIter.to2dArray(result)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 1, mod, result);
            Assert.assertArrayEquals(
                new long[][]{
                    {6, 1, 2, 3},
                    {7, 1, 2, 3}
                },
                RnsIter.to2dArray(result)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 2, mod, result);
            Assert.assertArrayEquals(
                new long[][]{
                    {7, 6, 1, 2},
                    {8, 7, 1, 2}
                },
                RnsIter.to2dArray(result)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 3, mod, result);
            Assert.assertArrayEquals(
                new long[][]{
                    {8, 7, 6, 1},
                    {9, 8, 7, 1}
                },
                RnsIter.to2dArray(result)
            );
        }
        {

            Modulus[] mod = Modulus.createModulus(new long[]{10, 11});
            long[][][] data = new long[][][]{
                {
                    {1, 2, 3, 4},
                    {1, 2, 3, 4}
                },
                {
                    {1, 2, 3, 4},
                    {1, 2, 3, 4}
                }
            };
            PolyIter poly = PolyIter.from3dArray(data);
            PolyIter result = new PolyIter(2, 2, 4);

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 0, mod, result);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {1, 2, 3, 4},
                        {1, 2, 3, 4}
                    },
                    {
                        {1, 2, 3, 4},
                        {1, 2, 3, 4}
                    }
                },
                PolyIter.to3dArray(result)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 1, mod, result);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {6, 1, 2, 3},
                        {7, 1, 2, 3}
                    },
                    {
                        {6, 1, 2, 3},
                        {7, 1, 2, 3}
                    }
                },
                PolyIter.to3dArray(result)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 2, mod, result);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {7, 6, 1, 2},
                        {8, 7, 1, 2}
                    },
                    {
                        {7, 6, 1, 2},
                        {8, 7, 1, 2}
                    }
                },
                PolyIter.to3dArray(result)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(poly, 2, 3, mod, result);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {8, 7, 6, 1},
                        {9, 8, 7, 1}
                    },
                    {
                        {8, 7, 6, 1},
                        {9, 8, 7, 1}
                    }
                },
                PolyIter.to3dArray(result)
            );
        }

    }


}
