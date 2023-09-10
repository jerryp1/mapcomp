package edu.alibaba.mpc4j.crypto.fhe.iterator;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsContext;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.*;
import java.util.function.Consumer;

/**
 * Represent a degree-N poly in RNS representation.
 * A degree-N poly has N coeffs. Supposing that RnsBase is q = [q1, q2, ..., qk],
 * then, each coeff will be spilt into k parts. So, we can use a k * N matrix represent a degree-N poly under RNS representation.
 * Such as :
 * [ c1 mod q1, c2 mod q1,  ..., cn mod q1]
 * .......
 * [c1 mod qk, c2 mod qk, ....., cn mod qk]
 *
 * But remember, most of the time, we use this matrix column by column,
 * which is what we often call this matrix: [c1 mod q1, c1 mod q2, ..., c1 mod qk]^T
 *
 * @author Qixian Zhou
 * @date 2023/8/20
 */
public class RnsIter implements Iterator{

    // use long[][] represent CoeffIter in SEAL, k * N
    // a single CoeffIter is long[] with length k, can treat as a column vector
    private long[][] coeffIter;

    // k
    private int coeffModulusSize;

    // N
    private int polyModulusDegree;

    // inner pos
    private int pos;

    public RnsIter(long[][] coeffIter, int polyModulusDegree) {
        assert coeffIter[0].length == polyModulusDegree;

        this.coeffIter = coeffIter;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = coeffIter.length;

        this.pos = 0;
    }

    public RnsIter(long[] coeff, int polyModulusDegree) {
        assert coeff.length == polyModulusDegree;

        this.coeffIter = new long[1][polyModulusDegree];
        coeffIter[0] = coeff;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = 1;

        this.pos = 0;
    }


    /**
     * update this object inner coeffIter = (k1 + k2) * N
     *
     * @param coeffIter1 k1 * N
     * @param coeffIter2 k2 * N
     */
    public void update(long[][] coeffIter1, long[][] coeffIter2) {

        assert coeffIter1.length + coeffIter2.length == coeffIter.length;
        assert coeffIter1[0].length == coeffIter2[0].length;
        assert coeffIter1[0].length == polyModulusDegree;

        // copy 1
        for (int i = 0; i < coeffIter1.length; i++) {
            System.arraycopy(coeffIter1[i], 0, coeffIter[i], 0, polyModulusDegree);
        }
        // copy 2
        for (int i = 0; i < coeffIter2.length; i++) {
            System.arraycopy(coeffIter2[i], 0, coeffIter[i + coeffIter1.length], 0, polyModulusDegree);
        }
    }


    /**
     *   k  * N matrix
     * + k1 * N matrix
     *   (k + k1) * N
     *
     * @param otherCoeffIter k_1 * N matrix
     */
    public void extend(long[][] otherCoeffIter) {

        assert otherCoeffIter[0].length == polyModulusDegree;

        long[][] newCoeffIter = new long[otherCoeffIter.length + coeffModulusSize][polyModulusDegree];

        // copy original
        for (int i = 0; i < coeffIter.length; i++) {
            System.arraycopy(coeffIter[i], 0, newCoeffIter[i], 0, polyModulusDegree);
        }
        // copy other
        for (int i = 0; i < otherCoeffIter.length; i++) {
            System.arraycopy(otherCoeffIter[i], 0, newCoeffIter[coeffIter.length + i], 0, polyModulusDegree);
        }
        // update current object
        this.coeffIter = newCoeffIter;
        this.coeffModulusSize = newCoeffIter.length;
    }

    public void extend(RnsIter otherRnsIter) {

        assert otherRnsIter.polyModulusDegree == polyModulusDegree;

        long[][] newCoeffIter = new long[otherRnsIter.coeffModulusSize + coeffModulusSize][polyModulusDegree];

        // copy original
        for (int i = 0; i < coeffIter.length; i++) {
            System.arraycopy(coeffIter[i], 0, newCoeffIter[i], 0, polyModulusDegree);
        }
        // copy other
        long[][] otherCoeffIter = otherRnsIter.getCoeffIter();
        for (int i = 0; i < otherCoeffIter.length; i++) {
            System.arraycopy(otherCoeffIter[i], 0, newCoeffIter[coeffIter.length + i], 0, polyModulusDegree);
        }
        // update current object
        this.coeffIter = newCoeffIter;
        this.coeffModulusSize = newCoeffIter.length;
    }

