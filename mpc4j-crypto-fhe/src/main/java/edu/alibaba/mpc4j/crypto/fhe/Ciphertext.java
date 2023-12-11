package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.DynArray;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Class to store a ciphertext element. The data for a ciphertext consists
 * of two or more polynomials, which are in Microsoft SEAL stored in a CRT
 * form with respect to the factors of the coefficient modulus. This data
 * itself is not meant to be modified directly by the user, but is instead
 * operated on by functions in the Evaluator class. The size of the backing
 * array of a ciphertext depends on the encryption parameters and the size
 * of the ciphertext (at least 2). If the size of the ciphertext is T,
 * the poly_modulus_degree encryption parameter is N, and the number of
 * primes in the coeff_modulus encryption parameter is K, then the
 * ciphertext backing array requires precisely 8*N*K*T bytes of memory.
 * A ciphertext also carries with it the parms_id of its associated
 * encryption parameters, which is used to check the validity of the
 * ciphertext for homomorphic operations and decryption.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/ciphertext.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/13
 */
public class Ciphertext implements Cloneable, Serializable {

    /**
     * parms ID
     */
    private ParmsIdType parmsId = ParmsIdType.parmsIdZero();
    /**
     * whether the ciphertext is in NTT form.
     */
    private boolean isNttForm = false;
    /**
     * the size of the ciphertext
     */
    private int size = 0;
    /**
     * the degree of the polynomial
     */
    private int polyModulusDegree = 0;
    /**
     * the number of primes in the coefficient modulus
     */
    private int coeffModulusSize = 0;
    /**
     * scale, only needed when using the CKKS encryption scheme
     */
    private double scale = 1.0;
    /**
     * correction factor, only needed when using the BGV encryption scheme
     */
    private long correctionFactor = 1;
    /**
     * ciphertext data
     */
    private DynArray data = new DynArray();

    /**
     * Constructs an empty ciphertext allocating no memory.
     */
    public Ciphertext() {
    }

    /**
     * Constructs an empty ciphertext with capacity 2. In addition to the capacity,
     * the allocation size is determined by the highest-level parameters associated to the given context.
     *
     * @param context the context.
     */
    public Ciphertext(Context context) {
        reserve(context, 2);
    }

    /**
     * Constructs an empty ciphertext with capacity 2. In addition to the capacity,
     * the allocation size is determined by the encryption parameters with given parms_id.
     *
     * @param context the context.
     * @param parmsId the parms_id corresponding to the encryption parameters to be used.
     */
    public Ciphertext(Context context, ParmsIdType parmsId) {
        reserve(context, parmsId, 2);
    }

    /**
     * Constructs an empty ciphertext with given capacity. In addition to the capacity,
     * the allocation size is determined by the encryption parameters with given parms_id.
     *
     * @param context      the context.
     * @param parmsId      the parms_id corresponding to the encryption parameters to be used.
     * @param sizeCapacity the capacity.
     */
    public Ciphertext(Context context, ParmsIdType parmsId, int sizeCapacity) {
        reserve(context, parmsId, sizeCapacity);
    }

    /**
     * Copies a given ciphertext to the current one.
     *
     * @param assign the ciphertext to copy from.
     */
    public void copyFrom(Ciphertext assign) {
        if (this == assign) {
            return;
        }
        // copy over fields
        // todo: need deep copy?
        this.parmsId = assign.getParmsId().clone();
        this.isNttForm = assign.isNttForm();
        this.scale = assign.scale;
        this.correctionFactor = assign.correctionFactor;
        // Then resize
        resizeInternal(assign.size, assign.polyModulusDegree, assign.coeffModulusSize);
        // copy data, 注意这里长度的计算 是以 size 为准，而不是 capacity
        System.arraycopy(assign.getData(), 0, this.getData(), 0, assign.size * assign.polyModulusDegree * assign.coeffModulusSize);
    }

