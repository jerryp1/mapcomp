package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.utils.DynArray;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.exception.OutOfRangeException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/9/2
 */
public class Plaintext implements Cloneable {

    private ParmsIdType parmsId = ParmsIdType.parmsIdZero();

    private int coeffCount = 0;

    private double scale = 1.0;

    // todo: must use DynArray?
    // 始终只有 1个 Poly, 即 size = 1, 即使这个 poly 可能是 RNS base， 即 k * N
    private DynArray data;


    public Plaintext() {
        data = new DynArray();
    }

    public Plaintext(int coeffCount) {
        this.coeffCount = coeffCount;
        this.data = new DynArray(coeffCount);
    }

    public Plaintext(int capacity, int coeffCount) {
        this.coeffCount = coeffCount;
        this.data = new DynArray(capacity, coeffCount);
    }

    public Plaintext(long[] coeffs, int capacity) {
        this.coeffCount = coeffs.length;
        this.data = new DynArray(coeffs, capacity);
    }

    public Plaintext(long[] coeffs) {
        this.coeffCount = coeffs.length;
        this.data = new DynArray(coeffs);
    }

    /**
     * deep-copy a Plaintext object
     *
     * @param copy another Plaintext object
     */
    public Plaintext(Plaintext copy) {

        this.coeffCount = copy.coeffCount;
        this.parmsId = new ParmsIdType(copy.parmsId);
        this.scale = copy.scale;
        this.data = new DynArray(copy.data);
    }

//    public Plaintext(String hexPoly) {
//
//        if (isNttForm()) {
//            throw new RuntimeException("cannot set an NTT transformed Plaintext");
//        }
//
//        if (Common.unsignedGt(hexPoly.length(), Integer.MAX_VALUE)) {
//            throw new IllegalArgumentException("hex_poly too long");
//        }
//
//        int length = hexPoly.length();
//        // Determine size needed to store string coefficient.
//        int assignCoeffCount = 0;
//
//        int assignCoeffBitCount = 0;
//
//        int pos = 0;
//        int lastPower =
//
//    }


    public void reserve(int capacity) {

        if (isNttForm()) {
            throw new RuntimeException("cannot reserve for an NTT transformed Plaintext");
        }

        data.reserve(capacity);
        coeffCount = data.size();
    }

    public void shrinkToFit() {
        data.shrinkToFit();
    }

    public void release() {

        parmsId = ParmsIdType.parmsIdZero();
        coeffCount = 0;
        scale = 1.0;
        data.release();
    }


    public void resize(int coeffCount) {

        if (isNttForm()) {
            throw new RuntimeException("cannot resize for an NTT transformed Plaintext");
        }
        data.resize(coeffCount);
        this.coeffCount = coeffCount;
    }


//    public void set(String hexPoly) {
//
//    }


    public void set(int index, long coeff) {
        data.set(index, coeff);
    }


    public void set(long constCoeff) {

        data.resize(1);
        data.set(0, constCoeff);
        coeffCount = 1;
        parmsId = ParmsIdType.parmsIdZero();
    }

    public void set(long[] coeffs) {
        data = new DynArray(coeffs);
        coeffCount = coeffs.length;
        parmsId = ParmsIdType.parmsIdZero();
    }

    public long get(int index) {
        return data.at(index);
    }


    public void setZero(int startCoeff, int length) {

        if (length <= 0) {
            return;
        }

        if (startCoeff + length - 1 >= coeffCount) {
            throw new IndexOutOfBoundsException("length must be non-negative and start_coeff + length - 1 must be within [0, coeff_count)");
        }
        data.setZero(startCoeff, length);
    }

    public void setZero(int startCoeff) {

        if (startCoeff >= coeffCount) {
            throw new IndexOutOfBoundsException("start_coeff must be within [0, coeff_count)");
        }

        data.setZero(startCoeff);
    }

    public void setZero() {
        data.setZero();
    }

    public DynArray getDynArray() {
        return data;
    }

    public long[] getData() {
        return data.data();
    }

    public long getData(int coeffIndex) {

        if (coeffCount == 0) {
            throw new RuntimeException();
        }

        if (coeffIndex >= coeffCount) {
            throw new IndexOutOfBoundsException("coeff_index must be within [0, coeff_count)");
        }

        return data.at(coeffIndex);
    }

    public boolean isZero() {
        return (coeffCount == 0) || data.isZero();
    }


    public int getCapacity() {
        return data.capacity();
    }

    public int getCoeffCount() {
        return coeffCount;
    }

    /**
     *
     * @return the significant coefficient count of the current plaintext polynomial.
     */
    public int significantCoeffCount() {

        if (coeffCount == 0) {
            return 0;
        }
        return UintCore.getSignificantUint64CountUint(data.data(), coeffCount);
    }


    public int nonZeroCoeffCount() {

        if (coeffCount == 0) {
            return 0;
        }

        return UintCore.getNonZeroUint64CountUint(data.data(), coeffCount);
    }


    public ParmsIdType getParmsId() {
        return parmsId;
    }

    public void setParmsId(long[] parmsId) {
        this.parmsId.set(parmsId);
    }

    public void setParmsId(ParmsIdType parmsId) {
        this.parmsId = new ParmsIdType(parmsId);
    }


    public double scale() {
        return scale;
    }


    /**
     * @return Returns whether the plaintext is in NTT form.
     */
    public boolean isNttForm() {

        return !parmsId.equals(ParmsIdType.PARMS_ID_ZERO);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Plaintext)) return false;

        Plaintext that = (Plaintext) o;

        int sigCoeffCount = this.significantCoeffCount();
        int sigCoeffCountThat = that.significantCoeffCount();

        if (sigCoeffCount != sigCoeffCountThat) {
            return false;
        }

        // if both is ntt form, then compare parmsId
        boolean parmsIdCompare = (isNttForm() && that.isNttForm() && (parmsId.equals(that.parmsId))) || (
                !isNttForm() && !that.isNttForm());

        if (!parmsIdCompare) {
            return false;
        }

        // data equal
        // 1. [0, sigCoeffCount) must be equal
        // 2. [sigCoeffCount, ..) should be zero
        boolean b1 = Arrays.equals(
                Arrays.copyOfRange(this.data.data(), 0, sigCoeffCount),
                Arrays.copyOfRange(that.data.data(), 0, sigCoeffCountThat)
        );
        if (!b1) {
            return false;
        }
        //
        boolean b2 = Arrays.stream(Arrays.copyOfRange(this.data.data(), sigCoeffCount, this.data.size())).allMatch(n -> n == 0);
        boolean b3 = Arrays.stream(Arrays.copyOfRange(that.data.data(), sigCoeffCountThat, that.data.size())).allMatch(n -> n == 0);

        return b2 && b3;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(parmsId).append(coeffCount).append(scale).append(data).toHashCode();
    }

    @Override
    public String toString() {
        return "Plaintext{" +
                "parmsId=" + parmsId +
                ", coeffCount=" + coeffCount +
                ", scale=" + scale +
                ", data=" + data +
                '}';
    }

    @Override
    public Plaintext clone() {
        try {
            Plaintext clone = (Plaintext) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.parmsId = this.parmsId.clone();
            clone.data = this.data.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
