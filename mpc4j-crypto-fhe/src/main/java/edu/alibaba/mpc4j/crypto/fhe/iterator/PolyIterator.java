package edu.alibaba.mpc4j.crypto.fhe.iterator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * PolyIterator represents multiple (m >= 1) degree-(N-1) RNS representations.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L1304
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class PolyIterator {
    /**
     * The coefficient for the r-th RNS representation is coeff[offset + r * N * k + 0 .. offset + r * N * k + N * k).
     */
    public final long[] coeff;
    /**
     * offset
     */
    public final int offset;
    /**
     * m, i.e., the number of RNS representations.
     */
    private final int m;
    /**
     * k, i.e., the number of RNS bases.
     */
    public final int k;
    /**
     * N, i.e., modulus polynomial degree.
     */
    public final int n;
    /**
     * RNS iterators
     */
    public final RnsIterator[] rnsIterators;

    /**
     * Creates a poly iterator.
     *
     * @param coeff  the coefficient.
     * @param offset the offset.
     * @param m      m, i.e., the number of RNS representations.
     * @param n      N, i.e., the modulus polynomial degree.
     * @param k      k, i.e., the number of RNS bases.
     */
    public PolyIterator(long[] coeff, int offset, int m, int n, int k) {
        assert coeff.length >= offset + m * n * k;
        this.coeff = coeff;
        this.offset = offset;
        this.m = m;
        this.n = n;
        this.k = k;
        rnsIterators = new RnsIterator[m];
        for (int r = 0; r < m; r++) {
            int subOffset = offset + r * n * k;
            rnsIterators[r] = new RnsIterator(coeff, subOffset, n, k);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolyIterator)) {
            return false;
        }
        PolyIterator that = (PolyIterator) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.m, that.m);
        equalsBuilder.append(this.n, that.n);
        equalsBuilder.append(this.k, that.k);
        // add coefficients
        for (int i = 0; i < m * n * k; i++) {
            equalsBuilder.append(this.coeff[this.offset + i], that.coeff[that.offset + i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(m);
        hashCodeBuilder.append(n);
        hashCodeBuilder.append(k);
        // add coefficients
        for (int i = 0; i < m * n * k; i++) {
            hashCodeBuilder.append(coeff[offset + i]);
        }
        return hashCodeBuilder.hashCode();
    }
}
