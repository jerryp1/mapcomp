package edu.alibaba.mpc4j.crypto.fhe.iterator;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Represent a degree-(N-1) polynomial in RNS representation. A degree-(N-1) polynomial has N coefficients.
 * Suppose RnsBase is q = [q1, q2, ..., qk]. Each coefficient can be spilt into k parts. Therefore, we use k * N matrix
 * to represent a degree-(N-1) polynomial in RNS representation with the following form:
 * <p>
 * [ c1 mod q1, c2 mod q1, ..., cn mod q1]
 * </p>
 * <p>
 * ...
 * </p>
 * <p>
 * [ c1 mod qk, c2 mod qk, ..., cn mod qk]
 * </p>
 * But most of the time, we use this matrix via column, i.e., operate on [c1 mod q1, c1 mod q2, ..., c1 mod qk]^T.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L951
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/20
 */
public class RnsIter implements Cloneable {
    /**
     * Converts an RNS iterator 1d Array with lenght k*N to a 2d Array with shape k * N
     *
     * @return a 2d Array
     */
    public static long[][] to2dArray(RnsIter rnsIter) {

        return IntStream.range(0, rnsIter.coeffModulusSize)
            .mapToObj(i -> Arrays.copyOfRange(rnsIter.coeffIter, i * rnsIter.polyModulusDegree, (i + 1) * rnsIter.polyModulusDegree))
            .toArray(long[][]::new);

    }

    /**
     * a 1D-array with length k * N to represent a degree-N polynomial in RNS representation.
     */
    public long[] coeffIter;
    /**
     * k, i.e., the number of RNS bases.
     */
    public int coeffModulusSize;
    /**
     * N, i.e., modulus polynomial degree.
     */
    public int polyModulusDegree;

    /**
     * private constructor.
     */
    private RnsIter() {
        // empty
    }

    /**
     * Creates an RNS iterator with the given coefficient iterator represented in 1D-array with length k * N.
     *
     * @param coeffIter         the coefficient iterator.
     * @param polyModulusDegree N, i.e., the modulus polynomial degree.
     */
    public RnsIter(long[] coeffIter, int polyModulusDegree) {
        assert coeffIter.length % polyModulusDegree == 0;
        this.coeffIter = coeffIter;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = coeffIter.length / polyModulusDegree;
    }

    /**
     * Creates an RNS iterator with the number of RNS bases and the modulus polynomial degree. All coefficients are
     * initialized with 0.
     *
     * @param coeffModulusSize  k, i.e., the number of RNS bases.
     * @param polyModulusDegree N, i.e., the modulus polynomial degree.
     */
    public RnsIter(int coeffModulusSize, int polyModulusDegree) {
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = coeffModulusSize;
        this.coeffIter = new long[coeffModulusSize * polyModulusDegree];
    }

    /**
     * Creates an RNS iterator from an 2D-array coefficient matrix with size k * N.
     *
     * @param data an 2D-array coefficient matrix with size k * N.
     */
    public static RnsIter from2dArray(long[][] data) {
        RnsIter rnsIter = new RnsIter();
        rnsIter.coeffModulusSize = data.length;
        rnsIter.polyModulusDegree = data[0].length;
        rnsIter.coeffIter = Arrays.stream(data).flatMapToLong(Arrays::stream).toArray();
        return rnsIter;
    }

    /**
     * update this object inner coeffIter = (k1 + k2) * N
     *
     * @param coeffIter1 k1 * N
     * @param coeffIter2 k2 * N
     */
    public void update(long[] coeffIter1, long[] coeffIter2) {

        assert coeffIter1.length + coeffIter2.length == coeffIter.length;
        assert coeffIter1.length % polyModulusDegree == 0;
        assert coeffIter2.length % polyModulusDegree == 0;
        // copy k1 * N into [0, k1*N)
        System.arraycopy(coeffIter1, 0, coeffIter, 0, coeffIter1.length);
        // copy k2 * N into [k1*N, (k1 + k2) *N)
        System.arraycopy(coeffIter2, 0, coeffIter, coeffIter1.length, coeffIter2.length);
    }


