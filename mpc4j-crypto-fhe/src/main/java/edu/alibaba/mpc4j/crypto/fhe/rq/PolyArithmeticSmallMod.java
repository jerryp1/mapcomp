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

    /**
     * result[resultStartIndex, resultStartIndex + N) = operand1Array[startIndex1, startIndex1 + N) + operand2Array[startIndex2, startIndex2 + N)
     *
     * @param operand1Array    single poly in RNS, lengt is k * N
     * @param startIndex1      startIndex of a single poly in operand1Array
     * @param operand2Array    single poly in RNS, lengt is k * N
     * @param startIndex2      startIndex of a single poly in operand2Array
     * @param coeffCount       N
     * @param modulus          single modulus
     * @param resultStartIndex startIndex of a single poly in result
     * @param result           single poly in RNS, length is k * N
     */
    public static void addPolyCoeffMod(long[] operand1Array, int startIndex1,
                                       long[] operand2Array, int startIndex2,
                                       int coeffCount, Modulus modulus,
                                       int resultStartIndex, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert Arrays.stream(operand1Array, startIndex1, startIndex1 + coeffCount).allMatch(n -> n < modulus.getValue());
        assert Arrays.stream(operand2Array, startIndex2, startIndex2 + coeffCount).allMatch(n -> n < modulus.getValue());


        long modulusValue = modulus.getValue();
        // 实测 大于这个的时候，并行更快
        if (coeffCount > 8192) {
            IntStream.range(0, coeffCount).parallel().forEach(
                    i -> {
                        long sum = operand1Array[startIndex1 + i] + operand2Array[startIndex2 + i];
                        result[resultStartIndex + i] = sum >= modulusValue ? sum - modulusValue : sum;
                    }
            );
        } else {
            long sum;
            for (int i = 0; i < coeffCount; i++) {
                sum = operand1Array[startIndex1 + i] + operand2Array[startIndex2 + i];
                result[resultStartIndex + i] = sum >= modulusValue ? sum - modulusValue : sum;
            }
        }
    }


    public static void addPolyCoeffModFor(long[] operand1, long[] operand2, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert Arrays.stream(operand1).allMatch(n -> n < modulus.getValue());
        assert Arrays.stream(operand2).allMatch(n -> n < modulus.getValue());

        long modulusValue = modulus.getValue();
        long sum;
        for (int i = 0; i < coeffCount; i++) {
            sum = operand1[i] + operand2[i];
            result[i] = sum >= modulusValue ? sum - modulusValue : sum;
        }
    }


    public static void addPolyCoeffMod(RnsIter operand1, RnsIter operand2, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert operand1.getPolyModulusDegree() == result.getPolyModulusDegree();
        assert operand2.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    assert !modulus[i].isZero();

                    long modulusValue = modulus[i].getValue();
                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> {
                                assert operand1.coeffIter[i * polyModulusDegree + j] < modulusValue;
                                assert operand2.coeffIter[i * polyModulusDegree + j] < modulusValue;

                                long sum = operand1.coeffIter[i * polyModulusDegree + j] + operand2.coeffIter[i * polyModulusDegree + j];
                                result.coeffIter[i * polyModulusDegree + j] = sum >= modulusValue ? sum - modulusValue : sum;
                            }
                    );
                }
        );
    }

    public static void addPolyCoeffMod(PolyIter operand1, PolyIter operand2, int size, Modulus[] modulus, PolyIter result) {

        assert size > 0;
        assert operand1.getCoeffModulusSize() == result.getCoeffModulusSize();
        assert operand2.getCoeffModulusSize() == result.getCoeffModulusSize();

        int coeffModulusSize = result.getCoeffModulusSize();

        IntStream.range(0, size).parallel().forEach(
                i -> addPolyCoeffMod(operand1.getRnsIter(i), operand2.getRnsIter(i), coeffModulusSize, modulus, result.getRnsIter(i))
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
        assert !modulus.isZero();

        // poly[i] * scalar mod moudlus
        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[i] = UintArithmeticSmallMod.multiplyUintMod(poly[i], scalar, modulus)
        );
    }


    /**
     * using startIndex and resultStartIndex  to avoid RnsIter.getCoeffIter(i)'s call
     *
     * @param poly
     * @param startIndex
     * @param coeffCount
     * @param scalar
     * @param modulus
     * @param result
     */
    public static void multiplyPolyScalarCoeffMod(long[] poly,
                                                  int startIndex,
                                                  int coeffCount,
                                                  MultiplyUintModOperand scalar,
                                                  Modulus modulus,
                                                  int resultStartIndex,
                                                  long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert startIndex % coeffCount == 0 && resultStartIndex % coeffCount == 0;

        // poly[i] * scalar mod moudlus
        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[resultStartIndex + i] = UintArithmeticSmallMod.multiplyUintMod(poly[startIndex + i], scalar, modulus)
        );
    }

    public static void multiplyPolyScalarCoeffMod(long[] poly,
                                                  int startIndex,
                                                  int coeffCount,
                                                  long scalar,
                                                  Modulus modulus,
                                                  int resultStartIndex,
                                                  long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert startIndex % coeffCount == 0 && resultStartIndex % coeffCount == 0;
        MultiplyUintModOperand tmp = new MultiplyUintModOperand();
        tmp.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus), modulus);

        // todo: 当传入的 scalar 是 long 类型的，一定要转换为 MultiplyUintModOperand 类型吗？还是不用？
        // todo: 因为 UintArithmeticSmallMod.multiplyUintMod 也是直接支持 long * long 的

        // poly[i] * scalar mod moudlus
