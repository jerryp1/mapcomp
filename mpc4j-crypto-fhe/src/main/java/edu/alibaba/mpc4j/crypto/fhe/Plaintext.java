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
     * coeff count
     */
    private int coeffCount = 0;
    /**
     * scale
     */
    private double scale = 1.0;

    /**
     * data
     */
    // todo: must use DynArray?
    // 始终只有 1个 Poly, 即 size = 1, 即使这个 poly 可能是 RNS base， 即 k * N
    private DynArray data;

    public Plaintext() {
        data = new DynArray();
    }

    /**
     * create plaintext zero with given coeff count.
     *
     * @param coeffCount coeff count.
     */
    public Plaintext(int coeffCount) {
        this.coeffCount = coeffCount;
        this.data = new DynArray(coeffCount);
    }

    /**
     * create plaintext zero with given coeff count.
     *
     * @param capacity   capacity.
     * @param coeffCount coeff count.
     */
    public Plaintext(int capacity, int coeffCount) {
        this.coeffCount = coeffCount;
        this.data = new DynArray(capacity, coeffCount);
    }

    /**
     * create plaintext zero with given coefficients.
     *
     * @param coeffs   coefficients.
     * @param capacity capacity.
     */
    public Plaintext(long[] coeffs, int capacity) {
        this.coeffCount = coeffs.length;
        this.data = new DynArray(coeffs, capacity);
    }

    /**
     * create plaintext zero with given coefficients.
     *
     * @param coeffs coefficients.
     */
    public Plaintext(long[] coeffs) {
        this.coeffCount = coeffs.length;
        this.data = new DynArray(coeffs);
    }

    /**
     * copy, (operator "=").
     *
     * @param assign other plaintext.
     */
    public void copyFrom(Plaintext assign) {
        this.coeffCount = assign.coeffCount;
        this.scale = assign.scale;
        this.setParmsId(assign.getParmsId().clone());
        this.resize(assign.coeffCount);
        System.arraycopy(assign.getData(), 0, this.getData(), 0, assign.coeffCount);
    }

    /**
     * deep-copy a Plaintext object.
     *
     * @param copy another Plaintext object.
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
     * create plaintext from string.
     *
     * @param hexPoly a poly in hex string.
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
     * whether character is decimal.
     *
     * @param c character.
     * @return return true if character is decimal, otherwise false.
     */
    private boolean isDecimalChar(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * get decimal value from character.
     *
     * @param c character.
     * @return decimal value.
     */
    private int getDecimalValue(char c) {
        return c - '0';
    }

    /**
     * get coeff length, example 1Fx^1 --> coeffLength = 2
     *
     * @param poly       poly.
     * @param startIndex start index.
     * @return coeff length.
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
     * get coeff power, example 1Fx^1 --> coeff power = 1
     *
     * @param poly        poly.
     * @param startIndex  start index.
     * @param powerLength power length.
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
     * get "+" symbol length.
     * @param poly       poly.
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
     * reserve the capacity of the plaintext data.
     *
     * @param capacity capacity.
     */
    public void reserve(int capacity) {
        if (isNttForm()) {
            throw new RuntimeException("cannot reserve for an NTT transformed Plaintext");
        }
        data.reserve(capacity);
        coeffCount = data.size();
    }

    /**
     * reallocates the data so that its capacity exactly matches its size.
     */
    public void shrinkToFit() {
        data.shrinkToFit();
    }

    /**
     * release the data.
     */
    public void release() {
        parmsId = ParmsIdType.parmsIdZero();
        coeffCount = 0;
        scale = 1.0;
        data.release();
    }

    /**
     * resize the data.
     *
     * @param coeffCount coeff count.
     */
    public void resize(int coeffCount) {
        if (isNttForm()) {
            throw new RuntimeException("cannot resize for an NTT transformed Plaintext");
        }
        data.resize(coeffCount);
        this.coeffCount = coeffCount;
    }

    /**
     * set the coefficient of the plaintext.
     *
     * @param index index.
     * @param coeff coefficient.
     */
    public void set(int index, long coeff) {
        data.set(index, coeff);
    }

    /**
     * create a const plaintext.
     *
     * @param constCoeff const coefficient.
     */
    public void set(long constCoeff) {
        data.resize(1);
        data.set(0, constCoeff);
        coeffCount = 1;
        parmsId = ParmsIdType.parmsIdZero();
    }

    /**
     * set the coefficients of the plaintext.
     *
     * @param coeffs coefficients.
     */
    public void set(long[] coeffs) {
        data = new DynArray(coeffs);
        coeffCount = coeffs.length;
        parmsId = ParmsIdType.parmsIdZero();
    }

    /**
     * get the index-th coefficient.
     *
     * @param index index.
     * @return the index-th coefficient.
     */
    public long get(int index) {
        return data.at(index);
    }

    /**
     * get the index-th coefficient.
     *
     * @param index index.
     * @return the index-th coefficient.
     */
    public long at(int index) {
        return data.at(index);
    }

    /**
     * get the index-th coefficient.
     *
     * @param index index.
     * @return the index-th coefficient.
     */
    public long getValue(int index) {
        return data.at(index);
    }

    /**
     * get scale.
     *
     * @return scale.
     */
    public double getScale() {
        return scale;
    }

    /**
     * set the coefficients from i-th to (i + length)-th as zero.
     *
     * @param startCoeff start coeff.
     * @param length     length.
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
     * set the coefficients i-th to the end as zero.
     *
     * @param startCoeff start coeff.
     */
    public void setZero(int startCoeff) {
        if (startCoeff >= coeffCount) {
            throw new IndexOutOfBoundsException("start_coeff must be within [0, coeff_count)");
        }
        data.setZero(startCoeff);
    }

    /**
     * set plaintext as zero.
     */
    public void setZero() {
        data.setZero();
    }

    /**
     * get plaintext data.
     *
     * @return plaintext data.
     */
    public DynArray getDynArray() {
        return data;
    }

    /**
     * get plaintext data.
     *
     * @return plaintext data.
     */
    public long[] getData() {
        return data.data();
    }

    /**
     * get the i-th coefficient of the polynomial.
     *
     * @param coeffIndex coeff index.
     * @return i-th coefficient.
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
     * whether the plaintext is zero.
     *
     * @return ture if the plaintext is zero, otherwise false.
     */
    public boolean isZero() {
        return (coeffCount == 0) || data.isZero();
    }

    /**
     * return capacity of the data.
     *
     * @return capacity of the data.
     */
    public int getCapacity() {
        return data.capacity();
    }

    /**
     * return coeff count.
     *
     * @return coeff count.
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
     * return the count of non-zero coefficients.
     *
     * @return the count of non-zero coefficients.
     */
    public int nonZeroCoeffCount() {
        if (coeffCount == 0) {
            return 0;
        }
        return UintCore.getNonZeroUint64CountUint(data.data(), coeffCount);
    }

    /**
     * return parms ID.
     *
     * @return parms ID.
     */
    public ParmsIdType getParmsId() {
        return parmsId;
    }

    /**
     * set parms ID to plaintext.
     *
     * @param parmsId parms ID.
     */
    public void setParmsId(long[] parmsId) {
        this.parmsId.set(parmsId);
    }

    /**
     * set parms ID to plaintext.
     *
     * @param parmsId parms ID.
     */
    public void setParmsId(ParmsIdType parmsId) {
        // todo: really need clone?
        this.parmsId = parmsId.clone();
    }

    /**
     * return scale.
     *
     * @return scale.
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