    /**
     * @param startIndex start index of CoeffIter
     * @param endIndex   end index of CoeffIter
     * @return a new RnsIter object with coeffIter[startIndex * N, endIndex * N)
     */
    public RnsIter subIter(int startIndex, int endIndex) {
        assert endIndex > startIndex;
        assert endIndex <= coeffModulusSize;

        long[] newCoeffIter = new long[(endIndex - startIndex) * polyModulusDegree];
        System.arraycopy(coeffIter, startIndex * polyModulusDegree, newCoeffIter, 0, (endIndex - startIndex) * polyModulusDegree);

        return new RnsIter(newCoeffIter, polyModulusDegree);
    }

    /**
     * @param startIndex start index
     * @return a new RnsIter object with coeffIter[startIndex * N, k * N)
     */
    public RnsIter subIter(int startIndex) {

        assert startIndex < coeffModulusSize;

        long[] newCoeffIter = new long[(coeffModulusSize - startIndex) * polyModulusDegree];
        // copy
        System.arraycopy(coeffIter, startIndex * polyModulusDegree, newCoeffIter, 0, newCoeffIter.length);

        return new RnsIter(newCoeffIter, polyModulusDegree);
    }


    public int getCoeffModulusSize() {
        return coeffModulusSize;
    }

//    /**
//     * @return coeffIter.length
//     */
//    public int getCoeffIterCount() {
//        return coeffIter.length;
//    }
//
//    public void setCoeffIter(long[][] coeffIter) {
//        this.coeffIter = coeffIter;
//    }
//
//    public void setCoeffIter(long[] coeff, int index) {
//        this.coeffIter[index] = coeff;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof RnsIter)) return false;

        RnsIter rnsIter = (RnsIter) o;

        return new EqualsBuilder()
            .append(polyModulusDegree, rnsIter.polyModulusDegree)
            .append(coeffIter, rnsIter.coeffIter)
            .isEquals();
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(polyModulusDegree);
        result = 31 * result + Arrays.hashCode(coeffIter);
        return result;
    }

    /**
     * @return the size of the RnsBase to which the current RnsIter belongs
     */
    public int getRnsBaseSize() {
        return coeffModulusSize;
    }

    public long[] getCoeffIter() {
        return coeffIter;
    }

//    /**
//     * note that this method will create new array, which has poor performance
//     *
//     * @param modulusIndex [0, coeffModulusSize)
//     * @return [modulusIndex * polyModulusDegree, (modulusIndex + 1) * modulusIndex)
//     */
//    public long[] getCoeffIter(int modulusIndex) {
//
//        assert modulusIndex >= 0 && modulusIndex < coeffModulusSize;
//        long[] result = new long[polyModulusDegree];
//        System.arraycopy(coeffIter, modulusIndex * polyModulusDegree, result, 0, polyModulusDegree);
//
//        return result;
//    }


    /**
     * 感觉可能会出错啊，这里 会深拷贝一个 数组对象出去，那么这个数组出去被修改后，和原始数据就没啥关系了
     * 1D Array 就存在这种问题. 突然在想，是不是不用去返回一个新的 subArray , 根据，运算的时候 直接修改 整个大数组的特定区间即可？
     *
     * @param index range of [0, k), k is the RnsBase size
     * @return the i-th row of k * N matrix, coeffIter[index * N, (index+1) * N)
     */
//    public long[] getCoeffIter(int index) {
//
//        assert index >= 0 && index < coeffModulusSize;
//
//        return coeffIter[index];
//    }


    /**
     * @param rowIndex row
     * @param colIndex col
     * @return coeffIter[i][j] = c_j mod q_i
     */
    public long getCoeff(int rowIndex, int colIndex) {
        assert rowIndex >= 0 && rowIndex < coeffModulusSize;
        assert colIndex >= 0 && colIndex < polyModulusDegree;

        return coeffIter[rowIndex * polyModulusDegree + colIndex];
    }

    public long getCoeff(int index) {
        assert index >= 0 && index < polyModulusDegree * coeffModulusSize;

        return coeffIter[index];
    }


    /**
     * coeffIter[rowIndex][colIndex] = value;
     *
     * @param rowIndex i
     * @param colIndex j
     * @param value    value
     */
    public void setCoeff(int rowIndex, int colIndex, long value) {
        coeffIter[rowIndex * polyModulusDegree + colIndex] = value;
    }


    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    @Override
    public RnsIter clone() {
        try {
            RnsIter clone = (RnsIter) super.clone();

            clone.coeffIter = new long[this.coeffIter.length];
            System.arraycopy(this.coeffIter, 0, clone.coeffIter, 0, this.coeffIter.length);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