//        IntStream.range(0, coeffCount).parallel().forEach(
//                i -> result[resultStartIndex + i] = UintArithmeticSmallMod.multiplyUintMod(poly[startIndex + i], scalar, modulus)
//        );

        for (int i = 0; i < coeffCount; i++) {
            result[resultStartIndex + i] = UintArithmeticSmallMod.multiplyUintMod(poly[startIndex + i], tmp, modulus);
        }

    }


    public static void multiplyPolyScalarCoeffMod(long[] poly,
                                                  int coeffCount,
                                                  long scalar,
                                                  Modulus modulus,
                                                  long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();

        // convert long scalar to MultiplyUintModOperand object
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

        int polyModulusDegree = poly.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    assert !modulus[i].isZero();

                    MultiplyUintModOperand curScalar = new MultiplyUintModOperand();
                    curScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus[i]), modulus[i]);

                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> result.coeffIter[i * polyModulusDegree + j]
                                    = UintArithmeticSmallMod.multiplyUintMod(poly.coeffIter[i * polyModulusDegree + j], curScalar, modulus[i])
                    );
                }
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

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[i] = UintArithmeticSmallMod.addUintMod(poly[i], scalar, modulus)
        );
    }

    public static void addPolyScalarCoeffMod(long[] poly, int startIndex, int coeffCount, long scalar, Modulus modulus, int resultStartIndex, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert scalar < modulus.getValue();

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[resultStartIndex + i] = UintArithmeticSmallMod.addUintMod(poly[startIndex + i], scalar, modulus)
        );
    }


    public static void addPolyScalarCoeffMod(RnsIter poly, int coeffModulusSize, long scalar, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    assert !modulus[i].isZero();

                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> result.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.addUintMod(poly.coeffIter[i * polyModulusDegree + j], scalar, modulus[i])
                    );
                }

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

    public static void moduloPolyCoeffs(long[] poly, int startIndex, int coeffCount, Modulus modulus, int resultIndex, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> result[resultIndex + i] = UintArithmeticSmallMod.barrettReduce64(poly[startIndex + i], modulus)
        );
    }


    public static void moduloPolyCoeffs(RnsIter poly, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = poly.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    assert !modulus[i].isZero();
                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> result.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.barrettReduce64(poly.coeffIter[i * polyModulusDegree + j], modulus[i])
                    );
                }
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
                i -> {
                    assert !modulus[i].isZero();
                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> result.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.subUintMod(poly.coeffIter[i * polyModulusDegree + j], scalar, modulus[i])
                    );
                }

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

    public static void subPolyCoeffMod(long[] operand1, int startIndex1, long[] operand2, int startIndex2, int coeffCount, Modulus modulus, int resultStartIndex, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();


        long modulusValue = modulus.getValue();

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {

                    assert operand1[startIndex1 + i] < modulusValue;
                    assert operand2[startIndex2 + i] < modulusValue;

                    long[] tempResult = new long[1];
                    long borrow = UintArithmetic.subUint64(operand1[startIndex1 + i], operand2[startIndex2 + i], tempResult);
                    // borrow = 0 ---> result[i] = tempResult[0]
                    // borrow = 1 ---> result[i] = tempResult[0] + modulusValue
                    result[resultStartIndex + i] = tempResult[0] + (modulusValue & (-borrow));
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
                    assert !modulus[i].isZero();
                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> {
                                assert operand1.coeffIter[i * polyModulusDegree + j] < modulus[i].getValue();
                                assert operand2.coeffIter[i * polyModulusDegree + j] < modulus[i].getValue();

                                long[] temp = new long[1];
                                long borrow = UintArithmetic.subUint64(operand1.coeffIter[i * polyModulusDegree + j], operand2.coeffIter[i * polyModulusDegree + j], temp);
                                result.coeffIter[i * polyModulusDegree + j] = temp[0] + (modulus[i].getValue() & (-borrow));
                            }
                    );
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

    /**
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

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
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
        );
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

        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
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
        );
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
    public static void dyadicProductCoeffModRnsIter(long[] polyIter1, int startIndex1, long[] polyIter2, int startIndex2, int coeffModulusSize, int coeffCount, Modulus[] modulus, int resultStartIndex, long[] resultPolyIter) {

        assert coeffCount > 0;
        assert modulus.length == coeffModulusSize;

        assert startIndex1 % (coeffCount * coeffModulusSize) == 0;
        assert startIndex2 % (coeffCount * coeffModulusSize) == 0;
        assert resultStartIndex % (coeffCount * coeffModulusSize) == 0;


        // 处理 RnsIter 下的每一个子多项式
        IntStream.range(0, coeffModulusSize).parallel().forEach(
                j -> {
                    assert !modulus[j].isZero();

                    int polyStartIndex = j * coeffCount;
                    long modulusValue = modulus[j].getValue();
                    long constRation0 = modulus[j].getConstRatio()[0];
                    long constRation1 = modulus[j].getConstRatio()[1];

                    IntStream.range(0, coeffCount).parallel().forEach(
                            i -> {
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
                    );
                }
        );
    }


    public static void dyadicProductCoeffMod(RnsIter operand1, RnsIter operand2, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert operand1.getPolyModulusDegree() == result.getPolyModulusDegree();
        assert operand2.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = result.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    assert !modulus[i].isZero();

                    long modulusValue = modulus[i].getValue();
                    long constRation0 = modulus[i].getConstRatio()[0];
                    long constRation1 = modulus[i].getConstRatio()[1];

                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> {
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
                    );
                }
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


    public static void negatePolyCoeffMod(long[] poly, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert Arrays.stream(poly).allMatch(n -> n < modulus.getValue());

        long modulusValue = modulus.getValue();
        long nonZero;
        for (int i = 0; i < coeffCount; i++) {
            nonZero = poly[i] != 0 ? 1 : 0;
            // 0 ---> & 0 = 0
            // 1 ---> & -1 = & 0xFFFFFFF
            result[i] = (modulusValue - poly[i]) & (-nonZero);
        }
        // 实测即使 coeffCount = 32768, for 也更快
//        IntStream.range(0, coeffCount).parallel().forEach(
//                i -> {
//                    long nonZero = poly[i] != 0 ? 1 : 0;
//                    // 0 ---> & 0 = 0
//                    // 1 ---> & -1 = & 0xFFFFFFF
//                    result[i] = (modulusValue - poly[i]) & (-nonZero);
//                }
//        );
    }

    /**
     * result[resultStartIndex, resultStartIndex + N) = - polyArray[startIndex, startIndex + N) mod modulus
     *
     * @param polyArray        single poly in RNS, length is k * N
     * @param startIndex       startIndex of a single poly in polyArray
     * @param coeffCount       N
     * @param modulus          single modulus
     * @param resultStartIndex startIndex of a single poly in result
     * @param result           single poly in RNS, length is k * N
     */
    public static void negatePolyCoeffMod(long[] polyArray, int startIndex, int coeffCount, Modulus modulus, int resultStartIndex, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert Arrays.stream(polyArray, startIndex, startIndex + coeffCount).allMatch(n -> n < modulus.getValue());

        long modulusValue = modulus.getValue();
        long nonZero;
        for (int i = 0; i < coeffCount; i++) {
            nonZero = polyArray[startIndex + i] != 0 ? 1 : 0;
            // 0 ---> & 0 = 0
            // 1 ---> & -1 = & 0xFFFFFFF
            result[resultStartIndex + i] = (modulusValue - polyArray[startIndex + i]) & (-nonZero);
        }
        // 实测即使 coeffCount = 32768, for 也更快
//        IntStream.range(0, coeffCount).parallel().forEach(
//                i -> {
//                    long nonZero = poly[i] != 0 ? 1 : 0;
//                    // 0 ---> & 0 = 0
//                    // 1 ---> & -1 = & 0xFFFFFFF
//                    result[i] = (modulusValue - poly[i]) & (-nonZero);
//                }
//        );
    }

    public static void negatePolyCoeffModFor(long[] poly, int coeffCount, Modulus modulus, long[] result) {

        assert coeffCount > 0;
        assert !modulus.isZero();
        assert Arrays.stream(poly).allMatch(n -> n < modulus.getValue());

        long modulusValue = modulus.getValue();
        long nonZero;
        for (int i = 0; i < coeffCount; i++) {
            nonZero = poly[i] != 0 ? 1 : 0;
            // 0 ---> & 0 = 0
            // 1 ---> & -1 = & 0xFFFFFFF
            result[i] = (modulusValue - poly[i]) & (-nonZero);
        }
    }


    public static void negatePolyCoeffMod(RnsIter poly, int coeffModulusSize, Modulus[] modulus, RnsIter result) {

        assert coeffModulusSize > 0;
        assert poly.getPolyModulusDegree() == result.getPolyModulusDegree();

        int polyModulusDegree = poly.getPolyModulusDegree();

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    assert !modulus[i].isZero();

                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> {
                                assert poly.coeffIter[i * polyModulusDegree + j] < modulus[i].getValue();

                                long nonZero = poly.coeffIter[i * polyModulusDegree + j] != 0 ? 1 : 0;
                                // 0 ---> & 0 = 0
                                // 1 ---> & -1 = & 0xFFFFFFF
                                result.coeffIter[i * polyModulusDegree + j] = (modulus[i].getValue() - poly.coeffIter[i * polyModulusDegree + j]) & (-nonZero);
                            }
                    );
                }
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

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
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

        RnsIter temp = new RnsIter(poly.getCoeffModulusSize(), poly.getPolyModulusDegree());
        // monoCoeff as Scalar
        multiplyPolyScalarCoeffMod(poly, coeffModulusSize, monoCoeff, modulus, temp);
        // then shift
        negAcyclicShiftPolyCoeffMod(temp, coeffModulusSize, monoExponent, modulus, result);

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
        // first mul
        RnsIter temp = new RnsIter(poly.getCoeffModulusSize(), poly.getPolyModulusDegree());

        IntStream.range(0, coeffModulusSize).parallel().forEach(
                i -> {
                    assert !modulus[i].isZero();
                    MultiplyUintModOperand curScalar = new MultiplyUintModOperand();
                    curScalar.set(UintArithmeticSmallMod.barrettReduce64(monoCoeff[i], modulus[i]), modulus[i]);

                    IntStream.range(0, polyModulusDegree).parallel().forEach(
                            j -> {
                                assert poly.coeffIter[i * polyModulusDegree + j] < modulus[i].getValue();

                                temp.coeffIter[i * polyModulusDegree + j] = UintArithmeticSmallMod.multiplyUintMod(
                                        poly.coeffIter[i * polyModulusDegree + j], curScalar, modulus[i]);
                            }
                    );
                }
        );
        // then neg acyclic
        negAcyclicShiftPolyCoeffMod(temp, coeffModulusSize, monoExponent, modulus, result);

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
                .parallel()
                .map(n -> UintArithmeticSmallMod.barrettReduce64(n, modulus))
                .map(n -> n >= modulusNegThreshold ? modulus.getValue() - n : n)
                .max().orElseThrow(() -> new IllegalArgumentException("operand is empty"));
    }


}
