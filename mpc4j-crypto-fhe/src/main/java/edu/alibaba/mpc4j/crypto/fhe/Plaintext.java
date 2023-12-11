package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.utils.DynArray;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * Class to store a plaintext element. The data for the plaintext is a polynomial
 * with coefficients modulo the plaintext modulus. The degree of the plaintext
 * polynomial must be one less than the degree of the polynomial modulus. The
 * backing array always allocates one 64-bit word per each coefficient of the
 * polynomial.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/plaintext.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/2
 */
public class Plaintext implements Cloneable {

    /**
     * params ID
     */
    private ParmsIdType parmsId = ParmsIdType.parmsIdZero();
    /**
     * the number of coefficients in the plaintext polynomial
     */
    private int coeffCount = 0;
    /**
     * scale, only needed when using the CKKS encryption scheme
     */
    private double scale = 1.0;
    /**
     * data, todo: must use DynArray?
     */
    private DynArray data;

    public Plaintext() {
        data = new DynArray();
    }

    /**
     * Constructs a plaintext representing a constant polynomial 0.
     * The coefficient count of the polynomial is set to the given value. The capacity is set to the same value.
     *
     * @param coeffCount the number of (zeroed) coefficients in the plaintext.
     */
    public Plaintext(int coeffCount) {
        this.coeffCount = coeffCount;
        this.data = new DynArray(coeffCount);
    }

    /**
     * Constructs a plaintext representing a constant polynomial 0.
     * The coefficient count of the polynomial and the capacity are set to the given values.
     *
     * @param capacity   the capacity.
     * @param coeffCount the number of (zeroed) coefficients in the plaintext.
     */
    public Plaintext(int capacity, int coeffCount) {
        this.coeffCount = coeffCount;
        this.data = new DynArray(capacity, coeffCount);
    }

    /**
     * Constructs a plaintext representing a polynomial with given coefficient values.
     * The coefficient count of the polynomial is set to the number of coefficient values provided,
     * and the capacity is set to the given value.
     *
     * @param coeffs   the coefficient values.
     * @param capacity the capacity.
     */
    public Plaintext(long[] coeffs, int capacity) {
        this.coeffCount = coeffs.length;
        this.data = new DynArray(coeffs, capacity);
    }

    /**
     * Constructs a plaintext representing a polynomial with given coefficient values.
     * The coefficient count of the polynomial is set to the number of coefficient values provided,
     * and the capacity is set to the same value.
     *
     * @param coeffs the coefficient values.
     */
    public Plaintext(long[] coeffs) {
        this.coeffCount = coeffs.length;
        this.data = new DynArray(coeffs);
    }

    /**
     * Copies a given plaintext to the current one.
     *
     * @param assign the plaintext to copy from.
     */
    public void copyFrom(Plaintext assign) {
        this.coeffCount = assign.coeffCount;
        this.scale = assign.scale;
        this.setParmsId(assign.getParmsId().clone());
        this.resize(assign.coeffCount);
        System.arraycopy(assign.getData(), 0, this.getData(), 0, assign.coeffCount);
    }

    /**
     * Creates a new plaintext by copying a given one.
     *
     * @param copy the plaintext to copy from.
     */
    public Plaintext(Plaintext copy) {
        this.coeffCount = copy.coeffCount;
        this.parmsId = new ParmsIdType(copy.parmsId);
        this.scale = copy.scale;
        this.data = new DynArray(copy.data);
    }

    /**
     * Constructs a plaintext from a given hexadecimal string describing the
     * plaintext polynomial.
     * <p>
     * The string description of the polynomial must adhere to the format returned
     * by to_string(),
     * which is of the form "7FFx^3 + 1x^1 + 3" and summarized by the following
     * rules:
     * 1. Terms are listed in order of strictly decreasing exponent
     * 2. Coefficient values are non-negative and in hexadecimal format (upper
     * and lower case letters are both supported)
     * 3. Exponents are positive and in decimal format
     * 4. Zero coefficient terms (including the constant term) may be (but do
     * not have to be) omitted
     * 5. Term with the exponent value of one must be exactly written as x^1
     * 6. Term with the exponent value of zero (the constant term) must be written
     * as just a hexadecimal number without exponent
     * 7. Terms must be separated by exactly <space>+<space> and minus is not
     * allowed
     * 8. Other than the +, no other terms should have whitespace
     *
     * @param hexPoly a poly in hex string.
     */
    public Plaintext(String hexPoly) {
        // first call new Plaintext()
        this();
        fromHexPoly(hexPoly);
    }

