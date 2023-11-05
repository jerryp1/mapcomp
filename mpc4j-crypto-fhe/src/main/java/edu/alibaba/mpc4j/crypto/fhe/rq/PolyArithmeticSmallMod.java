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
    public static void moduloPolyCoeff(long[] coeff, int n, Modulus modulus, long[] coeffR) {
        moduloPolyCoeff(coeff, 0, n, modulus, coeffR, 0);
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
    public static void moduloPolyCoeff(long[] coeff, int pos, int n, Modulus modulus, long[] result, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            result[posR + i] = UintArithmeticSmallMod.barrettReduce64(coeff[pos + i], modulus);
        }
    }

    public static void moduloPolyCoeff(RnsIter poly, int coeffModulusSize, Modulus[] modulus, RnsIter result) {
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

    public static void moduloPolyCoeff(PolyIter polyArray, int size, Modulus[] modulus, PolyIter result) {
        assert size > 0;
        assert polyArray.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = polyArray.getCoeffModulusSize();
        for (int i = 0; i < size; i++) {
            moduloPolyCoeff(polyArray.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
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
            int jOffset = j * n;
            addPolyCoeffMod(rns1, pos1 + jOffset, rns2, pos2 + jOffset, n, modulus[j], rnsR, posR + jOffset);
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
                int jOffset = rOffset + j * n;
                addPolyCoeffMod(poly1, jOffset, poly2, jOffset, n, modulus[j], polyR, jOffset);
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

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                subPolyCoeffMod(poly1, jOffset, poly2, jOffset, n, modulus[j], polyR, jOffset);
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
            int jOffset = j * n;
            negatePolyCoeffMod(rns, pos + jOffset, n, modulus[j], rnsR, posR + jOffset);
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
                int jOffset = rOffset + j * n;
                negatePolyCoeffMod(poly, jOffset, n, modulus[j], polyR, jOffset);
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
            int jOffset = j * n;
            tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus[j]), modulus[j]);
            multiplyPolyScalarCoeffMod(rns, pos + jOffset, n, tempScalar, modulus[j], rnsR, posR + jOffset);
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
                multiplyPolyScalarCoeffMod(poly, jOffset, n, tempScalar, modulus[j], polyR, jOffset);
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
     * Dyadic products two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param coeff2  the 2nd Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void dyadicProductCoeffMod(long[] coeff1, long[] coeff2, int n, Modulus modulus, long[] coeffR) {
        dyadicProductCoeffMod(coeff1, 0, coeff2, 0, n, modulus, coeffR, 0);
    }

    /**
     * Dyadic products two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param pos1    the 1st start position.
     * @param coeff2  the 2nd Coeff representation.
     * @param pos2    the 2nd start position.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void dyadicProductCoeffMod(long[] coeff1, int pos1, long[] coeff2, int pos2,
                                             int n, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long constRation0 = modulus.getConstRatio()[0];
        long constRation1 = modulus.getConstRatio()[1];
        for (int i = 0; i < n; i++) {
            long[] z = new long[2];
            long tmp3, carry;
            long[] tmp1 = new long[1];
            long[] tmp2 = new long[2];
            // Reduces z using base 2^64 Barrett reduction
            UintArithmetic.multiplyUint64(coeff1[pos1 + i], coeff2[pos2 + i], z);
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
            coeffR[posR + i] = tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3;
        }
    }

    /**
     * Dyadic products two RNS representations.
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
    public static void dyadicProductCoeffModRns(long[] rns1, int pos1, int n1, int k1, long[] rns2, int pos2, int n2, int k2,
                                                Modulus[] modulus, long[] rnsR, int posR, int n, int k) {
        assert k == k1 && k == k2 && k == modulus.length;
        assert n == n1 && n == n2;

        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            dyadicProductCoeffMod(rns1, pos1 + jOffset, rns2, pos2 + jOffset, n, modulus[j], rnsR, posR + jOffset);
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

    /**
     * Negative cyclic shift Coeff representation.
     *
     * @param coeff the Coeff representation.
     * @param n the modulus polynomial degree.
     * @param shift shift.
     * @param modulus modulus.
     * @param coeffR the result Coeff representation.
     */
    public static void negacyclicShiftPolyCoeffMod(long[] coeff, int n, int shift, Modulus modulus, long[] coeffR) {
        negacyclicShiftPolyCoeffMod(coeff, 0, n, shift, modulus, coeffR, 0);
    }

    /**
     * Negative cyclic shift Coeff representation.
     *
     * @param coeff the Coeff representation.
     * @param pos the start position.
     * @param n the modulus polynomial degree.
     * @param shift shift.
     * @param modulus modulus.
     * @param coeffR the result Coeff representation.
     * @param posR the result start position.
     */
    public static void negacyclicShiftPolyCoeffMod(long[] coeff, int pos, int n, int shift, Modulus modulus, long[] coeffR, int posR) {
        assert coeff != coeffR;
        assert !modulus.isZero();
        assert UintCore.getPowerOfTwo(n) >= 0;

        // Nothing to do, just copy
        if (shift == 0) {
            UintCore.setUint(coeff, pos, n, coeffR, posR, n);
            return;
        }
        long indexRaw = shift;
        long coeffCountModMask = (long) (n) - 1L;

        for (int i = 0; i < n; i++, indexRaw++) {
            long index = indexRaw & coeffCountModMask;
            if ((indexRaw & (long) n) == 0 || coeff[pos + i] == 0) {
                coeffR[(int) index + posR] = coeff[pos + i];
            } else {
                coeffR[(int) index + posR] = modulus.getValue() - coeff[i + pos];
            }
        }
    }

    public static void negacyclicShiftPolyCoeffMod(RnsIter poly, int coeffModulusSize, int shift, Modulus[] modulus, RnsIter result) {
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

    public static void negacyclicShiftPolyCoeffMod(PolyIter poly, int size, int shift, Modulus[] modulus, PolyIter result) {
        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();

        for (int i = 0; i < size; i++) {
            negacyclicShiftPolyCoeffMod(poly.getRnsIter(i), coeffModulusSize, shift, modulus, result.getRnsIter(i));
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
        negacyclicShiftPolyCoeffMod(temp, coeffCount, monoExponent, modulus, result);
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
        negacyclicShiftPolyCoeffMod(temp, coeffModulusSize, monoExponent, modulus, result);

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
                negacyclicShiftPolyCoeffMod(
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
                negacyclicShiftPolyCoeffMod(
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
        negacyclicShiftPolyCoeffMod(temp, coeffModulusSize, monoExponent, modulus, result);

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
