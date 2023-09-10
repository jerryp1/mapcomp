package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Qixian Zhou
 * @date 2023/8/22
 */
public class RnsToolTest {

    private static final int MAX_LOOP_NUM = 1000;


    @Test
    public void initialize() {

        int polyModulusDegree = 32;
        int coeffBaseCount = 4;
        int primeBitCout = 20;
        Modulus plainT = new Modulus(65537);
        // q_i mod 2N = 1
        RnsBase coeffBase = new RnsBase(Numth.getPrimes(polyModulusDegree * 2, primeBitCout, coeffBaseCount));
        // no assert throw
        RnsTool rnsTool = new RnsTool(polyModulusDegree, coeffBase, plainT);

        // Succeeds with 0 plain_modulus (case of CKKS) omitted this test case
//        rnsTool = new RnsTool(polyModulusDegree, coeffBase, 0);

        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsTool(1, coeffBase, plainT));
    }


    @Test
    public void exactSacleAndRound() {
        // This function computes [round(t/q * |input|_q)]_t exactly using the gamma-correction technique.

        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            int polyModulusDegree = 2;
            Modulus plainT = new Modulus(3);
            RnsTool rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[]{5, 7}), plainT);

            RnsIter inIter = new RnsIter(rnsTool.getBaseBsk().getSize(), polyModulusDegree);
            long[] outIter = new long[polyModulusDegree];
            // 0 ---> sacle and round must be 0
            rnsTool.decryptScaleAndRound(inIter, outIter);
            for (long o : outIter) {
                Assert.assertEquals(0, o);
            }
            // baseQSize = 2, {5, 7}, coeff count = 2
            // mod q is zero
            long[][] in = new long[][]{
                    {35, 70},
                    {35, 70}
            };
            RnsIter iter = new RnsIter(in, polyModulusDegree);
            // so result is zero
            rnsTool.decryptScaleAndRound(iter, outIter);
            for (long o : outIter) {
                Assert.assertEquals(0, o);
            }
            // try non-trivial case
            // round([(29 * 7 *3 + 29 * 5 * 3) mod 35] * (3/35)) mod 3 = 2
            // round([(65 * 7 *3 + 65 * 5 * 3) mod 35] * (3/35)) mod 3 = 3 mod 3 = 0
            long[][] in1 = new long[][]{
                    {29, 30 + 35},
                    {29, 30 + 35}
            };
            outIter = new long[polyModulusDegree];
            RnsIter iter1 = new RnsIter(in1, polyModulusDegree);
            // Here 29 will scale and round to 2 and 30 will scale and round to 0.
            // The added 35 should not make a difference.
            rnsTool.decryptScaleAndRound(iter1, outIter);
            Assert.assertEquals(2, outIter[0]);
            Assert.assertEquals(0, outIter[1]);
        }
    }


    @Test
    public void fastBConvMTilde() {

        Modulus plainT = new Modulus(2);
        RnsTool rnsTool;


        // 1-th test
        int polyModulusDegree = 2;
        long[] zeros = new long[polyModulusDegree];
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[] {3}), plainT);

        long[][] in = new long[rnsTool.getBaseQ().getSize()][polyModulusDegree];
        long[][] out = new long[rnsTool.getBaseBskMTilde().getSize()][polyModulusDegree];
        RnsIter inIter = new RnsIter(in, polyModulusDegree);
        RnsIter outIter = new RnsIter(out, polyModulusDegree);

        rnsTool.fastBConvMTilde(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }
        // 2-th test
        in = new long[][] { {1, 2}};
        inIter = new RnsIter(in, polyModulusDegree);

        rnsTool.fastBConvMTilde(inIter, outIter);

        // These are results for fase base conversion for a length-2 array ((m_tilde), (2*m_tilde))
        // before reduction to target base.
        long temp = rnsTool.getMTilde().getValue() % 3;
        long temp2 = (2 * rnsTool.getMTilde().getValue()) % 3;
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(
                    new long[] {
                            temp % rnsTool.getBaseBskMTilde().getBase(i).getValue(),
                            temp2 % rnsTool.getBaseBskMTilde().getBase(i).getValue(),
                    },
                    outIter.getCoeffIter(i)
            );
        }

        // 3-th test
        polyModulusDegree = 2;
        int coeffModulusSize = 2;
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[] {3, 5}), plainT);

        in = new long[coeffModulusSize][polyModulusDegree];
        out = new long[rnsTool.getBaseBskMTilde().getSize()][polyModulusDegree];
        inIter = new RnsIter(in, polyModulusDegree);
        outIter = new RnsIter(out, polyModulusDegree);

        rnsTool.fastBConvMTilde(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }
        // 4-th test
        in = new long[][] { {1, 1}, {2, 2}};
        inIter = new RnsIter(in, polyModulusDegree);

        rnsTool.fastBConvMTilde(inIter, outIter);

        long mTilde = rnsTool.getMTilde().getValue();
        // This is the result of fast base conversion for a length-2 array
        // ((m_tilde, 2*m_tilde), (m_tilde, 2*m_tilde)) before reduction to target base.
        temp = (( 2 * mTilde) % 3) * 5 + ((4 * mTilde) % 5) * 3;
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(
                    new long[] {
                            temp % rnsTool.getBaseBskMTilde().getBase(i).getValue(),
                            temp % rnsTool.getBaseBskMTilde().getBase(i).getValue(),
                    },
                    outIter.getCoeffIter(i)
            );
        }
    }


    @Test
    public void smMrq() {
        // This function assumes the input is in base Bsk U {m_tilde}. If the input is
        // |[c*m_tilde]_q + qu|_m for m in Bsk U {m_tilde}, then the output is c' in Bsk
        // such that c' = c mod q. In other words, this function cancels the extra multiples
        // of q in the Bsk U {m_tilde} representation. The functions works correctly for
        // sufficiently small values of u.

        Modulus plainT = new Modulus(2);
        RnsTool rnsTool;

        // 1-th test
        int polyModulusDegree = 2;
        long[] zeros = new long[polyModulusDegree];
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[] {3}), plainT);

        long[][] in = new long[rnsTool.getBaseBskMTilde().getSize()][polyModulusDegree];
        long[][] out = new long[rnsTool.getBaseBsk().getSize()][polyModulusDegree];

        RnsIter inIter = new RnsIter(in, polyModulusDegree);
        RnsIter outIter = new RnsIter(out, polyModulusDegree);

        rnsTool.smMrq(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }

        // Input base is Bsk U {m_tilde}, in this case consisting of 3 primes.
        // m_tilde is always smaller than the primes in Bsk (SEAL_INTERNAL_MOD_BIT_COUNT (61) bits).
        // Set the length-2 array to have values 1*m_tilde and 2*m_tilde.
        in = new long[][] {
                {rnsTool.getMTilde().getValue(), 2 * rnsTool.getMTilde().getValue()},
                {rnsTool.getMTilde().getValue(), 2 * rnsTool.getMTilde().getValue()},
                {0, 0} // Modulo m_tilde
        };
        inIter = new RnsIter(in, polyModulusDegree);
        // This should simply get rid of the m_tilde factor
        rnsTool.smMrq(inIter, outIter);
        // [1, 2]
        // [1, 2]
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(
                    new long[] {1, 2} ,
                    outIter.getCoeffIter(i)
            );
        }

        in = new long[][] {
                { rnsTool.getBaseQ().getBase(0).getValue(), rnsTool.getBaseQ().getBase(0).getValue()},
                { rnsTool.getBaseQ().getBase(0).getValue(), rnsTool.getBaseQ().getBase(0).getValue()},
                { rnsTool.getBaseQ().getBase(0).getValue(), rnsTool.getBaseQ().getBase(0).getValue() } // Modulo m_tilde
        };
        inIter = new RnsIter(in, polyModulusDegree);

        rnsTool.smMrq(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }


        // 2-th test
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[] {3, 5}), plainT);
        in = new long[rnsTool.getBaseBskMTilde().getSize()][polyModulusDegree];
        out = new long[rnsTool.getBaseBsk().getSize()][polyModulusDegree];

        inIter = new RnsIter(in, polyModulusDegree);
        outIter = new RnsIter(out, polyModulusDegree);

        rnsTool.smMrq(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }
        // Input base is Bsk U {m_tilde}, in this case consisting of 6 primes.
        // m_tilde is always smaller than the primes in Bsk (SEAL_INTERNAL_MOD_BIT_COUNT (61) bits).
        // Set the length-2 array to have values 1*m_tilde and 2*m_tilde.
        in = new long[][] {
                { rnsTool.getMTilde().getValue(), 2 * rnsTool.getMTilde().getValue() },
                { rnsTool.getMTilde().getValue(), 2 * rnsTool.getMTilde().getValue() },
                { rnsTool.getMTilde().getValue(), 2 * rnsTool.getMTilde().getValue() },
                // Modulo m_tilde
                {0, 0}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.smMrq(inIter, outIter);

        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(
                    new long[] {1, 2} ,
                    outIter.getCoeffIter(i)
            );
        }


        // Next add a multiple of q to the input and see if it is reduced properly
        in = new long[][] {
                {15, 30},
                {15, 30},
                {15, 30},
                {15, 30}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.smMrq(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }

        // Now with a multiple of m_tilde + multiple of q
        in = new long[][] {
                { 2 * rnsTool.getMTilde().getValue() + 15,  2 * rnsTool.getMTilde().getValue() + 30 },
                { 2 * rnsTool.getMTilde().getValue() + 15,  2 * rnsTool.getMTilde().getValue() + 30 },
                { 2 * rnsTool.getMTilde().getValue() + 15,  2 * rnsTool.getMTilde().getValue() + 30 },
                { 2 * rnsTool.getMTilde().getValue() + 15,  2 * rnsTool.getMTilde().getValue() + 30 },
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.smMrq(inIter, outIter);
        long[] two = new long[] {2, 2};
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(two, outIter.getCoeffIter(i));
        }
    }


    @Test
    public void fastFloor() {
        // This function assumes the input is in base q U Bsk. It outputs an approximation of
        // the value divided by q floored in base Bsk. The approximation has absolute value up
        // to k-1, where k is the number of primes in the base q.

        Modulus plainT = new Modulus(2);
        RnsTool rnsTool;

        // 1-th test
        int polyModulusDegree = 2;
        long[] zeros = new long[polyModulusDegree];
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[] {3}), plainT);
        int inSize = rnsTool.getBaseQ().getSize() + rnsTool.getBaseBsk().getSize();
        long[][] in = new long[inSize][polyModulusDegree];
        long[][] out = new long[rnsTool.getBaseBsk().getSize()][polyModulusDegree];

        RnsIter inIter = new RnsIter(in, polyModulusDegree);
        RnsIter outIter = new RnsIter(out, polyModulusDegree);

        rnsTool.fastFloor(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }

        // The size of q U Bsk is 3. We set the input to have values 15 and 5, and divide by 3 (i.e., q).
        in = new long[][] {
                {15, 3},
                {15, 3},
                {15, 3},
        };
        inIter = new RnsIter(in, polyModulusDegree);
        // This should simply get rid of the m_tilde factor
        rnsTool.fastFloor(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(
                    new long[] {5, 1} ,
                    outIter.getCoeffIter(i)
            );
        }
        // Now a case where the floor really shows up
        in = new long[][] {
                {17, 4},
                {17, 4},
                {17, 4},
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.fastFloor(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(
                    new long[] {5, 1},
                    outIter.getCoeffIter(i));
        }


        // 2-th
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[] {3, 5}), plainT);
        inSize = rnsTool.getBaseQ().getSize() + rnsTool.getBaseBsk().getSize();
        in = new long[inSize][polyModulusDegree];
        out = new long[rnsTool.getBaseBsk().getSize()][polyModulusDegree];
        inIter = new RnsIter(in, polyModulusDegree);
        outIter = new RnsIter(out, polyModulusDegree);

        rnsTool.fastFloor(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }
        // The size of q U Bsk is now 5. We set the input to multiples of 15 an divide by 15 (i.e., q).
        // 2 + (2 + 1)
        in = new long[][] {
                {15, 30},
                {15, 30},
                {15, 30},
                {15, 30},
                {15, 30},
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.fastFloor(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(
                    new long[] {1, 2},
                    outIter.getCoeffIter(i));
        }


        in = new long[][] {
                {21, 32},
                {21, 32},
                {21, 32},
                {21, 32},
                {21, 32},
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.fastFloor(inIter, outIter);
        // The result is not exact but differs at most by 1
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertTrue(Math.abs(1 - outIter.getCoeffIter(i)[0]) <= 1 );
            Assert.assertTrue(Math.abs(2 - outIter.getCoeffIter(i)[1]) <= 1 );
        }
    }

    @Test
    public void fastBConvSk() {

        Modulus plainT = new Modulus(2);
        RnsTool rnsTool;

        // 1-th test
        int polyModulusDegree = 2;
        long[] zeros = new long[polyModulusDegree];
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[] {3}), plainT);
        int inSize = rnsTool.getBaseBsk().getSize();
        long[][] in = new long[inSize][polyModulusDegree];
        long[][] out = new long[rnsTool.getBaseQ().getSize()][polyModulusDegree];

        RnsIter inIter = new RnsIter(in, polyModulusDegree);
        RnsIter outIter = new RnsIter(out, polyModulusDegree);

        rnsTool.fastBConvSk(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }

        in = new long[][] {
                {1, 2},
                {1, 2}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.fastBConvSk(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(new long[] {1, 2}, outIter.getCoeffIter(i));
        }

        // 2-th test
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[] {3, 5}), plainT);
        inSize = rnsTool.getBaseBsk().getSize();
        in = new long[inSize][polyModulusDegree];
        out = new long[rnsTool.getBaseQ().getSize()][polyModulusDegree];
        inIter = new RnsIter(in, polyModulusDegree);
        outIter = new RnsIter(out, polyModulusDegree);
        rnsTool.fastBConvSk(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(zeros, outIter.getCoeffIter(i));
        }


        in = new long[][] {
                {1, 2},
                {1, 2},
                {1, 2}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        // out will be changed
        out = new long[rnsTool.getBaseQ().getSize()][polyModulusDegree];
        outIter = new RnsIter(out, polyModulusDegree);
        rnsTool.fastBConvSk(inIter, outIter);
        for (int i = 0; i < outIter.getRnsBaseSize(); i++) {
            Assert.assertArrayEquals(new long[] {1, 2}, outIter.getCoeffIter(i));
        }
    }

    @Test
    public void divideAndRoundQLastInplace() {

        // This function approximately divides the input values by the last prime in the base q.
        // Input is in base q; the last RNS component becomes invalid.
        // note that the last RNS component becomes invalid. So in the following test,
        // we drop the last RNS component.

        int polyModulusDegree = 2;
        Modulus plainT = new Modulus(2);
        RnsTool rnsTool;
        long[] zeros = new long[polyModulusDegree];

        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[]{13, 7}), plainT);

        int baseQSize = rnsTool.getBaseQ().getSize();

        long[][] in = new long[baseQSize][polyModulusDegree];
        RnsIter inIter = new RnsIter(in, polyModulusDegree);

        rnsTool.divideAndRoundQLastInplace(inIter);
        Assert.assertArrayEquals(zeros, inIter.getCoeffIter(0));

        // The size of q is 2. We set some values here and divide by the last modulus (i.e., 7).
        in = new long[][] {
                {1, 2},
                {1, 2}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        // We expect to get a zero output also in this case
        rnsTool.divideAndRoundQLastInplace(inIter);
        Assert.assertArrayEquals(zeros, inIter.getCoeffIter(0));

        // Next a case with non-trivial rounding
        in = new long[][] {
                {12, 11},
                {4, 3}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        // We expect to get a zero output also in this case
        rnsTool.divideAndRoundQLastInplace(inIter);
        Assert.assertArrayEquals(new long[] {4, 3}, inIter.getCoeffIter(0));

        // Input array (19, 15)
        in = new long[][] {
                {6, 2},
                {5, 1}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        // We expect to get a zero output also in this case
        rnsTool.divideAndRoundQLastInplace(inIter);
        Assert.assertArrayEquals(new long[] {3, 2}, inIter.getCoeffIter(0));

        // another test
        rnsTool = new RnsTool(polyModulusDegree, new RnsBase(new long[]{3, 5, 7, 11}), plainT);
        in = new long[rnsTool.getBaseQ().getSize()][polyModulusDegree];
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.divideAndRoundQLastInplace(inIter);
        Assert.assertArrayEquals(new long[2], inIter.getCoeffIter(0));
        Assert.assertArrayEquals(new long[2], inIter.getCoeffIter(1));
        Assert.assertArrayEquals(new long[2], inIter.getCoeffIter(2));

        // We expect to get a zero output also in this case
        in = new long[][] {
                {1, 2},
                {1, 2},
                {1, 2},
                {1, 2}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.divideAndRoundQLastInplace(inIter);
        Assert.assertArrayEquals(new long[2], inIter.getCoeffIter(0));
        Assert.assertArrayEquals(new long[2], inIter.getCoeffIter(1));
        Assert.assertArrayEquals(new long[2], inIter.getCoeffIter(2));

        // Next a case with non-trivial rounding; array is (60, 70)
        in = new long[][] {
                {0, 1},
                {0, 0},
                {4, 0},
                {5, 4}
        };
        inIter = new RnsIter(in, polyModulusDegree);
        rnsTool.divideAndRoundQLastInplace(inIter);

        Assert.assertTrue(( 3 + 2 - in[0][0]) % 3 <= 1);
        Assert.assertTrue(( 3 + 0 - in[0][1]) % 3 <= 1);
        Assert.assertTrue(( 5 + 0 - in[1][0]) % 5 <= 1);
        Assert.assertTrue(( 5 + 1 - in[1][1]) % 5 <= 1);
        Assert.assertTrue(( 7 + 5 - in[2][0]) % 7 <= 1);
        Assert.assertTrue(( 7 + 6 - in[2][1]) % 7 <= 1);

    }


}