    /**
     * Creates a new plaintext from a given hexadecimal string describing the plaintext polynomial.
     *
     * @param hexPoly the formatted polynomial string specifying the plaintext polynomial.
     */
    public void fromHexPoly(String hexPoly) {
        if (isNttForm()) {
            throw new RuntimeException("cannot set an NTT transformed Plaintext");
        }
        if (Common.unsignedGt(hexPoly.length(), Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("hex poly too long");
        }
        int length = hexPoly.length();
        // Determine size needed to store string coefficient.
        int assignCoeffCount = 0;
        int assignCoeffBitCount = 0;
        int pos = 0;
        // todo: need Math.min? here two value is equal.
        int lastPower = Math.min(data.maxSize(), Integer.MAX_VALUE);
        // 这个 while 是在做两件事情: 1. 确定多项式的 coeffCount, 2. 确定每一个系数的 bitCount
        // 以 "1x^63 + 2Fx^62 + Fx^32" 为例子
        while (pos < length) {
            // Determine length of coefficient starting at pos.
            // 这里是在计算 coeff 的长度，例如 1 --> 长度就是 1，这里的长度是指 char 的长度
            // 2F 的长度就是 2
            int coeffLength = getCoeffLength(hexPoly, pos);
            if (coeffLength == 0) {
                throw new IllegalArgumentException("unable to parse hex poly, please check the format of the hex poly");
            }
            // Determine bit length of coefficient.
            // 迭代找出最大的  assignCoeffBitCount
            int coeffBitCount = Common.getHexStringBitCount(hexPoly, pos, coeffLength);
            if (coeffBitCount > assignCoeffBitCount) {
                assignCoeffBitCount = coeffBitCount;
            }
            pos += coeffLength;
            // Extract power-term.
            // 这里是在计算 x^63 的长度，这里就是 4,  x^2 --> 3
            int[] powerLength = new int[1];
            // power 就是 x^ 的这个power 值，长度保存在 powerLength
            int power = getCoeffPower(hexPoly, pos, powerLength);
            if (power == -1 || power >= lastPower) {
                throw new IllegalArgumentException("unable to parse hex poly");
            }
            // 按照规定第一个 1x^63 的power 是最高项，那么系数长度（coeff count）就 63 + 1
            if (assignCoeffCount == 0) {
                assignCoeffCount = power + 1;
            }
            pos += powerLength[0];
            lastPower = power;
            // Extract plus (unless it is the end).
            // 获取 " + " 的长度，正常情况下 就是 3
            int plusLength = getPlus(hexPoly, pos);
            if (plusLength == -1) {
                throw new IllegalArgumentException("unable to parse hex poly");
            }
            pos += plusLength;
        }
        // If string is empty, then done.
        if (assignCoeffCount == 0 || assignCoeffBitCount == 0) {
            setZero();
            return;
        }
        // Resize polynomial.
        // 单个多项式系数至多 64-bit（实际上更少）
        if (assignCoeffBitCount > Common.BITS_PER_UINT64) {
            throw new IllegalArgumentException("hex poly has too large coefficients");
        }
        resize(assignCoeffCount);
        // Populate polynomial from string.
        pos = 0;
        lastPower = getCoeffCount();
        // 这里的 while 开始真正的从一个 Hex Poly 转换为 long[]
        // 这里的做法和上面完全相同，就是逐个遍历 hexPoly，依次读取 coeff (1), power(x^63), plus(" + ")
        // 然后把 coeff 放在 long[] 中，注意 long[] 中顺序和 power 的关系
        // 1x^63 ， 是把 1 放在 data[63]  这个位置上的，是一一对应的
        // 也就是 power = index(base-0)
        while (pos < length) {
            // Determine length of coefficient starting at pos.
            // 记录下系数 所在的 pos
            int coeffPos = pos;
            int coeffLength = getCoeffLength(hexPoly, pos);
            pos += coeffLength;
            // Extract power-term.
            int[] powerLength = new int[1];
            int power = getCoeffPower(hexPoly, pos, powerLength);
            pos += powerLength[0];
            // Extract plus (unless it is the end).
            int plusLength = getPlus(hexPoly, pos);
            pos += plusLength;
            // Zero coefficients not set by string.
            for (int zeroPower = lastPower - 1; zeroPower > power; --zeroPower) {
                data.set(zeroPower, 0);
            }
            // Populate coefficient.
            // 注意函数签名
            UintCore.hexStringToUint(hexPoly, coeffPos, coeffLength, 1, power, getData());
            lastPower = power;
        }
        // Zero coefficients not set by string.
        for (int zeroPower = lastPower - 1; zeroPower >= 0; --zeroPower) {
            data.set(zeroPower, 0);
        }
    }

    /**
     * Returns whether the character is decimal.
     *
     * @param c the character.
     * @return whether the character is decimal.
     */
    private boolean isDecimalChar(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Gets the decimal value of the given character.
     *
     * @param c the character.
     * @return the decimal value.
     */
    private int getDecimalValue(char c) {
        return c - '0';
    }

    /**
     * Gets the coefficient values length of the polynomial, example 1Fx^1 --> coeffLength = 2
     *
     * @param poly       the polynomial.
     * @param startIndex start index.
     * @return the coefficient values length.
     */
    private int getCoeffLength(String poly, int startIndex) {
        int length = 0;
        int charIndex = startIndex;
        // 注意 while 条件有两个
        while (charIndex < poly.length() && Common.isHexChar(poly.charAt(charIndex))) {
            length++;
            charIndex++;
        }
        return length;
    }

    /**
     * Gets the coefficient power, example 1Fx^1 --> coeff power = 1
     *
     * @param poly        the polynomial.
     * @param startIndex  start index.
     * @param powerLength the power length.
     * @return coeff power.
     */
    private int getCoeffPower(String poly, int startIndex, int[] powerLength) {
        int length = 0;
        int polyIndex = startIndex;
        if (poly.length() == startIndex) {
            powerLength[0] = 0;
            return 0;
        }
        if (poly.charAt(polyIndex) != 'x') {
            return -1;
        }
        polyIndex++;
        length++;
        if (poly.charAt(polyIndex) != '^') {
            return -1;
        }
        polyIndex++;
        length++;
        // 例如： '4' '0' '9' '6' ---> 4096
        int power = 0;
        while (polyIndex < poly.length() && isDecimalChar(poly.charAt(polyIndex))) {
            power *= 10;
            power += getDecimalValue(poly.charAt(polyIndex));
            polyIndex++;
            length++;
        }
        powerLength[0] = length;
        return power;
    }

    /**
     * Gets "+" symbol length.
     * @param poly       the polynomial.
     * @param startIndex start index.
     * @return "+" symbol length.
     */
    private int getPlus(String poly, int startIndex) {
        int polyIndex = startIndex;
        if (poly.length() == startIndex) {
            return 0;
        }

        if (poly.charAt(polyIndex++) != ' ') {
            return -1;
        }

        if (poly.charAt(polyIndex++) != '+') {
            return -1;
        }
        if (poly.charAt(polyIndex) != ' ') {
            return -1;
        }
        // " + "  --> length = 3
        return 3;
    }

    /**
     * Allocates enough memory to accommodate the backing array of a plaintext with given capacity.
     *
     * @param capacity the capacity.
     */
    public void reserve(int capacity) {
        if (isNttForm()) {
            throw new RuntimeException("cannot reserve for an NTT transformed Plaintext");
        }
        data.reserve(capacity);
        coeffCount = data.size();
    }

    /**
     * Reallocates the data so that its capacity exactly matches its size.
     */
    public void shrinkToFit() {
        data.shrinkToFit();
    }

    /**
     * Releases the data.
     */
    public void release() {
        parmsId = ParmsIdType.parmsIdZero();
        coeffCount = 0;
        scale = 1.0;
        data.release();
    }

    /**
     * Resizes the plaintext to have a given coefficient count.
     * The plaintext is automatically reallocated if the new coefficient count does not fit in the current capacity.
     *
     * @param coeffCount the number of coefficients in the plaintext polynomial.
     */
    public void resize(int coeffCount) {
        if (isNttForm()) {
            throw new RuntimeException("cannot resize for an NTT transformed Plaintext");
        }
        data.resize(coeffCount);
        this.coeffCount = coeffCount;
    }

    /**
     * Sets a coefficient of the polynomial with the given value.
     *
     * @param index the index of the coefficient to set.
     * @param coeff the given coefficient value.
     */
    public void set(int index, long coeff) {
        data.set(index, coeff);
    }

    /**
     * Sets the value of the current plaintext to a given constant polynomial and sets the parms_id to parms_id_zero,
     * effectively marking the plaintext as not NTT transformed. The coefficient count is set to one.
     *
     * @param constCoeff the constant coefficient.
     */
    public void set(long constCoeff) {
        data.resize(1);
        data.set(0, constCoeff);
        coeffCount = 1;
        parmsId = ParmsIdType.parmsIdZero();
    }

    /**
     * Sets the coefficients of the current plaintext to given values and sets the parms_id to parms_id_zero,
     * effectively marking the plaintext as not NTT transformed.
     *
     * @param coeffs desired values of the plaintext coefficients.
     */
    public void set(long[] coeffs) {
        data = new DynArray(coeffs);
        coeffCount = coeffs.length;
        parmsId = ParmsIdType.parmsIdZero();
    }

    /**
     * Returns the value of a given coefficient in the plaintext polynomial.
     *
     * @param index the index of the coefficient in the plaintext polynomial.
     * @return the value of a given coefficient in the plaintext polynomial.
     */
    public long get(int index) {
        return data.at(index);
    }

    /**
     * Returns the value of a given coefficient in the plaintext polynomial.
     *
     * @param index the index of the coefficient in the plaintext polynomial.
     * @return the value of a given coefficient in the plaintext polynomial.
     */
    public long at(int index) {
        return data.at(index);
    }

    /**
     * Returns the value of a given coefficient in the plaintext polynomial.
     *
     * @param index the index of the coefficient in the plaintext polynomial.
     * @return the value of a given coefficient in the plaintext polynomial.
     */
    public long getValue(int index) {
        return data.at(index);
    }

    /**
     * Returns the scale of the plaintext.
     *
     * @return the scale of the plaintext.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets a given range of coefficients of a plaintext polynomial to zero; does nothing if length is zero.
     *
     * @param startCoeff the index of the first coefficient to set to zero.
     * @param length     the number of coefficients to set to zero.
     */
    public void setZero(int startCoeff, int length) {
        if (length <= 0) {
            return;
        }
        if (startCoeff + length - 1 >= coeffCount) {
            throw new IndexOutOfBoundsException("length must be non-negative and start_coeff + length - 1 must be within [0, coeff_count)");
        }
        data.setZero(startCoeff, length);
    }

    /**
     * Sets the plaintext polynomial coefficients to zero starting at a given index.
     *
     * @param startCoeff the index of the first coefficient to set to zero.
     */
    public void setZero(int startCoeff) {
        if (startCoeff >= coeffCount) {
            throw new IndexOutOfBoundsException("start_coeff must be within [0, coeff_count)");
        }
        data.setZero(startCoeff);
    }

    /**
     * Sets the plaintext polynomial to zero.
     */
    public void setZero() {
        data.setZero();
    }

    /**
     * Gets the DynArray object of the plaintext.
     *
     * @return the data of the plaintext.
     */
    public DynArray getDynArray() {
        return data;
    }

    /**
     * Gets the data of the plaintext.
     *
     * @return the data of the plaintext.
     */
    public long[] getData() {
        return data.data();
    }

    /**
     * Returns the value of a given coefficient in the plaintext polynomial.
     *
     * @param coeffIndex the index of the coefficient in the plaintext polynomial.
     * @return the value of a given coefficient in the plaintext polynomial.
     */
    public long getData(int coeffIndex) {
        if (coeffCount == 0) {
            throw new RuntimeException();
        }
        if (coeffIndex >= coeffCount) {
            throw new IndexOutOfBoundsException("coeff_index must be within [0, coeff_count)");
        }
        return data.at(coeffIndex);
    }

    /**
     * Returns whether the current plaintext polynomial has all zero coefficients.
     *
     * @return whether the current plaintext polynomial has all zero coefficients.
     */
    public boolean isZero() {
        return (coeffCount == 0) || data.isZero();
    }

    /**
     * Returns the capacity of the current allocation.
     *
     * @return the capacity of the current allocation.
     */
    public int getCapacity() {
        return data.capacity();
    }

    /**
     * Returns the coefficient count of the current plaintext polynomial.
     *
     * @return the coefficient count of the current plaintext polynomial.
     */
    public int getCoeffCount() {
        return coeffCount;
    }

    /**
     * @return the significant coefficient count of the current plaintext polynomial.
     */
    public int significantCoeffCount() {
        if (coeffCount == 0) {
            return 0;
        }
        return UintCore.getSignificantUint64CountUint(data.data(), coeffCount);
    }

    /**
     * Returns the non-zero coefficient count of the current plaintext polynomial.
     *
     * @return the non-zero coefficient count of the current plaintext polynomial.
     */
    public int nonZeroCoeffCount() {
        if (coeffCount == 0) {
            return 0;
        }
        return UintCore.getNonZeroUint64CountUint(data.data(), coeffCount);
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
     * Sets the given parms ID to current plaintext.
     *
     * @param parmsId the given parms ID.
     */
    public void setParmsId(long[] parmsId) {
        this.parmsId.set(parmsId);
    }

    /**
     * Sets the given parms ID to current plaintext.
     *
     * @param parmsId the given parms ID.
     */
    public void setParmsId(ParmsIdType parmsId) {
        // todo: really need clone?
        this.parmsId = parmsId.clone();
    }

    /**
     * Returns the scale of the plaintext.
     *
     * @return the scale of the plaintext.
     */
    public double scale() {
        return scale;
    }

    /**
     * @return Returns whether the plaintext is in NTT form.
     */
    public boolean isNttForm() {
        return !parmsId.isZero();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Plaintext)) {
            return false;
        }
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
        if (isNttForm()) {
            throw new IllegalArgumentException("cannot convert NTT transformed plaintext to string");
        }
        return PolyCore.polyToHexString(data.data(), coeffCount, 1);
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
