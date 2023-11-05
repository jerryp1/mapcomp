package edu.alibaba.mpc4j.crypto.fhe.iterator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * RnsIterator represents k degree-(N-1) polynomials in RNS representation. A degree-(N-1) polynomial has N coefficients.
 * Suppose RNS base is q = [q1, q2, ..., qk]. Each coefficient can be spilt into k parts. Therefore, we use 1D array
 * the length at least k * N to represent k degree-(N-1) polynomials in RNS representation with the following form:
 * <p>
 * [ c_11 mod q1, c_12 mod q1, ..., c_1n mod q1]
 * </p>
 * <p>
 * ...
 * </p>
 * <p>
 * [ c_k1 mod qk, c_k2 mod qk, ..., c_kn mod qk]
 * </p>
 * But most of the time, we use this matrix via column, i.e., operate on [c_1i mod q1, c_2i mod q2, ..., cki mod qk]^T.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L951
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class RnsIterator {
    /**
     * The coefficient for the j-th degree-(N-1) polynomial is coeff[offset + j * N + 0 .. offset + j * N + N).
     */
    public final long[] coeff;
    /**
     * offset
     */
    public final int offset;
    /**
     * k, i.e., the number of RNS bases.
     */
    public final int k;
    /**
     * N, i.e., modulus polynomial degree.
     */
    public final int n;
    /**
     * coefficient iterators
     */
    public final CoeffIterator[] coeffIterators;

    /**
     * Creates an RNS iterator.
     *
     * @param coeff  the coefficient.
     * @param offset the offset.
     * @param n      N, i.e., the modulus polynomial degree.
     * @param k      k, i.e., the number of RNS bases.
     */
    public RnsIterator(long[] coeff, int offset, int n, int k) {
        assert coeff.length >= offset + n * k;
        this.coeff = coeff;
        this.offset = offset;
        this.n = n;
        this.k = k;
        coeffIterators = new CoeffIterator[k];
        for (int j = 0; j < k; j++) {
            int subOffset = offset + j * n;
            coeffIterators[j] = new CoeffIterator(coeff, subOffset, n);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RnsIterator)) {
            return false;
        }
        RnsIterator that = (RnsIterator) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.n, that.n);
        equalsBuilder.append(this.k, that.k);
        // add coefficients
        for (int i = 0; i < n * k; i++) {
            equalsBuilder.append(this.coeff[this.offset + i], that.coeff[that.offset + i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(n);
        hashCodeBuilder.append(k);
        // add coefficients
        for (int i = 0; i < n * k; i++) {
            hashCodeBuilder.append(coeff[offset + i]);
        }
        return hashCodeBuilder.hashCode();
    }
}