    /**
     *
     * @param startIndex satrtIndex of CoeffIter
     * @param endIndex end index of CoeffIter
     * @return a new RnsIter object with coeffIter[startIndex, endIndex - 1]
     */
    public RnsIter subIter(int startIndex, int endIndex) {
        assert endIndex > startIndex;
        assert endIndex <= coeffModulusSize;

        long[][] newCoeffIter = new long[endIndex - startIndex][polyModulusDegree];
        // copy
        for (int i = startIndex; i < endIndex; i++) {
            System.arraycopy(coeffIter[i], 0, newCoeffIter[i - startIndex], 0, polyModulusDegree);
        }

        return new RnsIter(newCoeffIter, polyModulusDegree);
    }

    /**
     *
     * @param startIndex start index
     * @return a new RnsIter object with coeffIter[startIndex, rnsBase - 1]
     */
    public RnsIter subIter(int startIndex) {

        assert startIndex < coeffModulusSize;

        long[][] newCoeffIter = new long[coeffModulusSize - startIndex][polyModulusDegree];
        // copy
        for (int i = startIndex; i < coeffModulusSize; i++) {
            System.arraycopy(coeffIter[i], 0, newCoeffIter[i - startIndex], 0, polyModulusDegree);
        }

        return new RnsIter(newCoeffIter, polyModulusDegree);
    }




    public RnsIter() {}

    /**
     * An empty RnsIter with size of  k * N
     * @param coeffModulusSize k
     * @param coeffCount N
     */
    public RnsIter(int coeffModulusSize, int coeffCount) {
        coeffIter = new long[coeffModulusSize][coeffCount];
        this.polyModulusDegree = coeffCount;
        this.coeffModulusSize = coeffModulusSize;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RnsIter) {
            RnsIter that = (RnsIter) obj;
            return new EqualsBuilder()
                    .append(this.coeffIter, that.coeffIter )
                    .append(this.polyModulusDegree, that.polyModulusDegree)
                    .isEquals();
        }
        return false;
    }


    public int getCoeffModulusSize() {
        return coeffModulusSize;
    }

    /**
     * @return coeffIter.length
     */
    public int getCoeffIterCount() {
        return coeffIter.length;
    }

    public void setCoeffIter(long[][] coeffIter) {
        this.coeffIter = coeffIter;
    }

    public void setCoeffIter(long[] coeff, int index) {
        this.coeffIter[index] = coeff;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(polyModulusDegree);
        result = 31 * result + Arrays.hashCode(coeffIter);
        return result;
    }

    /**
     *
     * @return the size of the RnsBase to which the current RnsIter belongs
     */
    public int getRnsBaseSize() {
        return coeffModulusSize;
    }

    public long[][] getCoeffIter() {
        return coeffIter;
    }

    /**
     *
     * @param index i, range of [0, k), k is the RnsBase
     * @return the i-th row of k * N matrix, just the coeffs mod q_i
     */
    public long[] getCoeffIter(int index) {
        return coeffIter[index];
    }


    /**
     *
     * @param rowIndex row
     * @param colIndex col
     * @return coeffIter[i][j] = c_j mod q_i
     */
    public long getCoeff(int rowIndex, int colIndex) {
        return coeffIter[rowIndex][colIndex];
    }

    /**
     *  coeffIter[rowIndex][colIndex] = value;
     * @param rowIndex i
     * @param colIndex j
     * @param value value
     */
    public void setCoeff(int rowIndex, int colIndex, long value) {
        coeffIter[rowIndex][colIndex] = value;
    }



    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = 0;
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return pos < coeffIter.length;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public long[] next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        long[] temp = coeffIter[pos];
        pos++;
        return temp;
    }

    /**
     * Performs the given action for each remaining element until all elements
     * have been processed or the action throws an exception.  Actions are
     * performed in the order of iteration, if that order is specified.
     * Exceptions thrown by the action are relayed to the caller.
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @implSpec <p>The default implementation behaves as if:
     * <pre>{@code
     *     while (hasNext())
     *         action.accept(next());
     * }</pre>
     * @since 1.8
     */
    @Override
    public void forEachRemaining(Consumer action) {
        while (hasNext()) {
            action.accept(next());
        }
    }
}
