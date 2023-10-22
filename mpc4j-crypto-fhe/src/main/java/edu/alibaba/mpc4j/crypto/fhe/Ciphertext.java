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
import java.util.Arrays;

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

    private ParmsIdType parmsId = ParmsIdType.parmsIdZero();

    private boolean isNttForm = false;
    // size 个 Poly， size >= 2, 总共需要 size * k * N 个 Slots
    private int size = 0;

    private int polyModulusDegree = 0;

    private int coeffModulusSize = 0;

    private double scale = 1.0;

    private long correctionFactor = 1;

    private DynArray data = new DynArray();

    /**
     * Constructs an empty ciphertext allocating no memory.
     */
    public Ciphertext() {
    }

    /**
     * Constructs an empty ciphertext with capacity 2. In addition to the
     * capacity, the allocation size is determined by the highest-level
     * parameters associated to the given SEALContext.
     * <p>
     * highest-level parameters 是何意？
     * 2 应该就是容纳两个多项式
     *
     * @param context
     */
    public Ciphertext(Context context) {
        reserve(context, 2);
    }

    /**
     * Constructs an empty ciphertext with capacity 2. In addition to the
     * capacity, the allocation size is determined by the encryption parameters
     * with given parms_id.
     * <p>
     * 一个 parmsId 对应一个 加密参数对象
     *
     * @param context
     * @param parmsId
     */
    public Ciphertext(Context context, ParmsIdType parmsId) {
        reserve(context, parmsId, 2);
    }

    public Ciphertext(Context context, ParmsIdType parmsId, int sizeCapacity) {
        reserve(context, parmsId, sizeCapacity);
    }

    /**
     * 对标 Ciphertext &Ciphertext::operator=(const Ciphertext &assign) 这个实现
     *
     * @param assign another Ciphertext Object
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
     * Resets the ciphertext. This function releases any memory allocated
     * by the ciphertext, returning it to the memory pool. It also sets all
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


    public void reserve(Context context, int sizeCapacity) {

        reserve(context, context.getFirstParmsId(), sizeCapacity);
    }

    public void reserve(int sizeCapacity) {
        reserveInternal(sizeCapacity, polyModulusDegree, coeffModulusSize);
    }


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
     * @param sizeCapacity      max number of ciphertext poly
     * @param polyModulusDegree N
     * @param coeffModulusSize  number of coeff moduli
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


    public void resize(Context context, int size) {
        resize(context, context.getFirstParmsId(), size);
    }

    public void resize(int size) {
        resizeInternal(size, polyModulusDegree, coeffModulusSize);
    }


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


    public DynArray getDynArray() {
        return data;
    }

    public long[] getData() {
        return data.data();
    }

    /**
     * @param polyIndex
     * @return 第 polyIndex 在 数组中的起始位置
     */
    public int getData(int polyIndex) {
        assert polyIndex >= 0 && polyIndex < size;

        // 一个多项式 在 RNS 下被表示为 coeffModuluSize 个多项式
        int polyUint64Count = Common.mulSafe(polyModulusDegree, coeffModulusSize, false);

        // 其实是返回这个多项式 在 DynArray 中的起点

        return Common.mulSafe(polyIndex, polyUint64Count, false);
    }

    /**
     * 获取这个 polyIndex 的索引，例如 2 * 3 * 64, polyIndex = 1 ---> 3 * 64
     *
     * @param polyIndex 某个 poly 的 index
     * @return
     */
    public int indexAt(int polyIndex) {
        assert polyIndex >= 0 && polyIndex < size;

        // 一个多项式 在 RNS 下被表示为 coeffModuluSize 个多项式
        // todo: need mulSafe ?
        int polyUint64Count = Common.mulSafe(polyModulusDegree, coeffModulusSize, false);

        // 其实是返回这个多项式 在 DynArray 中的起点

        return Common.mulSafe(polyIndex, polyUint64Count, false);
    }


    public long[] getPoly(int polyIndex) {
        // 一个多项式 在 RNS 下被表示为 coeffModuluSize 个多项式
        int polyUint64Count = Common.mulSafe(polyModulusDegree, coeffModulusSize, false);

        // 其实是返回这个多项式 在 DynArray 中的起点

        int startIndex = Common.mulSafe(polyIndex, polyUint64Count, false);
        int endIndex = startIndex + polyUint64Count;
        return data.data(startIndex, endIndex);
    }

    /**
     * Returns a reference to a polynomial coefficient at a particular
     * index in the ciphertext data. If the polynomial modulus has degree N,
     * and the number of primes in the coefficient modulus is K, then the
     * ciphertext contains size*N*K coefficients. Thus, the coeff_index has
     * a range of [0, size*N*K).
     * <p>
     * size 是密文中多项式的数量，一个多项式在 RNS 表示下 需要 N * k 个系数来表示
     *
     * @param coeffIndex
     * @return
     */
    public long getCoeff(int coeffIndex) {

        return data.at(coeffIndex);
    }

    public int getCoeffModulusSize() {
        return coeffModulusSize;
    }

    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * @return 密文中多项式的数量
     */
    public int getSize() {
        return size;
    }

    public ParmsIdType getParmsId() {
        return parmsId;
    }

    public void setParmsId(ParmsIdType parmsId) {
        this.parmsId = parmsId;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public long getCorrectionFactor() {
        return correctionFactor;
    }

    public void setCorrectionFactor(long correctionFactor) {
        this.correctionFactor = correctionFactor;
    }

    public int getSizeCapacity() {

        int polyUint64Count = polyModulusDegree * coeffModulusSize;
        return polyUint64Count > 0 ? data.capacity() / polyUint64Count : 0;
    }


    public boolean isNttForm() {
        return isNttForm;
    }

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
     * @return
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

        return new EqualsBuilder().append(isNttForm, that.isNttForm).append(size, that.size).append(polyModulusDegree, that.polyModulusDegree).append(coeffModulusSize, that.coeffModulusSize).append(scale, that.scale).append(correctionFactor, that.correctionFactor).append(parmsId, that.parmsId).append(data, that.data).isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(parmsId).append(isNttForm).append(size).append(polyModulusDegree).append(coeffModulusSize).append(scale).append(correctionFactor).append(data).toHashCode();
    }
}
