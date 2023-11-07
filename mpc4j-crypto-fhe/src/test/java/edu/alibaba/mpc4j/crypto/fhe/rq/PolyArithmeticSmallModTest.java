package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import org.junit.Assert;
import org.junit.Test;

/**
 * polynomial arithmetic operations for small modulus test.
 *
 * @author Qixian Zhou
 * @date 2023/8/20
 */
public class PolyArithmeticSmallModTest {

    @Test
    public void testModuloPolyCoeffs() {
        // Coeff representation
        {
            int n = 3;
            Modulus modulus = new Modulus(15);
            long[] coeff = new long[]{2, 15, 77};
            PolyArithmeticSmallMod.moduloPolyCoeff(coeff, n, modulus, coeff);
            Assert.assertArrayEquals(new long[]{2, 0, 2}, coeff);
        }
        // RNS representation
        {
            int n = 3;
            int k = 2;
            long[][] data = new long[][]{
                {2, 15, 77},
                {2, 15, 77}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] rns = RnsIterator.createRnsFrom2dArray(data);
            PolyArithmeticSmallMod.moduloPolyCoeffRns(rns, n, k, modulus, rns, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 0, 2},
                    {2, 0, 2}},
                RnsIterator.rnsTo2dArray(rns, n, k)
            );
        }
        // Poly-RNS representation
        {
            int m = 2;
            int n = 3;
            int k = 2;
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
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] poly = PolyIterator.createPolyFrom3dArray(data);
            PolyArithmeticSmallMod.moduloPolyCoeffPoly(poly, n, k, m, modulus, poly, n, k);
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
                PolyIterator.polyTo3dArray(poly, m, n, k)
            );
        }
    }

    @Test
    public void testNegatePolyCoeffs() {
        // Coeff representation
        {
            int n = 3;
            Modulus modulus = new Modulus(15);
            long[] coeff = new long[]{2, 3, 4};
            PolyArithmeticSmallMod.negatePolyCoeffMod(coeff, n, modulus, coeff);
            Assert.assertArrayEquals(new long[]{13, 12, 11}, coeff);

            coeff = new long[]{2, 3, 4};
            modulus = new Modulus(0xFFFFFFFFFFFFFFL);
            PolyArithmeticSmallMod.negatePolyCoeffMod(coeff, 3, modulus, coeff);
            Assert.assertArrayEquals(new long[]{0xFFFFFFFFFFFFFDL, 0xFFFFFFFFFFFFFCL, 0xFFFFFFFFFFFFFBL}, coeff);
        }
        // RNS representation
        {
            int n = 3;
            int k = 2;
            long[][] data = new long[][]{
                {2, 3, 4},
                {2, 0, 1}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] rns = RnsIterator.createRnsFrom2dArray(data);
            PolyArithmeticSmallMod.negatePolyCoeffModRns(rns, n, k, modulus, rns, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {13, 12, 11},
                    {1, 0, 2}},
                RnsIterator.rnsTo2dArray(rns, n, k)
            );
        }
        // Poly-RNS representation
        {
            int m = 2;
            int n = 3;
            int k = 2;
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
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] poly = PolyIterator.createPolyFrom3dArray(data);
            PolyArithmeticSmallMod.negatePolyCoeffModPoly(poly, n, k, m, modulus, poly, n, k);
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
                PolyIterator.polyTo3dArray(poly, m, n, k)
            );
        }
    }

    @Test
    public void testAddPolyCoeffMod() {
        // Coeff representation
        {
            int n = 3;
            long[] coeff1 = new long[]{1, 3, 4};
            long[] coeff2 = new long[]{1, 2, 4};
            Modulus modulus = new Modulus(5);
            PolyArithmeticSmallMod.addPolyCoeffMod(coeff1, coeff2, n, modulus, coeff1);
            Assert.assertArrayEquals(new long[]{2, 0, 3}, coeff1);
        }
        // RNS representation
        {
            int n = 3;
            int k = 2;
            long[][] data1 = new long[][]{
                {1, 3, 4},
                {0, 1, 2}
            };
            long[][] data2 = new long[][]{
                {1, 2, 4},
                {2, 1, 0}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] rns1 = RnsIterator.createRnsFrom2dArray(data1);
            long[] rns2 = RnsIterator.createRnsFrom2dArray(data2);
            PolyArithmeticSmallMod.addPolyCoeffModRns(rns1, n, k, rns2, n, k, modulus, rns1, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 0, 3},
                    {2, 2, 2}
                },
                RnsIterator.rnsTo2dArray(rns1, n, k)
            );
        }
        // Poly-RNS representation
        {
            int m = 2;
            int n = 3;
            int k = 2;
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
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] poly1 = PolyIterator.createPolyFrom3dArray(data1);
            long[] poly2 = PolyIterator.createPolyFrom3dArray(data2);
            PolyArithmeticSmallMod.addPolyCoeffModPoly(poly1, n, k, poly2, n, k, m, modulus, poly1, n, k);
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
                PolyIterator.polyTo3dArray(poly1, m, n, k)
            );
        }
    }

    @Test
    public void testSubPolyCoeffMod() {
        // Coeff representation
        {
            int n = 3;
            long[] coeff1 = new long[]{4, 3, 2};
            long[] coeff2 = new long[]{2, 3, 4};
            Modulus modulus = new Modulus(5);
            PolyArithmeticSmallMod.subPolyCoeffMod(coeff1, coeff2, n, modulus, coeff1);
            Assert.assertArrayEquals(new long[]{2, 0, 3}, coeff1);
        }
        // RNS representation
        {
            int n = 3;
            int k = 2;
            long[][] data1 = new long[][]{
                {1, 3, 4},
                {0, 1, 2}
            };
            long[][] data2 = new long[][]{
                {1, 2, 4},
                {2, 1, 0}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] rns1 = RnsIterator.createRnsFrom2dArray(data1);
            long[] rns2 = RnsIterator.createRnsFrom2dArray(data2);
            PolyArithmeticSmallMod.subPolyCoeffModRns(rns1, n, k, rns2, n, k, modulus, rns1, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {0, 1, 0},
                    {1, 0, 2}
                },
                RnsIterator.rnsTo2dArray(rns1, n, k)
            );
        }
        // Poly-RNS representation
        {
            int m = 2;
            int n = 3;
            int k = 2;
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
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] poly1 = PolyIterator.createPolyFrom3dArray(data1);
            long[] poly2 = PolyIterator.createPolyFrom3dArray(data2);
            PolyArithmeticSmallMod.subPolyCoeffModPoly(poly1, n, k, poly2, n, k, m, modulus, poly1, n, k);
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
                PolyIterator.polyTo3dArray(poly1, m, n, k)
            );
        }
    }

    @Test
    public void testMultiplyPolyScalarCoeffMod() {
        // Coeff representation
        {
            int n = 3;
            long[] coeff = new long[]{1, 3, 4};
            Modulus modulus = new Modulus(5);
            long scalar = 3;

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(coeff, n, scalar, modulus, coeff);
            Assert.assertArrayEquals(new long[]{3, 4, 2}, coeff);
        }
        // RNS representation
        {
            int n = 3;
            int k = 2;
            long[][] data = new long[][]{
                {1, 3, 4},
                {1, 0, 2}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] rns = RnsIterator.createRnsFrom2dArray(data);
            long scalar = 2;
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(rns, n, k,scalar, modulus, rns, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 1, 3},
                    {2, 0, 1}
                },
                RnsIterator.rnsTo2dArray(rns, n, k)
            );
        }
        {
            int m = 2;
            int n = 3;
            int k = 2;
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
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] poly = PolyIterator.createPolyFrom3dArray(data);
            long scalar = 2;
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(poly, n, k, m, scalar, modulus, poly, n, k);
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
                PolyIterator.polyTo3dArray(poly, m, n, k)
            );
        }
    }

    @Test
    public void testDyadicProductCoeffMod() {
        // Coeff representation
        {
            int n = 3;
            long[] coeff1 = new long[]{1, 1, 1};
            long[] coeff2 = new long[]{2, 3, 4};
            long[] coeffR = new long[n];
            Modulus modulus = new Modulus(13);
            PolyArithmeticSmallMod.dyadicProductCoeffMod(coeff1, coeff2, n, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{2, 3, 4}, coeffR);
        }
        // RNS representation
        {
            int n = 3;
            int k = 2;
            long[][] data1 = new long[][]{
                {1, 2, 1},
                {2, 1, 2}
            };
            long[][] data2 = new long[][]{
                {2, 3, 4},
                {2, 3, 4}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{13, 7});
            long[] rns1 = RnsIterator.createRnsFrom2dArray(data1);
            long[] rns2 = RnsIterator.createRnsFrom2dArray(data2);
            long[] rnsR = RnsIterator.createRnsFromZero(n, k);
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(rns1, n, k, rns2, n, k, modulus, rnsR, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 6, 4},
                    {4, 3, 1}
                },
                RnsIterator.rnsTo2dArray(rnsR, n, k)
            );
        }
        // Poly-RNS representation
        {
            int m = 2;
            int n = 3;
            int k = 2;
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
            Modulus[] modulus = Modulus.createModulus(new long[]{13, 7});
            long[] poly1 = PolyIterator.createPolyFrom3dArray(data1);
            long[] poly2 = PolyIterator.createPolyFrom3dArray(data2);
            long[] polyR = PolyIterator.createPolyFromZero(m, n, k);
            PolyArithmeticSmallMod.dyadicProductCoeffModPoly(poly1, n, k, poly2, n, k, m, modulus, polyR, n, k);
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
                PolyIterator.polyTo3dArray(polyR, m, n, k)
            );
        }
    }

    @Test
    public void testNegacyclicShiftPolyCoeffMod() {
        // Coeff representation
        {
            int n = 4;
            long[] coeff = new long[n];
            long[] coeffR = new long[n];
            Modulus modulus = new Modulus(10);

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, n, 0, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, n, 1, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, n, 2, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, n, 3, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffR);

            coeff = new long[]{1, 2, 3, 4};
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, n, 0, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{1, 2, 3, 4}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, n, 1, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{6, 1, 2, 3}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, n, 2, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{7, 6, 1, 2}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, n, 3, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{8, 7, 6, 1}, coeffR);

            coeff = new long[]{1, 2, 3, 4};
            // coeff1 = 1 + 2x
            int n1 = 2;
            long[] coeff1 = new long[n1];
            coeffR = new long[n1];
            System.arraycopy(coeff, 0, coeff1, 0, n1);
            // shift 1, 1 + 2x -> x + 2x^2 --> (-2 mod 10) + x -> [8, 1]
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff1, n1, 1, modulus, coeffR);
            Assert.assertArrayEquals(new long[] {8, 1}, coeffR);
            // coeff2 = 3 + 4x
            int n2 = 2;
            long[] coeff2 = new long[n2];
            coeffR = new long[n2];
            System.arraycopy(coeff, 2, coeff2, 0, n2);
            // shift 1, 3 + 4x -> 3x + 4x^2 --> (-4 mod 10) + 3x -> [6, 3]
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff2, n2, 1, modulus, coeffR);
            Assert.assertArrayEquals(new long[] {6, 3}, coeffR);
        }
        // RNS representation
        {
            int n = 4;
            int k = 2;
            long[][] data = new long[][]{
                {1, 2, 3, 4},
                {1, 2, 3, 4}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{10, 11});
            long[] rns = RnsIterator.createRnsFrom2dArray(data);
            long[] rnsR = RnsIterator.createRnsFromZero(n, k);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rns, n, k, 0, modulus, rnsR, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {1, 2, 3, 4},
                    {1, 2, 3, 4}
                },
                RnsIterator.rnsTo2dArray(rnsR, n, k)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rns, n, k, 1, modulus, rnsR, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {6, 1, 2, 3},
                    {7, 1, 2, 3}
                },
                RnsIterator.rnsTo2dArray(rnsR, n, k)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rns, n, k, 2, modulus, rnsR, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {7, 6, 1, 2},
                    {8, 7, 1, 2}
                },
                RnsIterator.rnsTo2dArray(rnsR, n, k)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rns, n, k, 3, modulus, rnsR, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {8, 7, 6, 1},
                    {9, 8, 7, 1}
                },
                RnsIterator.rnsTo2dArray(rnsR, n, k)
            );
        }
        // Poly-RNS representation
        {
            int m = 2;
            int n = 4;
            int k = 2;
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
            Modulus[] modulus = Modulus.createModulus(new long[]{10, 11});
            long[] poly = PolyIterator.createPolyFrom3dArray(data);
            long[] polyR = PolyIterator.createPolyFromZero(m, n, k);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(poly, n, k, m, 0, modulus, polyR, n, k);
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
                PolyIterator.polyTo3dArray(polyR, m, n, k)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(poly, n, k, m, 1, modulus, polyR, n, k);
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
                PolyIterator.polyTo3dArray(polyR, m, n, k)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(poly, n, k, m, 2, modulus, polyR, n, k);
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
                PolyIterator.polyTo3dArray(polyR, m, n, k)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(poly, n, k, m, 3, modulus, polyR, n, k);
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
                PolyIterator.polyTo3dArray(polyR, m, n, k)
            );
        }
    }

    @Test
    public void multiplyPolyMonoCoeffMod() {
        // Coeff representation.
        {
            int n = 4;
            long[] coeff = new long[]{1, 3, 4, 2};
            Modulus modulus = new Modulus(5);
            long[] result = new long[n];
            long monoCoeff = 3;

            int monoExponent = 0;
            // n = 1, 1 * 3 mod 5 = 3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 1, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{3, 0, 0, 0}, result);
            // n = 2, (1 + 3x) * 3 mod 5 = 3 + 4x
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 2, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{3, 4, 0, 0}, result);

            monoExponent = 1;
            // n = 2, (1 + 3x) * 3x mod 5 = (3x - 9) mod 5 = 1 + 3x
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 2, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{1, 3, 0, 0}, result);
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (3x) = (3x + 9x^2 + 12x^3 - 6) mod 5 = 4 + 3x + 3x^2 + 2x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 4, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{4, 3, 4, 2}, result);

            monoCoeff = 1;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (x) mod 5 = (x + 3x^2 + 4x^3 - 2) mod 5 = 3 + x + 3x^2 + 4x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 4, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{3, 1, 3, 4}, result);

            monoCoeff = 4;
            monoExponent = 3;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (4x^3) mod 5 = (4x^3 - 12 - 16x - 8x^2) mod 5 = 3 + 4x + 2x^2 + 4x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 4, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{3, 4, 2, 4}, result);

            monoCoeff = 1;
            monoExponent = 0;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (1) mod 5 = 1 + 3x + 4x^2 + 2x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 4, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{1, 3, 4, 2}, result);
        }
        // RNS representation
        {
            int n = 4;
            int k = 2;
            long[][] data = new long[][]{
                {1, 3, 4, 2},
                {1, 3, 4, 2}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 7});
            long[] rns = RnsIterator.createRnsFrom2dArray(data);
            long[] rnsR = RnsIterator.createRnsFromZero(n, k);

            long monoCoeff = 4;
            int monoExponent = 2;
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModRns(rns, n, k, monoCoeff, monoExponent, modulus, rnsR, n, k);
            Assert.assertArrayEquals(
                new long[][]{
                    {4, 2, 4, 2},
                    {5, 6, 4, 5}
                },
                RnsIterator.rnsTo2dArray(rnsR, n, k)
            );
        }
        // Poly-RNS representation
        {
            int m = 2;
            int n = 4;
            int k = 2;
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
            Modulus[] modolus = Modulus.createModulus(new long[]{5, 7});
            long[] poly = PolyIterator.createPolyFrom3dArray(data);
            long[] polyR = PolyIterator.createPolyFromZero(m, n, k);
            long monoCoeff = 4;
            int monoExponent = 2;
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(poly, n, k, m, monoCoeff, monoExponent, modolus, polyR, n, k);
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
                PolyIterator.polyTo3dArray(polyR, m, n, k)
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
}