    /**
     * Resets the ciphertext. This function releases any memory allocated by the ciphertext. It also sets all
     * encryption parameter specific size information to zero.
     */
    public void release() {
        parmsId = ParmsIdType.parmsIdZero();
        isNttForm = false;
        size = 0;
        polyModulusDegree = 0;
        coeffModulusSize = 0;
        scale = 1.0;
        correctionFactor = 1;
        data.release();
    }

    /**
     * Allocates enough memory to accommodate the backing array of a ciphertext with given capacity.
     * In addition to the capacity, the allocation size is determined by the highest-level parameters
     * associated to the given context.
     *
     * @param context      the context.
     * @param sizeCapacity the capacity.
     */
    public void reserve(Context context, int sizeCapacity) {
        reserve(context, context.getFirstParmsId(), sizeCapacity);
    }

    /**
     * Allocates enough memory to accommodate the backing array of a ciphertext with given capacity.
     * In addition to the capacity, the allocation size is determined by the current encryption parameters.
     *
     * @param sizeCapacity the capacity.
     */
    public void reserve(int sizeCapacity) {
        reserveInternal(sizeCapacity, polyModulusDegree, coeffModulusSize);
    }

    /**
     *  Allocates enough memory to accommodate the backing array of a ciphertext with given capacity.
     *  In addition to the capacity, the allocation size is determined by the encryption parameters corresponding
     *  to the given parms_id.
     *
     * @param context      the context.
     * @param parmsId      the parms_id corresponding to the encryption parameters to be used.
     * @param sizeCapacity the capacity.
     */
    public void reserve(Context context, ParmsIdType parmsId, int sizeCapacity) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        Context.ContextData contextData = context.getContextData(parmsId);
        if (contextData == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }
        EncryptionParams parms = contextData.getParms();
        // note that parmsId must be cloned
        this.parmsId = contextData.getParmsId().clone();
        reserveInternal(sizeCapacity, parms.getPolyModulusDegree(), parms.getCoeffModulus().length);
    }

    /**
     * Allocates enough memory to accommodate the backing array of a ciphertext with given parameters.
     *
     * @param sizeCapacity      the max number of ciphertext poly.
     * @param polyModulusDegree the degree of the polynomial.
     * @param coeffModulusSize  the number of primes in the coefficient modulus.
     */
    private void reserveInternal(int sizeCapacity, int polyModulusDegree, int coeffModulusSize) {
        if (sizeCapacity < Constants.CIPHERTEXT_SIZE_MIN || sizeCapacity > Constants.CIPHERTEXT_SIZE_MAX) {
            throw new IllegalArgumentException("invalid size capacity");
        }
        // sizeCapacity * polyModulusDegree * coeffModulusSize is the total number of long type value needed by current ciphertext
        int newDataCapacity = Common.mulSafe(sizeCapacity, polyModulusDegree, false, coeffModulusSize);
        int newDataSize = Math.min(newDataCapacity, data.size());
        data.reserve(newDataCapacity);
        data.resize(newDataSize);
        size = Math.min(sizeCapacity, size);
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = coeffModulusSize;
    }

    /**
     * Resizes the ciphertext to given size, reallocating if the capacity of the ciphertext is too small.
     * The ciphertext parameters are determined by the highest-level parameters associated to the given context.
     *
     * @param context the context.
     * @param size    the new size.
     */
    public void resize(Context context, int size) {
        resize(context, context.getFirstParmsId(), size);
    }

    /**
     * Resizes the ciphertext to given size, reallocating if the capacity of the ciphertext is too small.
     * The ciphertext parameters are determined by the current context.
     *
     * @param size the new size.
     */
    public void resize(int size) {
        resizeInternal(size, polyModulusDegree, coeffModulusSize);
    }

    /**
     * Resizes the ciphertext to given size, reallocating if the capacity of the ciphertext is too small.
     * The ciphertext parameters are determined by the given context and parms ID.
     *
     * @param context the context.
     * @param parmsId the parms_id corresponding to the encryption parameters to be used.
     * @param size    the new size.
     */
    public void resize(Context context, ParmsIdType parmsId, int size) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        Context.ContextData contextData = context.getContextData(parmsId);
        if (contextData == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }
        EncryptionParams parms = contextData.getParms();
        // todo: need deep-copy?
        this.parmsId = contextData.getParmsId().clone();
        resizeInternal(size, parms.getPolyModulusDegree(), parms.getCoeffModulus().length);
    }

    /**
     * Resizes the ciphertext to given parameters.
     *
     * @param size              the size of the ciphertext.
     * @param polyModulusDegree the degree of the polynomial.
     * @param coeffModulusSize  the number of primes in the coefficient modulus.
     */
    private void resizeInternal(int size, int polyModulusDegree, int coeffModulusSize) {
        if (size < Constants.CIPHERTEXT_SIZE_MIN || size > Constants.CIPHERTEXT_SIZE_MAX) {
            throw new IllegalArgumentException("invalid size");
        }
        // resize data
        // todo: need mulSafe?
        int newDataSize = Common.mulSafe(size, polyModulusDegree, false, coeffModulusSize);
        data.resize(newDataSize);
        // setSize parameters
        this.size = size;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = coeffModulusSize;
    }

    /**
     * Returns the backing DynArray object.
     *
     * @return the backing DynArray object.
     */
    public DynArray getDynArray() {
        return data;
    }

    /**
     * Returns the ciphertext data.
     *
     * @return the ciphertext data.
     */
    public long[] getData() {
        return data.data();
    }

    /**
     * Returns the index of a particular polynomial in the ciphertext data.
     * Note that Microsoft SEAL stores each polynomial in the ciphertext modulo all the K primes in the coefficient modulus.
     * The index returned by this function is the beginning index (constant coefficient) of the first one of these K polynomials.
     *
     * @param polyIndex the index of the polynomial in the ciphertext.
     * @return the beginning index of the particular polynomial.
     */
    public int getData(int polyIndex) {
        assert polyIndex >= 0 && polyIndex < size;
        int polyUint64Count = Common.mulSafe(polyModulusDegree, coeffModulusSize, false);
        return Common.mulSafe(polyIndex, polyUint64Count, false);
    }

    /**
     * Returns the index of a particular polynomial in the ciphertext data.
     * Note that Microsoft SEAL stores each polynomial in the ciphertext modulo all the K primes in the coefficient modulus.
     * The index returned by this function is the beginning index (constant coefficient) of the first one of these K polynomials.
     *
     * @param polyIndex the index of the polynomial in the ciphertext.
     * @return the beginning index of the particular polynomial.
     */
    public int indexAt(int polyIndex) {
        assert polyIndex >= 0 && polyIndex < size;
        int polyUint64Count = Common.mulSafe(polyModulusDegree, coeffModulusSize, false);
        return Common.mulSafe(polyIndex, polyUint64Count, false);
    }

    /**
     * Returns the data of a particular polynomial in the ciphertext data.
     *
     * @param polyIndex the index of the polynomial in the ciphertext.
     * @return the data of the particular polynomial.
     */
    public long[] getPoly(int polyIndex) {
        int polyUint64Count = Common.mulSafe(polyModulusDegree, coeffModulusSize, false);
        int startIndex = Common.mulSafe(polyIndex, polyUint64Count, false);
        int endIndex = startIndex + polyUint64Count;
        return data.data(startIndex, endIndex);
    }

    /**
     * Returns a reference to a polynomial coefficient at a particular index in the ciphertext data.
     * If the polynomial modulus has degree N, and the number of primes in the coefficient modulus is K, then the
     * ciphertext contains size*N*K coefficients. Thus, the coeff_index has a range of [0, size*N*K).
     *
     * @param coeffIndex the index of the coefficient under RNS form.
     * @return the coefficient.
     */
    public long getCoeff(int coeffIndex) {
        return data.at(coeffIndex);
    }

    /**
     * Returns the number of primes in the coefficient modulus of the associated encryption parameters.
     * This directly affects the allocation size of the ciphertext.
     *
     * @return the number of primes in the coefficient modulus.
     */
    public int getCoeffModulusSize() {
        return coeffModulusSize;
    }

    /**
     * Returns the degree of the polynomial modulus of the associated encryption parameters.
     * This directly affects the allocation size of the ciphertext.
     *
     * @return the degree of the polynomial.
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * Returns the size of the ciphertext.
     *
     * @return the size of the ciphertext.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the parms ID.
     *
     * @return the parms ID.
     */
    public ParmsIdType getParmsId() {
        return parmsId;
    }

    /**
     * Sets the parms ID.
     *
     * @param parmsId the parms ID.
     */
    public void setParmsId(ParmsIdType parmsId) {
        this.parmsId = parmsId;
    }

    /**
     * Returns a reference to the scale. This is only needed when using the CKKS encryption scheme.
     * The user should have little or no reason to ever change the scale by hand.
     *
     * @return the scale.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets the scale.
     *
     * @param scale the scale.
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     * Returns a reference to the correction factor. This is only needed when using the BGV encryption scheme.
     * The user should have little or no reason to ever change the correction factor by hand.
     *
     * @return the correction factor.
     */
    public long getCorrectionFactor() {
        return correctionFactor;
    }

    /**
     * Sets the correction factor.
     * @param correctionFactor correction factor.
     */
    public void setCorrectionFactor(long correctionFactor) {
        this.correctionFactor = correctionFactor;
    }

    /**
     * Returns the size of ciphertext.
     *
     * @return the size of ciphertext.
     */
    public int getSizeCapacity() {
        int polyUint64Count = polyModulusDegree * coeffModulusSize;
        return polyUint64Count > 0 ? data.capacity() / polyUint64Count : 0;
    }

    /**
     * Returns whether the ciphertext is in NTT form.
     *
     * @return whether the ciphertext is in NTT form.
     */
    public boolean isNttForm() {
        return isNttForm;
    }

    /**
     * Sets the NTT form of the ciphertext.
     *
     * @param isNttForm the NTT form.
     */
    public void setIsNttForm(boolean isNttForm) {
        this.isNttForm = isNttForm;
    }

    /**
     * Check whether the current ciphertext is transparent, i.e. does not require
     * a secret key to decrypt. In typical security models such transparent
     * ciphertexts would not be considered to be valid. Starting from the second
     * polynomial in the current ciphertext, this function returns true if all
     * following coefficients are identically zero. Otherwise, returns false.
     *
     * @return true if the ciphertext is transparent, otherwise false.
     */
    public boolean isTransparent() {
        boolean b1 = data.size() == 0 || (size < Constants.CIPHERTEXT_SIZE_MIN);
        // 判断 第二个多项式的每一个值是否为 0, 全部为0 返回 true, 其余则返回 false
        boolean b2 = true;
        int startIndex = getData(1);
        for (int i = startIndex; i < data.data().length; i++) {
            if (data.data()[i] != 0) {
                b2 = false;
                break;
            }
        }
        return b1 || b2;
    }


    @Override
    public Ciphertext clone() {
        try {
            Ciphertext clone = (Ciphertext) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            // only need deep-copy reference variable
            clone.parmsId = this.parmsId.clone();
            clone.data = this.data.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ciphertext)) {
            return false;
        }
        Ciphertext that = (Ciphertext) o;
        return new EqualsBuilder()
            .append(isNttForm, that.isNttForm)
            .append(size, that.size)
            .append(polyModulusDegree, that.polyModulusDegree)
            .append(coeffModulusSize, that.coeffModulusSize)
            .append(scale, that.scale)
            .append(correctionFactor, that.correctionFactor)
            .append(parmsId, that.parmsId)
            .append(data, that.data)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(parmsId)
            .append(isNttForm)
            .append(size)
            .append(polyModulusDegree)
            .append(coeffModulusSize)
            .append(scale)
            .append(correctionFactor)
            .append(data)
            .toHashCode();
    }
}
