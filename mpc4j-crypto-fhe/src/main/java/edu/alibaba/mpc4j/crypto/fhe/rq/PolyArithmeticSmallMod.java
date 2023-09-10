package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.iterator.PolyIter;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/8/20
 */
public class PolyArithmeticSmallMod {


    public static void addPolyCoeffMod(long[] operand1, long[] operand2, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert Arrays.stream(operand1).parallel().allMatch(n -> n < modulus.getValue());
        assert Arrays.stream(operand2).parallel().allMatch(n -> n < modulus.getValue());

        long modulusValue = modulus.getValue();

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
                    long sum = operand1[i] + operand2[i];
                    result[i] = sum >= modulusValue ? sum - modulusValue : sum;
                }
        );

    }

    public static void addPolyCoeffMod(RnsIter operand1, RnsIter operand2, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert operand1.getPolyModulusDegree() == result.getPolyModulusDegree();
        assert operand2.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> addPolyCoeffMod(
                        operand1.getCoeffIter(i),
                        operand2.getCoeffIter(i),
                        polyModulusDegree,
                        modulus[i],
                        result.getCoeffIter(i)
                )
        );
    }

    public static void addPolyCoeffMod(PolyIter operand1, PolyIter operand2, int size, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert operand1.getCoeffModulusSize() == result.getCoeffModulusSize();
        assert operand2.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> {
                    addPolyCoeffMod(operand1.getRnsIter(i), operand2.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
                }
        );
    }


    /**
     * compute (poly[i] * scalar) mod modulus and store in result.
     *
     * @param poly
     * @param coeffCount
     * @param scalar
     * @param modulus
     * @param result
     */
    public static void multiplyPolyScalarCoeffMod(long[] poly,
                                                  int coeffCount,
                                                  MultiplyUintModOperand scalar,
                                                  Modulus modulus,
                                                  long[] result) {

        assert coeffCount > 0;

        // poly[i] * scalar mod moudlus
        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[i] = UintArithmeticSmallMod.multiplyUintMod(poly[i], scalar, modulus)
        );
    }


    public static void multiplyPolyScalarCoeffMod(long[] poly,
                                                  int coeffCount,
                                                  long scalar,
                                                  Modulus modulus,
                                                  long[] result) {

        assert coeffCount > 0;

        // convert scalar to Multi
        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus), modulus);

        multiplyPolyScalarCoeffMod(poly, coeffCount, tempScalar, modulus, result);
    }

    /**
     * @param poly             input polly in Rns
     * @param coeffModulusSize N
     * @param scalar           scalar
     * @param modulus          modulus
     * @param result           (poly * scalar) mod mosulus
     */
    public static void multiplyPolyScalarCoeffMod(RnsIter poly,
                                                  int coeffModulusSize,
                                                  long scalar,
                                                  Modulus[] modulus,
                                                  RnsIter result
    ) {
        assert coeffModulusSize > 0;
        assert coeffModulusSize == modulus.length;

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> multiplyPolyScalarCoeffMod(
                        poly.getCoeffIter(i),
                        poly.getPolyModulusDegree(),
                        scalar,
                        modulus[i],
                        result.getCoeffIter(i) // will be over write
                )
        );
    }


    public static void multiplyPolyScalarCoeffMod(PolyIter poly,
                                                  int size,
                                                  long scalar,
                                                  Modulus[] modulus,
                                                  PolyIter result) {
        assert size > 0;
        IntStream.range(0, size).parallel().forEach(
                i -> multiplyPolyScalarCoeffMod(
                        poly.getRnsIter(i),
                        poly.getCoeffModulusSize(),
                        scalar,
                        modulus,
                        result.getRnsIter(i)
                ));
    }

    /**
     * @param poly
     * @param coeffCount
     * @param scalar
     * @param modulus
     * @param result
     */
    public static void addPolyScalarCoeffMod(long[] poly, int coeffCount, long scalar, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert scalar < modulus.getValue();
        assert poly.length == coeffCount;
        assert result.length == coeffCount;

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[i] = UintArithmeticSmallMod.addUintMod(poly[i], scalar, modulus)
        );
    }

    public static void addPolyScalarCoeffMod(RnsIter poly, int coeffModulusSize, long scalar, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> addPolyScalarCoeffMod(poly.getCoeffIter(i), polyModulusDegree, scalar, modulus[i], result.getCoeffIter(i))
        );
    }

    /**
     * @param poly    a PolyIter object
     * @param size
     * @param scalar
     * @param modulus
     * @param result
     */
    public static void addPolyScalarCoeffMod(PolyIter poly, int size, long scalar, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> addPolyScalarCoeffMod(poly.getRnsIter(i), coeffModulusSize, scalar, modulus, result.getRnsIter(i))
        );

    }

    /**
     * @param poly
     * @param coeffCount
     * @param modulus
     * @param result     (poly mod modulus)
     */
    public static void moduloPolyCoeffs(long[] poly, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[i] = UintArithmeticSmallMod.barrettReduce64(poly[i], modulus)
        );
    }

    public static void moduloPolyCoeffs(RnsIter poly, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = poly.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> moduloPolyCoeffs(poly.getCoeffIter(i), polyModulusDegree, modulus[i], result.getCoeffIter(i))
        );
    }

    public static void moduloPolyCoeffs(PolyIter polyArray, int size, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert polyArray.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = polyArray.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> moduloPolyCoeffs(polyArray.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i))
        );
    }


    public static void subPolyScalarCoeffMod(long[] poly, int coeffCount, long scalar, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert scalar < modulus.getValue();

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[i] = UintArithmeticSmallMod.subUintMod(poly[i], scalar, modulus)
        );
    }


    public static void subPolyScalarCoeffMod(RnsIter poly, int coeffModulusSize, long scalar, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = poly.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> subPolyScalarCoeffMod(poly.getCoeffIter(i), polyModulusDegree, scalar, modulus[i], result.getCoeffIter(i))
        );
    }

    public static void subPolyScalarCoeffMod(PolyIter poly, int size, long scalar, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = poly.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> subPolyScalarCoeffMod(poly.getRnsIter(i), coeffModulusSize, scalar, modulus, result.getRnsIter(i))
        );
    }


    /**
     * @param operand1
     * @param operand2
     * @param coeffCount
     * @param modulus
     * @param result     (operand1 - operand2) mod modulus
     */
    public static void subPolyCoeffMod(long[] operand1, long[] operand2, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert Arrays.stream(operand1).parallel().allMatch(n -> n < modulus.getValue());
        assert Arrays.stream(operand2).parallel().allMatch(n -> n < modulus.getValue());


        long modulusValue = modulus.getValue();

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
                    long[] tempResult = new long[1];
                    long borrow = UintArithmetic.subUint64(operand1[i], operand2[i], tempResult);
                    // borrow = 0 ---> result[i] = tempResult[0]
                    // borrow = 1 ---> result[i] = tempResult[0] + modulusValue
                    result[i] = tempResult[0] + (modulusValue & (-borrow));
                }
        );
    }


    public static void subPolyCoeffMod(RnsIter operand1, RnsIter operand2, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert operand1.getPolyModulusDegree() == result.getPolyModulusDegree();
        assert operand2.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();


        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    subPolyCoeffMod(operand1.getCoeffIter(i), operand2.getCoeffIter(i), polyModulusDegree, modulus[i], result.getCoeffIter(i));
                }
        );
    }

    public static void subPolyCoeffMod(PolyIter operand1, PolyIter operand2, int size, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert operand1.getCoeffModulusSize() == result.getCoeffModulusSize();
        assert operand2.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();


        IntStream.range(0, size).parallel().forEach(
                i -> {
                    subPolyCoeffMod(operand1.getRnsIter(i), operand2.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
                }
        );
    }


    public static void dyadicProductCoeffMod(long[] operand1, long[] operand2, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.getValue();
        long constRation0 = modulus.getConstRatio()[0];
        long constRation1 = modulus.getConstRatio()[1];

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
                    long[] z = new long[2];
                    long tmp3, carry;
                    long[] tmp1 = new long[1];
                    long[] tmp2 = new long[2];
                    // Reduces z using base 2^64 Barrett reduction
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
        );
    }

    public static void dyadicProductCoeffMod(RnsIter operand1, RnsIter operand2, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert operand1.getPolyModulusDegree() == result.getPolyModulusDegree();
        assert operand2.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> dyadicProductCoeffMod(
                        operand1.getCoeffIter(i),
                        operand2.getCoeffIter(i),
                        polyModulusDegree,
                        modulus[i],
                        result.getCoeffIter(i)
                )
        );
    }

    public static void dyadicProductCoeffMod(PolyIter operand1, PolyIter operand2, int size, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert operand1.getCoeffModulusSize() == result.getCoeffModulusSize();
        assert operand2.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> {
                    dyadicProductCoeffMod(operand1.getRnsIter(i), operand2.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i));
                }
        );
    }

    public static long polyInftyNormCoeffMod(long[] operand, int coeffCount, Modulus modulus) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        // Construct negative threshold (first negative modulus value) to compute absolute values of coeffs.
        // (p + 1)/2
        long modulusNegThreshold = (modulus.getValue() + 1) >>> 1;

        // Mod out the poly coefficients and choose a symmetric representative from
        // [-modulus,modulus). Keep track of the max.
        long[] result = new long[coeffCount];
        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
                    long polyCoeff = UintArithmeticSmallMod.barrettReduce64(operand[i], modulus);
                    if (polyCoeff >= modulusNegThreshold) {
                        polyCoeff = modulus.getValue() - polyCoeff;
                    }
                    result[i] = polyCoeff;
                }
        );

        return Arrays.stream(result).max().getAsLong();
    }


    public static void negatePolyCoeffMod(long[] poly, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert Arrays.stream(poly).allMatch(n -> n < modulus.getValue());

        long modulusValue = modulus.getValue();
        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
                    long nonZero = poly[i] != 0 ? 1 : 0;
                    // 0 ---> & 0 = 0
                    // 1 ---> & -1 = & 0xFFFFFFF
                    result[i] = (modulusValue - poly[i]) & (-nonZero);
                }
        );
    }

    public static void negatePolyCoeffMod(RnsIter poly, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = poly.getPolyModulusDegree();
        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> negatePolyCoeffMod(poly.getCoeffIter(i), polyModulusDegree, modulus[i], result.getCoeffIter(i))
        );
    }

    public static void negatePolyCoeffMod(PolyIter poly, int size, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = poly.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> negatePolyCoeffMod(poly.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i))
        );
    }


    public static void negAcyclicShiftPolyCoeffMod(long[] poly, int coeffCount, int shift, Modulus modulus, long[] result) {


        assert poly != result;
        assert !modulus.isZero();
        assert UintCore.getPowerOfTwo(coeffCount) >= 0;
        assert shift < coeffCount;
        // Nothing to do
        if (shift == 0) {
            UintCore.setUint(poly, coeffCount, result);
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
        assert coeffModulusSize == modulus.length;

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> negAcyclicShiftPolyCoeffMod(poly.getCoeffIter(i),
                        poly.getPolyModulusDegree(),
                        shift,
                        modulus[i],
                        result.getCoeffIter(i) // will be over write
                )
        );
    }

    public static void negAcyclicShiftPolyCoeffMod(PolyIter poly,
                                                   int size,
                                                   int shift,
                                                   Modulus[] modulus,
                                                   PolyIter result) {
        assert size > 0;
        assert poly.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> negAcyclicShiftPolyCoeffMod(
                        poly.getRnsIter(i),
                        coeffModulusSize,
                        shift,
                        modulus,
                        result.getRnsIter(i)
                ));
    }


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


    public static void negAcyclicMultiplyPolyMonoCoeffMod(
            RnsIter poly, int coeffModulusSize, long monoCoeff, int monoExponent, Modulus[] modulus, RnsIter result
    ) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();
        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    negAcyclicMultiplyPolyMonoCoeffMod(
                            poly.getCoeffIter(i),
                            polyModulusDegree,
                            monoCoeff,
                            monoExponent,
                            modulus[i],
                            result.getCoeffIter(i)
                    );
                }
        );
    }


    public static void negAcyclicMultiplyPolyMonoCoeffMod(
            PolyIter polyArray, int size, long monoCoeff, int monoExponent, Modulus[] modulus, PolyIter result
    ) {

        assert size > 0;
        assert polyArray.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();
        IntStream.range(0, size).parallel().forEach(
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

    public static void negAcyclicMultiplyPolyMonoCoeffMod(
            RnsIter poly, int coeffModulusSize, long[] monoCoeff, int monoExponent, Modulus[] modulus, RnsIter result
    ) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();
        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    negAcyclicMultiplyPolyMonoCoeffMod(
                            poly.getCoeffIter(i),
                            polyModulusDegree,
                            monoCoeff[i],
                            monoExponent,
                            modulus[i],
                            result.getCoeffIter(i)
                    );
                }
        );
    }

    public static void negAcyclicMultiplyPolyMonoCoeffMod(
            PolyIter polyArray, int size, long[] monoCoeff, int monoExponent, Modulus[] modulus, PolyIter result
    ) {

        assert size > 0;
        assert polyArray.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();
        IntStream.range(0, size).parallel().forEach(
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




}
