package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.iterator.PolyIter;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * This class provides modular arithmetic for polynomials.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/polyarithsmallmod.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/20
 */
public class PolyArithmeticSmallMod {
    /**
     * Mods the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void moduloPolyCoeffs(long[] coeff, int n, Modulus modulus, long[] coeffR) {
        moduloPolyCoeffs(coeff, 0, n, modulus, coeffR, 0);
    }

    /**
     * Mods the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param posR    the result start position.
     * @param result  the result Coeff representation.
     */
    public static void moduloPolyCoeffs(long[] coeff, int pos, int n, Modulus modulus, long[] result, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            result[posR + i] = UintArithmeticSmallMod.barrettReduce64(coeff[pos + i], modulus);
        }
    }

    public static void moduloPolyCoeffs(RnsIter poly, int coeffModulusSize, Modulus[] modulus, RnsIter result) {
        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = poly.getPolyModulusDegree();
        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            for (int j = 0; j < polyModulusDegree; j++) {
                result.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.barrettReduce64(poly.coeffIter[i * polyModulusDegree + j], modulus[i]);
            }
        }
    }

    public static void moduloPolyCoeffs(PolyIter polyArray, int size, Modulus[] modulus, PolyIter result) {
        assert size > 0;
        assert polyArray.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = polyArray.getCoeffModulusSize();
        for (int i = 0; i < size; i++) {
            moduloPolyCoeffs(polyArray.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
        }
    }

    /**
     * Adds two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param coeff2  the 2nd Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param result  the result Coeff representation.
     */
    public static void addPolyCoeffMod(long[] coeff1, long[] coeff2, int n, Modulus modulus, long[] result) {
        addPolyCoeffMod(coeff1, 0, coeff2, 0, n, modulus, result, 0);
    }

    /**
     * Adds two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param pos1    the 1st start position.
     * @param coeff2  the 2nd Coeff representation.
     * @param pos2    the 2nd start position.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void addPolyCoeffMod(long[] coeff1, int pos1, long[] coeff2, int pos2,
                                       int n, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long sum;
        for (int i = 0; i < n; i++) {
            assert coeff1[pos1 + i] < modulusValue;
            assert coeff2[pos2 + i] < modulusValue;
            sum = coeff1[pos1 + i] + coeff2[pos2 + i];
            coeffR[posR + i] = sum >= modulusValue ? sum - modulusValue : sum;
        }
    }

    /**
     * Adds two RNS representations.
     *
     * @param rns1    the 1st RNS representation.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param rns2    the 2nd RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void addPolyCoeffModRns(long[] rns1, int n1, int k1, long[] rns2, int n2, int k2,
                                          Modulus[] modulus, long[] rnsR, int n, int k) {
        addPolyCoeffModRns(rns1, 0, n1, k1, rns2, 0, n2, k2, modulus, rnsR, 0, n, k);
    }

    /**
     * Adds two RNS representations.
     *
     * @param rns1    the 1st RNS representation.
     * @param pos1    the 1st start position.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param rns2    the 2nd RNS representation.
     * @param pos2    the 2nd start position.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param posR    the result start position.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void addPolyCoeffModRns(long[] rns1, int pos1, int n1, int k1, long[] rns2, int pos2, int n2, int k2,
                                          Modulus[] modulus, long[] rnsR, int posR, int n, int k) {
        assert k == k1 && k == k2 && k == modulus.length;
        assert n == n1 && n == n2;

        for (int j = 0; j < k; j++) {
            assert !modulus[j].isZero();
            long modulusValue = modulus[j].getValue();
            int jOffset = j * n;
            long sum;
            for (int i = 0; i < n; i++) {
                assert rns1[pos1 + jOffset + i] < modulusValue;
                assert rns2[pos2 + jOffset + i] < modulusValue;
                sum = rns1[pos1 + jOffset + i] + rns2[pos2 + jOffset + i];
                rnsR[posR + jOffset + i] = sum >= modulusValue ? sum - modulusValue : sum;
            }
        }
    }

    /**
     * Adds first m RNS representations in the two Poly-RNS representations.
     *
     * @param poly1   the 1st Poly-RNS representation.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param poly2   the 2nd Poly-RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param m       the number of added RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-NRS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void addPolyCoeffModPoly(long[] poly1, int n1, int k1, long[] poly2, int n2, int k2,
                                           int m, Modulus[] modulus, long[] polyR, int n, int k) {
        assert n == n1 && n == n2;
        assert k == k1 && k == k2 && k == modulus.length;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                Modulus currentModulus = modulus[j];
                assert !currentModulus.isZero();
                int jOffset = rOffset + j * n;
                for (int i = 0; i < n; i++) {
                    assert poly1[jOffset + i] < currentModulus.getValue();
                    assert poly2[jOffset + i] < currentModulus.getValue();
                    long sum = poly1[jOffset + i] + poly2[jOffset + i];
                    polyR[jOffset + i] = sum >= currentModulus.getValue() ? sum - currentModulus.getValue() : sum;
                }
            }
        }
    }

    public static void addPolyCoeffMod(RnsIter operand1, RnsIter operand2, int coeffModulusSize, Modulus[] modulus, RnsIter result) {
        assert coeffModulusSize > 0;
        assert operand1.getPolyModulusDegree() == result.getPolyModulusDegree();
        assert operand2.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();

        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            long modulusValue = modulus[i].getValue();

            for (int j = 0; j < polyModulusDegree; j++) {
                assert operand1.coeffIter[i * polyModulusDegree + j] < modulusValue;
                assert operand2.coeffIter[i * polyModulusDegree + j] < modulusValue;
                long sum = operand1.coeffIter[i * polyModulusDegree + j] + operand2.coeffIter[i * polyModulusDegree + j];
                result.coeffIter[i * polyModulusDegree + j] = sum >= modulusValue ? sum - modulusValue : sum;
            }
        }
    }

    public static void addPolyCoeffMod(PolyIter operand1, PolyIter operand2, int size, Modulus[] modulus, PolyIter result) {
        assert size > 0;
        assert operand1.getCoeffModulusSize() == result.getCoeffModulusSize();
        assert operand2.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();
        for (int i = 0; i < size; i++) {
            addPolyCoeffMod(operand1.getRnsIter(i), operand2.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
        }
    }

    /**
     * Subtracts two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param coeff2  the 2nd Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param result  the result Coeff representation.
     */
    public static void subPolyCoeffMod(long[] coeff1, long[] coeff2, int n, Modulus modulus, long[] result) {
        subPolyCoeffMod(coeff1, 0, coeff2, 0, n, modulus, result, 0);
    }

    /**
     * Subtracts two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param pos1    the 1st start position.
     * @param coeff2  the 2nd Coeff representation.
     * @param pos2    the 2nd start position.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void subPolyCoeffMod(long[] coeff1, int pos1, long[] coeff2, int pos2,
                                       int n, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long[] tempResult = new long[1];
        long borrow;
        for (int i = 0; i < n; i++) {
            assert coeff1[pos1 + i] < modulusValue;
            assert coeff2[pos2 + i] < modulusValue;
            borrow = UintArithmetic.subUint64(coeff1[pos1 + i], coeff2[pos2 + i], tempResult);
            coeffR[posR + i] = tempResult[0] + (modulusValue & (-borrow));
        }
    }

    /**
     * Subtracts first m RNS representations in the two Poly-RNS representations.
     *
     * @param poly1   the 1st Poly-RNS representation.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param poly2   the 2nd Poly-RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param m       the number of added RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-NRS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void subPolyCoeffModPoly(long[] poly1, int n1, int k1, long[] poly2, int n2, int k2,
                                           int m, Modulus[] modulus, long[] polyR, int n, int k) {
        assert n == n1 && n == n2;
        assert k == k1 && k == k2 && k == modulus.length;
        assert m > 0;

        long[] tempResult = new long[1];
        long borrow;
        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                Modulus currentModulus = modulus[j];
                assert !currentModulus.isZero();
                int jOffset = rOffset + j * n;
                for (int i = 0; i < n; i++) {
                    assert poly1[jOffset + i] < currentModulus.getValue();
                    assert poly2[jOffset + i] < currentModulus.getValue();
                    borrow = UintArithmetic.subUint64(poly1[jOffset + i], poly2[jOffset + i], tempResult);
                    polyR[jOffset + i] = tempResult[0] + (currentModulus.getValue() & (-borrow));
                }
            }
        }
    }

    public static void subPolyCoeffMod(RnsIter operand1, RnsIter operand2, int coeffModulusSize, Modulus[] modulus, RnsIter result) {
        assert coeffModulusSize > 0;
        assert operand1.getPolyModulusDegree() == result.getPolyModulusDegree();
        assert operand2.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();
        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            for (int j = 0; j < polyModulusDegree; j++) {
                assert operand1.coeffIter[i * polyModulusDegree + j] < modulus[i].getValue();
                assert operand2.coeffIter[i * polyModulusDegree + j] < modulus[i].getValue();

                long[] temp = new long[1];
                long borrow = UintArithmetic.subUint64(operand1.coeffIter[i * polyModulusDegree + j], operand2.coeffIter[i * polyModulusDegree + j], temp);
                result.coeffIter[i * polyModulusDegree + j] = temp[0] + (modulus[i].getValue() & (-borrow));
            }
        }
    }

    public static void subPolyCoeffMod(PolyIter operand1, PolyIter operand2, int size, Modulus[] modulus, PolyIter result) {
        assert size > 0;
        assert operand1.getCoeffModulusSize() == result.getCoeffModulusSize();
        assert operand2.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();
        for (int i = 0; i < size; i++) {
            subPolyCoeffMod(operand1.getRnsIter(i), operand2.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
        }
    }

    /**
     * Adds a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void addPolyScalarCoeffMod(long[] coeff, int n, long scalar, Modulus modulus, long[] coeffR) {
        addPolyScalarCoeffMod(coeff, 0, n, scalar, modulus, coeffR, 0);
    }

    /**
     * Adds a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void addPolyScalarCoeffMod(long[] coeff, int pos, int n,
                                             long scalar, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();
        assert scalar < modulus.getValue();

        for (int i = 0; i < n; i++) {
            coeffR[posR + i] = UintArithmeticSmallMod.addUintMod(coeff[pos + i], scalar, modulus);
        }
    }

    public static void addPolyScalarCoeffMod(RnsIter poly, int coeffModulusSize, long scalar, Modulus[] modulus, RnsIter result) {
        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();
        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            for (int j = 0; j < polyModulusDegree; j++) {
                result.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.addUintMod(poly.coeffIter[i * polyModulusDegree + j], scalar, modulus[i]);
            }
        }
    }

    public static void addPolyScalarCoeffMod(PolyIter poly, int size, long scalar, Modulus[] modulus, PolyIter result) {
        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();
        IntStream.range(0, size).forEach(
            i -> addPolyScalarCoeffMod(poly.getRnsIter(i), coeffModulusSize, scalar, modulus, result.getRnsIter(i))
        );
    }

    /**
     * Subtracts a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     */
    public static void subPolyScalarCoeffMod(long[] coeff, int n, long scalar, Modulus modulus, long[] coeffR) {
        subPolyScalarCoeffMod(coeff, 0, n, scalar, modulus, coeffR, 0);
    }

    /**
     * Subtracts a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param posR    the result Coeff representation.
     */
    public static void subPolyScalarCoeffMod(long[] coeff, int pos, int n,
                                             long scalar, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();
        assert scalar < modulus.getValue();

        for (int i = 0; i < n; i++) {
            coeffR[posR + i] = UintArithmeticSmallMod.subUintMod(coeff[pos + i], scalar, modulus);
        }
    }

    public static void subPolyScalarCoeffMod(RnsIter poly, int coeffModulusSize, long scalar, Modulus[] modulus, RnsIter result) {
        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = poly.getPolyModulusDegree();
        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            for (int j = 0; j < polyModulusDegree; j++) {
                result.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.subUintMod(poly.coeffIter[i * polyModulusDegree + j], scalar, modulus[i]);
            }
        }
    }

    public static void subPolyScalarCoeffMod(PolyIter poly, int size, long scalar, Modulus[] modulus, PolyIter result) {
        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = poly.getCoeffModulusSize();
        IntStream.range(0, size).forEach(
            i -> subPolyScalarCoeffMod(poly.getRnsIter(i), coeffModulusSize, scalar, modulus, result.getRnsIter(i))
        );
    }

    /**
     * Negates the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void negatePolyCoeffMod(long[] coeff, int n, Modulus modulus, long[] coeffR) {
        negatePolyCoeffMod(coeff, 0, n, modulus, coeffR, 0);
    }

    /**
     * Negates the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param posR    the result start position.
     * @param coeffR  the result Coeff representation.
     */
    public static void negatePolyCoeffMod(long[] coeff, int pos, int n,
                                          Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long nonZero;
        for (int i = 0; i < n; i++) {
            assert coeff[pos + i] < modulusValue;
            nonZero = coeff[pos + i] != 0 ? 1 : 0;
            coeffR[posR + i] = (modulusValue - coeff[pos + i]) & (-nonZero);
        }
    }

    /**
     * Negates the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param posR    the result start position.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void negatePolyCoeffModRns(long[] rns, int pos, int n, int k,
                                             Modulus[] modulus, long[] rnsR, int posR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;

        for (int j = 0; j < k; j++) {
            Modulus currentModulus = modulus[j];
            assert !currentModulus.isZero();
            int jOffset = j * n;
            for (int i = 0; i < n; i++) {
                assert rns[pos + jOffset + i] < currentModulus.getValue();
                long nonZero = (rns[pos + jOffset + i] != 0) ? 1 : 0;
                rnsR[posR + jOffset + i] = (currentModulus.getValue() - rns[pos + jOffset + i]) & (-nonZero);
            }
        }
    }

    /**
     * Negates the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param m       the number of negated RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     * @param nR      the result number of RNS bases.
     * @param kR      the result number of RNS bases.
     */
    public static void negatePolyCoeffModPoly(long[] poly, int n, int k,
                                              int m, Modulus[] modulus, long[] polyR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                Modulus currentModulus = modulus[j];
                assert !currentModulus.isZero();
                int jOffset = rOffset + j * n;
                for (int i = 0; i < n; i++) {
                    assert poly[jOffset + i] < currentModulus.getValue();
                    long nonZero = (poly[jOffset + i] != 0) ? 1 : 0;
                    polyR[jOffset + i] = (currentModulus.getValue() - poly[jOffset + i]) & (-nonZero);
                }
            }
        }
    }

    public static void negatePolyCoeffMod(RnsIter poly, int coeffModulusSize, Modulus[] modulus, RnsIter result) {
        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = poly.getPolyModulusDegree();
        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            for (int j = 0; j < polyModulusDegree; j++) {
                assert poly.coeffIter[i * polyModulusDegree + j] < modulus[i].getValue();
                long nonZero = poly.coeffIter[i * polyModulusDegree + j] != 0 ? 1 : 0;
                result.coeffIter[i * polyModulusDegree + j] = (modulus[i].getValue() - poly.coeffIter[i * polyModulusDegree + j]) & (-nonZero);
            }
        }
    }

    public static void negatePolyCoeffMod(PolyIter poly, int size, Modulus[] modulus, PolyIter result) {
        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = poly.getCoeffModulusSize();
        for (int i = 0; i < size; i++) {
            negatePolyCoeffMod(poly.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
        }
    }

    /**
     * Multiplies a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void multiplyPolyScalarCoeffMod(long[] coeff, int n,
                                                  MultiplyUintModOperand scalar, Modulus modulus, long[] coeffR) {
        multiplyPolyScalarCoeffMod(coeff, 0, n, scalar, modulus, coeffR, 0);
    }

    /**
     * Multiplies a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void multiplyPolyScalarCoeffMod(long[] coeff, int pos, int n,
                                                  MultiplyUintModOperand scalar, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            coeffR[posR + i] = UintArithmeticSmallMod.multiplyUintMod(coeff[pos + i], scalar, modulus);
        }
    }

    /**
     * Multiplies a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void multiplyPolyScalarCoeffMod(long[] coeff, int n,
                                                  long scalar, Modulus modulus, long[] coeffR) {
        multiplyPolyScalarCoeffMod(coeff, 0, n, scalar, modulus, coeffR, 0);
    }

    /**
     * Multiplies a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void multiplyPolyScalarCoeffMod(long[] coeff, int pos, int n,
                                                  long scalar, Modulus modulus, long[] coeffR, int posR) {
        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus), modulus);
        multiplyPolyScalarCoeffMod(coeff, pos, n, tempScalar, modulus, coeffR, posR);
    }

    /**
     * Multiplies a scalar to the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param posR    the result start position.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void multiplyPolyScalarCoeffModRns(long[] rns, int pos, int n, int k,
                                                     long scalar, Modulus[] modulus, long[] rnsR, int posR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;

        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        for (int j = 0; j < k; j++) {
            Modulus currentModulus = modulus[j];
            assert !currentModulus.isZero();
            tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, currentModulus), currentModulus);
            int jOffset = j * n;
            for (int i = 0; i < n; i++) {
                rnsR[posR + jOffset + i] = UintArithmeticSmallMod.multiplyUintMod(rns[pos + jOffset + i], tempScalar, currentModulus);
            }
        }
    }

    /**
     * Multiplies a scalar to the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param m       the number of multiplied RNS representations.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void multiplyPolyScalarCoeffModPoly(long[] poly, int n, int k,
                                                      int m, long scalar, Modulus[] modulus, long[] polyR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;
        assert m > 0;

        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                Modulus currentModulus = modulus[j];
                assert !currentModulus.isZero();
                int jOffset = rOffset + j * n;
                tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, currentModulus), currentModulus);
                for (int i = 0; i < n; i++) {
                    polyR[jOffset + i] = UintArithmeticSmallMod.multiplyUintMod(poly[jOffset + i], tempScalar, currentModulus);
                }
            }
        }
    }

    /**
     * @param poly             input polly in Rns
     * @param coeffModulusSize N
     * @param scalar           scalar
     * @param modulus          modulus
     * @param result           (poly * scalar) mod mosulus
     */
    public static void multiplyPolyScalarCoeffMod(RnsIter poly, int coeffModulusSize, long scalar, Modulus[] modulus, RnsIter result) {
        assert coeffModulusSize > 0;
        assert coeffModulusSize == modulus.length;

        int polyModulusDegree = poly.getPolyModulusDegree();
        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            MultiplyUintModOperand curScalar = new MultiplyUintModOperand();
            curScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus[i]), modulus[i]);
            for (int j = 0; j < polyModulusDegree; j++) {
                result.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.multiplyUintMod(poly.coeffIter[i * polyModulusDegree + j], curScalar, modulus[i]);
            }
        }
    }

    public static void multiplyPolyScalarCoeffMod(PolyIter poly, int size, long scalar, Modulus[] modulus, PolyIter result) {
        assert size > 0;

        for (int i = 0; i < size; i++) {
            multiplyPolyScalarCoeffMod(poly.getRnsIter(i), poly.getCoeffModulusSize(), scalar, modulus, result.getRnsIter(i));
        }
    }

    /**
     * CoeffIter * CoeffIter = CoeffIter
     *
     * @param operand1   single poly, length is N
     * @param operand2   single poly, length is N
     * @param coeffCount N
     * @param modulus    single modulus
     * @param result     store the
     */
    public static void dyadicProductCoeffMod(long[] operand1, long[] operand2, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long constRation0 = modulus.getConstRatio()[0];
        long constRation1 = modulus.getConstRatio()[1];

        for (int i = 0; i < coeffCount; i++) {
            long[] z = new long[2];
            long tmp3, carry;
            long[] tmp1 = new long[1];
            long[] tmp2 = new long[2];

            // Reduces z using base 2^64 Barrett reduction
            // x * y
            UintArithmetic.multiplyUint64(operand1[i], operand2[i], z);

            // Multiply input and const_ratio
            // Round 1
            carry = UintArithmetic.multiplyUint64Hw64(z[0], constRation0);
            UintArithmetic.multiplyUint64(z[0], constRation1, tmp2);
            tmp3 = tmp2[1] + UintArithmetic.addUint64(tmp2[0], carry, tmp1);

            // Round 2
            UintArithmetic.multiplyUint64(z[1], constRation0, tmp2);
            carry = tmp2[1] + UintArithmetic.addUint64(tmp1[0], tmp2[0], tmp1);

            // This is all we care about
            tmp1[0] = z[1] * constRation1 + tmp3 + carry;

            // Barrett subtraction
            tmp3 = z[0] - tmp1[0] * modulusValue;

            // Claim: One more subtraction is enough
            result[i] = tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3;
        }

    }


    public static void dyadicProductCoeffModFor(long[] operand1, long[] operand2, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long constRation0 = modulus.getConstRatio()[0];
        long constRation1 = modulus.getConstRatio()[1];

        for (int i = 0; i < coeffCount; i++) {
            long[] z = new long[2];
            long tmp3, carry;
            long[] tmp1 = new long[1];
            long[] tmp2 = new long[2];

            // Reduces z using base 2^64 Barrett reduction
            // x * y
            UintArithmetic.multiplyUint64(operand1[i], operand2[i], z);

            // Multiply input and const_ratio
            // Round 1
            carry = UintArithmetic.multiplyUint64Hw64(z[0], constRation0);
            UintArithmetic.multiplyUint64(z[0], constRation1, tmp2);
            tmp3 = tmp2[1] + UintArithmetic.addUint64(tmp2[0], carry, tmp1);

            // Round 2
            UintArithmetic.multiplyUint64(z[1], constRation0, tmp2);
            carry = tmp2[1] + UintArithmetic.addUint64(tmp1[0], tmp2[0], tmp1);

            // This is all we care about
            tmp1[0] = z[1] * constRation1 + tmp3 + carry;

            // Barrett subtraction
            tmp3 = z[0] - tmp1[0] * modulusValue;

            // Claim: One more subtraction is enough
            result[i] = tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3;
        }

    }


    /**
     * RnsIter + startIndex = CoeffIter, 处理的基本单位为 CoeffIter
     *
     * @param operand1Array    single poly in RNS, length is k * N
     * @param startIndex1      startIndex of a singel poly in operand1Array
     * @param operand2Array    single poly in RNS, length is k * N
     * @param startIndex2      startIndex of a singel poly in operand2Array
     * @param coeffCount       N
     * @param modulus          multi modulus, length is k
     * @param resultStartIndex startIndex of a singel poly in result
     * @param result           single poly in RNS, length is k * N
     */
    public static void dyadicProductCoeffMod(long[] operand1Array, int startIndex1, long[] operand2Array, int startIndex2, int coeffCount, Modulus modulus, int resultStartIndex, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long constRation0 = modulus.getConstRatio()[0];
        long constRation1 = modulus.getConstRatio()[1];


        for (int i = 0; i < coeffCount; i++) {
            long[] z = new long[2];
            long tmp3, carry;
            long[] tmp1 = new long[1];
            long[] tmp2 = new long[2];
            // Reduces z using base 2^64 Barrett reduction
            UintArithmetic.multiplyUint64(operand1Array[startIndex1 + i], operand2Array[startIndex2 + i], z);

            // Multiply input and const_ratio
            // Round 1
            carry = UintArithmetic.multiplyUint64Hw64(z[0], constRation0);
            UintArithmetic.multiplyUint64(z[0], constRation1, tmp2);
            tmp3 = tmp2[1] + UintArithmetic.addUint64(tmp2[0], carry, tmp1);

            // Round 2
            UintArithmetic.multiplyUint64(z[1], constRation0, tmp2);
            carry = tmp2[1] + UintArithmetic.addUint64(tmp1[0], tmp2[0], tmp1);

            // This is all we care about
            tmp1[0] = z[1] * constRation1 + tmp3 + carry;

            // Barrett subtraction
            tmp3 = z[0] - tmp1[0] * modulusValue;

            // Claim: One more subtraction is enough
            result[resultStartIndex + i] = tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3;
        }

    }

    /**
     * 注意函数名和变量名，函数名后缀 RnsIter 表示当前函数运行一次 处理的基本对象是 RnsIter. 即 k * N 的长度
     * <p>
     * 输入是 polyIter, 长度是 size * k * N, polyIter + startIndex 定位到处理的是哪一个 RnsIter ,
     * 从起点到往后的步长为 k * N 表示一个 合法的 RnsIter,即：[startIndex, startIndex + k * N)
     * 注意到 合法的 startIndex 是: 0 * k * N , 1 * k * N , 2 * k * N , ...,(size - 1) * k * N
     * 所以合法的 startIndex 一定可以整除 k * N
     *
     * @param polyIter1
     * @param startIndex1
     * @param polyIter2
     * @param startIndex2
     * @param coeffModulusSize
     * @param coeffCount
     * @param modulus
     * @param resultStartIndex
     * @param resultPolyIter
     */
    public static void dyadicProductCoeffModRnsIter(long[] polyIter1,
                                                    int startIndex1,
                                                    long[] polyIter2,
                                                    int startIndex2,
                                                    int coeffModulusSize,
                                                    int coeffCount,
                                                    Modulus[] modulus,
                                                    int resultStartIndex,
                                                    long[] resultPolyIter) {

        assert coeffCount > 0;
        assert modulus.length == coeffModulusSize;

        assert startIndex1 % (coeffCount * coeffModulusSize) == 0;
        assert startIndex2 % (coeffCount * coeffModulusSize) == 0;
        assert resultStartIndex % (coeffCount * coeffModulusSize) == 0;


        for (int j = 0; j < coeffModulusSize; j++) {
            assert !modulus[j].isZero();

            int polyStartIndex = j * coeffCount;
            long modulusValue = modulus[j].getValue();
            long constRation0 = modulus[j].getConstRatio()[0];
            long constRation1 = modulus[j].getConstRatio()[1];

            for (int i = 0; i < coeffCount; i++) {
                // 处理多项式的 每一个系数
                long[] z = new long[2];
                long tmp3, carry;
                long[] tmp1 = new long[1];
                long[] tmp2 = new long[2];
                // Reduces z using base 2^64 Barrett reduction
                UintArithmetic.multiplyUint64(polyIter1[startIndex1 + polyStartIndex + i], polyIter2[startIndex2 + polyStartIndex + i], z);

                // Multiply input and const_ratio
                // Round 1
                carry = UintArithmetic.multiplyUint64Hw64(z[0], constRation0);
                UintArithmetic.multiplyUint64(z[0], constRation1, tmp2);
                tmp3 = tmp2[1] + UintArithmetic.addUint64(tmp2[0], carry, tmp1);

                // Round 2
                UintArithmetic.multiplyUint64(z[1], constRation0, tmp2);
                carry = tmp2[1] + UintArithmetic.addUint64(tmp1[0], tmp2[0], tmp1);

                // This is all we care about
                tmp1[0] = z[1] * constRation1 + tmp3 + carry;

                // Barrett subtraction
                tmp3 = z[0] - tmp1[0] * modulusValue;

                // Claim: One more subtraction is enough
                resultPolyIter[resultStartIndex + polyStartIndex + i] = tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3;
            }
        }
    }

    /**
     * 处理单个 CoeffIter, 不管输入数组是什么，反正通过 数组+startIndex 定位到 单个 CoeffIter 的起点
     *
     * @param polyIter1
     * @param startIndex1
     * @param polyIter2
     * @param startIndex2
     * @param coeffCount
     * @param modulus
     * @param resultStartIndex
     * @param resultPolyIter
     */
    public static void dyadicProductCoeffModCoeffIter(long[] polyIter1, int startIndex1, long[] polyIter2, int startIndex2, int coeffCount, Modulus modulus, int resultStartIndex, long[] resultPolyIter) {

        assert coeffCount > 0;

        assert startIndex1 % coeffCount == 0;
        assert startIndex2 % coeffCount == 0;
        assert resultStartIndex % coeffCount == 0;


        assert !modulus.isZero();
        long modulusValue = modulus.getValue();
        long constRation0 = modulus.getConstRatio()[0];
        long constRation1 = modulus.getConstRatio()[1];

        for (int i = 0; i < coeffCount; i++) {
            // 处理多项式的 每一个系数
            long[] z = new long[2];
            long tmp3, carry;
            long[] tmp1 = new long[1];
            long[] tmp2 = new long[2];
            // Reduces z using base 2^64 Barrett reduction
            UintArithmetic.multiplyUint64(polyIter1[startIndex1 + i], polyIter2[startIndex2 + i], z);

            // Multiply input and const_ratio
            // Round 1
            carry = UintArithmetic.multiplyUint64Hw64(z[0], constRation0);
            UintArithmetic.multiplyUint64(z[0], constRation1, tmp2);
            tmp3 = tmp2[1] + UintArithmetic.addUint64(tmp2[0], carry, tmp1);

            // Round 2
            UintArithmetic.multiplyUint64(z[1], constRation0, tmp2);
            carry = tmp2[1] + UintArithmetic.addUint64(tmp1[0], tmp2[0], tmp1);

            // This is all we care about
            tmp1[0] = z[1] * constRation1 + tmp3 + carry;

            // Barrett subtraction
            tmp3 = z[0] - tmp1[0] * modulusValue;

            // Claim: One more subtraction is enough
            resultPolyIter[resultStartIndex + i] = tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3;
        }

    }


    public static void dyadicProductCoeffMod(RnsIter operand1, RnsIter operand2, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert operand1.getPolyModulusDegree() == result.getPolyModulusDegree();
        assert operand2.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();

        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();

            long modulusValue = modulus[i].getValue();
            long constRation0 = modulus[i].getConstRatio()[0];
            long constRation1 = modulus[i].getConstRatio()[1];

            for (int j = 0; j < polyModulusDegree; j++) {
                long[] z = new long[2];
                long tmp3, carry;
                long[] tmp1 = new long[1];
                long[] tmp2 = new long[2];
                // Reduces z using base 2^64 Barrett reduction
                UintArithmetic.multiplyUint64(operand1.coeffIter[i * polyModulusDegree + j], operand2.coeffIter[i * polyModulusDegree + j], z);

                // Multiply input and const_ratio
                // Round 1
                carry = UintArithmetic.multiplyUint64Hw64(z[0], constRation0);
                UintArithmetic.multiplyUint64(z[0], constRation1, tmp2);
                tmp3 = tmp2[1] + UintArithmetic.addUint64(tmp2[0], carry, tmp1);

                // Round 2
                UintArithmetic.multiplyUint64(z[1], constRation0, tmp2);
                carry = tmp2[1] + UintArithmetic.addUint64(tmp1[0], tmp2[0], tmp1);

                // This is all we care about
                tmp1[0] = z[1] * constRation1 + tmp3 + carry;

                // Barrett subtraction
                tmp3 = z[0] - tmp1[0] * modulusValue;

                // Claim: One more subtraction is enough
                result.coeffIter[i * polyModulusDegree + j] = tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3;
            }
        }

    }

    public static void dyadicProductCoeffMod(PolyIter operand1, PolyIter operand2, int size, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert operand1.getCoeffModulusSize() == result.getCoeffModulusSize();
        assert operand2.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();

        for (int i = 0; i < size; i++) {

            dyadicProductCoeffMod(operand1.getRnsIter(i), operand2.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
        }

    }

//    public static long polyInftyNormCoeffMod(long[] operand, int coeffCount, Modulus modulus) {
//
//        assert coeffCount > 0;
//        assert !modulus.isZero();
//        // Construct negative threshold (first negative modulus value) to compute absolute values of coeffs.
//        // (p + 1)/2
//        long modulusNegThreshold = (modulus.getValue() + 1) >>> 1;
//
//        // Mod out the poly coefficients and choose a symmetric representative from
//        // [-modulus,modulus). Keep track of the max.
//        long[] result = new long[coeffCount];
//        IntStream.range(0, coeffCount).parallel().forEach(
//                i -> {
//                    long polyCoeff = UintArithmeticSmallMod.barrettReduce64(operand[i], modulus);
//                    if (polyCoeff >= modulusNegThreshold) {
//                        polyCoeff = modulus.getValue() - polyCoeff;
//                    }
//                    result[i] = polyCoeff;
//                }
//        );
//
//        return Arrays.stream(result).max().getAsLong();
//    }

    /**
     * 处理单个 CoeffIter, 通过 long[] + startIndex 来定位到 单个CoeffIter
     *
     * @param poly
     * @param coeffCount
     * @param shift
     * @param modulus
     * @param result
     */
    public static void negAcyclicShiftPolyCoeffModCoeffIter(
        long[] poly,
        int polyStartIndex,
        int coeffCount,
        int shift,
        Modulus modulus,
        long[] result,
        int resultStartIndex
    ) {


        assert poly != result;
        assert !modulus.isZero();
        assert UintCore.getPowerOfTwo(coeffCount) >= 0;
        // todo: 是否需要这个条件？某些情况下，无法通过 ，但是不通过是不影响正确性的
//        assert shift < coeffCount;
        // Nothing to do, just copy
        if (shift == 0) {
            System.arraycopy(
                poly,
                polyStartIndex,
                result,
                resultStartIndex,
                coeffCount
            );
//            UintCore.setUint(poly, coeffCount, result);
            return;
        }
        long indexRaw = shift;
        long coeffCountModMask = (long) (coeffCount) - 1L;

        for (int i = 0; i < coeffCount; i++, indexRaw++) {
            long index = indexRaw & coeffCountModMask;

            if ((indexRaw & (long) coeffCount) == 0 || poly[polyStartIndex + i] == 0) {
                result[(int) index + resultStartIndex] = poly[polyStartIndex + i];
            } else {
                result[(int) index + resultStartIndex] = modulus.getValue() - poly[i + polyStartIndex];
            }
        }
    }


    public static void negAcyclicShiftPolyCoeffMod(long[] poly, int coeffCount, int shift, Modulus modulus, long[] result) {


        assert poly != result;
        assert !modulus.isZero();
        assert UintCore.getPowerOfTwo(coeffCount) >= 0;
        assert shift < coeffCount;
        // Nothing to do
        if (shift == 0) {
            UintCore.setUint(poly, coeffCount, result);
            return;
        }
        long indexRaw = shift;
        long coeffCountModMask = (long) (coeffCount) - 1L;

        for (int i = 0; i < coeffCount; i++, indexRaw++) {
            long index = indexRaw & coeffCountModMask;

            if ((indexRaw & (long) coeffCount) == 0 || poly[i] == 0) {
                result[(int) index] = poly[i];
            } else {
                result[(int) index] = modulus.getValue() - poly[i];
            }
        }
    }

    public static void negAcyclicShiftPolyCoeffMod(RnsIter poly,
                                                   int coeffModulusSize,
                                                   int shift,
                                                   Modulus[] modulus,
                                                   RnsIter result
    ) {
        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        // nothing to do, just copy
        if (shift == 0) {
            System.arraycopy(poly.coeffIter, 0, result.coeffIter, 0, poly.coeffIter.length);
            return;
        }
        int polyModulusDegree = poly.getPolyModulusDegree();
//        long indexRaw = shift;
        long coeffCountModMask = (long) (polyModulusDegree) - 1L;

        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            // handle each range: [i * N , (i + 1) * N)
            long indexRaw = shift;
            for (int j = 0; j < polyModulusDegree; j++, indexRaw++) {
                long index = indexRaw & coeffCountModMask;
                if ((indexRaw & (long) polyModulusDegree) == 0 || poly.coeffIter[i * polyModulusDegree + j] == 0) {
                    // index + current range startPoint
                    result.coeffIter[(int) index + i * polyModulusDegree] = poly.coeffIter[i * polyModulusDegree + j];
                } else {
                    result.coeffIter[(int) index + i * polyModulusDegree] = modulus[i].getValue() - poly.coeffIter[i * polyModulusDegree + j];
                }
            }
        }
    }

    public static void negAcyclicShiftPolyCoeffMod(PolyIter poly,
                                                   int size,
                                                   int shift,
                                                   Modulus[] modulus,
                                                   PolyIter result) {
        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();

        for (int i = 0; i < size; i++) {
            negAcyclicShiftPolyCoeffMod(
                poly.getRnsIter(i),
                coeffModulusSize,
                shift,
                modulus,
                result.getRnsIter(i)
            );
        }
    }

    /**
     * 处理单个 coeffIter
     *
     * @param poly
     * @param coeffCount
     * @param monoCoeff
     * @param monoExponent
     * @param modulus
     * @param result
     */
    public static void negAcyclicMultiplyPolyMonoCoeffMod(
        long[] poly, int coeffCount, long monoCoeff, int monoExponent, Modulus modulus, long[] result
    ) {

        assert coeffCount > 0;
        assert !modulus.isZero();

        long[] temp = new long[coeffCount];
        // monoCoeff as Scalar
        multiplyPolyScalarCoeffMod(poly, coeffCount, monoCoeff, modulus, temp);
        // then shift
        negAcyclicShiftPolyCoeffMod(temp, coeffCount, monoExponent, modulus, result);
    }

    /**
     * 处理 整个 RnsIter
     *
     * @param poly
     * @param coeffModulusSize
     * @param monoCoeff
     * @param monoExponent
     * @param modulus
     * @param result
     */
    public static void negAcyclicMultiplyPolyMonoCoeffMod(
        RnsIter poly, int coeffModulusSize, long monoCoeff, int monoExponent, Modulus[] modulus, RnsIter result
    ) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        RnsIter temp = new RnsIter(poly.getCoeffModulusSize(), poly.getPolyModulusDegree());
        // monoCoeff as Scalar
        multiplyPolyScalarCoeffMod(poly, coeffModulusSize, monoCoeff, modulus, temp);
        // then shift
        negAcyclicShiftPolyCoeffMod(temp, coeffModulusSize, monoExponent, modulus, result);

    }

    /**
     * 处理整个PolyIter
     *
     * @param polyArray
     * @param size
     * @param monoCoeff
     * @param monoExponent
     * @param modulus
     * @param result
     */
    public static void negAcyclicMultiplyPolyMonoCoeffMod(
        PolyIter polyArray, int size, long monoCoeff, int monoExponent, Modulus[] modulus, PolyIter result
    ) {

        assert size > 0;
        assert polyArray.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();
        for (int i = 0; i < size; i++) {
            negAcyclicMultiplyPolyMonoCoeffMod(
                polyArray.getRnsIter(i),
                coeffModulusSize,
                monoCoeff,
                monoExponent,
                modulus,
                result.getRnsIter(i)
            );
        }

    }

    /**
     * 注意第4个参数，是一个数组. 长度就是 coeffModulusSize，每个 qi 下处理对应的
     *
     * @param poly
     * @param polyCoeffCount
     * @param polyCoeffModulusSize
     * @param size
     * @param monoCoeff
     * @param monoExponent
     * @param modulus
     * @param result
     * @param resultCoeffCount
     * @param resultCoeffModulusSize
     */
    public static void negAcyclicMultiplyPolyMonoCoeffModPolyIter(
        long[] poly,
        int polyCoeffCount,
        int polyCoeffModulusSize,
        int size,
        long[] monoCoeff,
        int monoExponent,
        Modulus[] modulus,
        long[] result,
        int resultCoeffCount,
        int resultCoeffModulusSize
    ) {

        assert poly != null;
        assert result != null;
        assert polyCoeffCount == resultCoeffCount;
        assert polyCoeffModulusSize == resultCoeffModulusSize;
        assert size > 0;
        // 避免重复 new 数组，放在循环外面
        long[] temp = new long[polyCoeffCount];
        // 遍历 每一个密文多项式
        for (int i = 0; i < size; i++) {
            int rnsStartIndex = i * polyCoeffCount * polyCoeffModulusSize;
            // 遍历每一个 RnsIter 下的多项式
            for (int j = 0; j < polyCoeffModulusSize; j++) {
                int coeffStartIndex = rnsStartIndex + j * polyCoeffCount;
                Modulus curModulus = modulus[j];

                // 处理单个CoeffIter
                multiplyPolyScalarCoeffMod(
                    poly,
                    coeffStartIndex,
                    polyCoeffCount,
                    monoCoeff[j], // todo: 需要现在循环外面把 monoCoeff 先处理好吗？这样可以 减少 size 倍数的new MultiplyOperand 次数
                    curModulus, // 注意这里是把结果放在 temp 中，它的起点为0
                    temp, // temp 的值每次会被覆盖掉，不需要担心，也不需要额外处理,
                    0
                );
                // 处理单个
                negAcyclicShiftPolyCoeffModCoeffIter(
                    temp,
                    0, // 注意起点
                    polyCoeffCount,
                    monoExponent,
                    curModulus,
                    result,
                    coeffStartIndex // 注意起点
                );
            }
        }
    }


    /**
     * 处理整个 PolyIter， 用(long[] + k + N)表示PolyIter, 不需要 startIndex
     *
     * @param poly
     * @param polyCoeffCount
     * @param polyCoeffModulusSize
     * @param size
     * @param monoCoeff
     * @param monoExponent
     * @param modulus
     * @param result
     * @param resultCoeffCount
     * @param resultCoeffModulusSize
     */
    public static void negAcyclicMultiplyPolyMonoCoeffModPolyIter(
        long[] poly,
        int polyCoeffCount,
        int polyCoeffModulusSize,
        int size,
        long monoCoeff,
        int monoExponent,
        Modulus[] modulus,
        long[] result,
        int resultCoeffCount,
        int resultCoeffModulusSize
    ) {

        assert poly != null;
        assert result != null;
        assert polyCoeffCount == resultCoeffCount;
        assert polyCoeffModulusSize == resultCoeffModulusSize;
        assert size > 0;
        // 避免重复 new 数组，放在循环外面
        long[] temp = new long[polyCoeffCount];
        // 遍历 每一个密文多项式
        for (int i = 0; i < size; i++) {
            int rnsStartIndex = i * polyCoeffCount * polyCoeffModulusSize;
            // 遍历每一个 RnsIter 下的多项式
            for (int j = 0; j < polyCoeffModulusSize; j++) {
                int coeffStartIndex = rnsStartIndex + j * polyCoeffCount;
                Modulus curModulus = modulus[j];

                // 处理单个CoeffIter
                multiplyPolyScalarCoeffMod(
                    poly,
                    coeffStartIndex,
                    polyCoeffCount,
                    monoCoeff, // todo: 需要现在循环外面把 monoCoeff 先处理好吗？这样可以 减少 size 倍数的new MultiplyOperand 次数
                    curModulus, // 注意这里是把结果放在 temp 中，它的起点为0
                    temp, // temp 的值每次会被覆盖掉，不需要担心，也不需要额外处理,
                    0
                );
                // 处理单个
                negAcyclicShiftPolyCoeffModCoeffIter(
                    temp,
                    0, // 注意起点
                    polyCoeffCount,
                    monoExponent,
                    curModulus,
                    result,
                    coeffStartIndex // 注意起点
                );
            }
        }
    }


    /**
     * 处理整个 RnsIter，注意到多项式系数是多个值
     *
     * @param poly
     * @param coeffModulusSize
     * @param monoCoeff
     * @param monoExponent
     * @param modulus
     * @param result
     */
    public static void negAcyclicMultiplyPolyMonoCoeffMod(
        RnsIter poly, int coeffModulusSize, long[] monoCoeff, int monoExponent, Modulus[] modulus, RnsIter result
    ) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();
        // first mul
        RnsIter temp = new RnsIter(poly.getCoeffModulusSize(), poly.getPolyModulusDegree());

        for (int i = 0; i < coeffModulusSize; i++) {
            assert !modulus[i].isZero();
            MultiplyUintModOperand curScalar = new MultiplyUintModOperand();
            curScalar.set(UintArithmeticSmallMod.barrettReduce64(monoCoeff[i], modulus[i]), modulus[i]);

            for (int j = 0; j < polyModulusDegree; j++) {
                assert poly.coeffIter[i * polyModulusDegree + j] < modulus[i].getValue();

                temp.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.multiplyUintMod(
                    poly.coeffIter[i * polyModulusDegree + j], curScalar, modulus[i]);
            }
        }

        // then neg acyclic
        negAcyclicShiftPolyCoeffMod(temp, coeffModulusSize, monoExponent, modulus, result);

    }


    public static void negAcyclicMultiplyPolyMonoCoeffMod(
        PolyIter polyArray, int size, long[] monoCoeff, int monoExponent, Modulus[] modulus, PolyIter result
    ) {

        assert size > 0;
        assert polyArray.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();
        IntStream.range(0, size).forEach(
            i -> {
                negAcyclicMultiplyPolyMonoCoeffMod(
                    polyArray.getRnsIter(i),
                    coeffModulusSize,
                    monoCoeff,
                    monoExponent,
                    modulus,
                    result.getRnsIter(i)
                );
            }
        );
    }

    /**
     * @param operand    a poly
     * @param coeffCount N
     * @param modulus    modulus
     * @return
     */
    public static long polyInftyNormCoeffMod(long[] operand, int coeffCount, Modulus modulus) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        // Construct negative threshold (first negative modulus value) to compute absolute values of coeffs.
        long modulusNegThreshold = (modulus.getValue() + 1) >>> 1;


        return Arrays.stream(operand)
            .map(n -> UintArithmeticSmallMod.barrettReduce64(n, modulus))
            .map(n -> n >= modulusNegThreshold ? modulus.getValue() - n : n)
            .max().orElseThrow(() -> new IllegalArgumentException("operand is empty"));
    }


}
