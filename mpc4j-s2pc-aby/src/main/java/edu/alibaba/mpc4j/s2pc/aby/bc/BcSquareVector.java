package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Square secret-shared bit vector ([x]). The share is of the form: x = x_0 ⊕ x_1.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class BcSquareVector {
    /**
     * 布尔秘密分享值
     */
    private byte[] bytes;
    /**
     * 比特长度
     */
    private int bitLength;
    /**
     * 是否为明文状态，布尔电路仅可能全部为明文或全部为密文
     */
    private boolean plain;

    /**
     * 构造布尔方括号向量。从性能角度考虑，传入时不复制导线值。
     *
     * @param bytes     布尔方括号向量值。
     * @param bitLength 比特长度。
     * @param isPublic  是否为明文状态。
     * @return 布尔方括号向量。
     */
    public static BcSquareVector create(byte[] bytes, int bitLength, boolean isPublic) {
        assert bitLength > 0 : "bit length must be greater than 0: " + bitLength;
        int byteLength = CommonUtils.getByteLength(bitLength);
        assert bytes.length == byteLength : "bytes.length must be equal to " + byteLength + ": " + bytes.length;
        assert BytesUtils.isReduceByteArray(bytes, bitLength) : "bytes must contain at most " + bitLength + " bits";
        BcSquareVector bcSquareVector = new BcSquareVector();
        bcSquareVector.bytes = bytes;
        bcSquareVector.bitLength = bitLength;
        bcSquareVector.plain = isPublic;

        return bcSquareVector;
    }

    /**
     * 构造随机（密文）布尔方括号向量。
     *
     * @param bitLength    比特长度。
     * @param secureRandom 随机状态。
     * @return 随机（密文）布尔方括号向量。
     */
    public static BcSquareVector createRandom(int bitLength, SecureRandom secureRandom) {
        assert bitLength > 0 : "bit length must be greater than 0: " + bitLength;
        int byteLength = CommonUtils.getByteLength(bitLength);
        // create random bytes
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        BytesUtils.reduceByteArray(bytes, bitLength);
        // create instance
        BcSquareVector bcSquareVector = new BcSquareVector();
        bcSquareVector.bytes = bytes;
        bcSquareVector.bitLength = bitLength;
        bcSquareVector.plain = false;

        return bcSquareVector;
    }

    /**
     * 构造全1（明文）布尔方括号向量。
     *
     * @param bitLength 比特长度。
     * @return 全1（明文）布尔方括号向量。
     */
    public static BcSquareVector createOnes(int bitLength) {
        assert bitLength > 0 : "bit length must be greater than 0: " + bitLength;
        int byteLength = CommonUtils.getByteLength(bitLength);
        // create bytes with all 1
        byte[] ones = new byte[byteLength];
        Arrays.fill(ones, (byte) 0xFF);
        BytesUtils.reduceByteArray(ones, bitLength);
        // create instance
        BcSquareVector bcSquareVector = new BcSquareVector();
        bcSquareVector.bytes = ones;
        bcSquareVector.bitLength = bitLength;
        bcSquareVector.plain = true;

        return bcSquareVector;
    }

    /**
     * 构造全0（明文）布尔方括号向量。
     *
     * @param bitLength 比特长度。
     * @return 全0（明文）布尔方括号向量。
     */
    public static BcSquareVector createZeros(int bitLength) {
        assert bitLength > 0 : "bit length must be greater than 0: " + bitLength;
        int byteLength = CommonUtils.getByteLength(bitLength);
        // create bytes with all 0
        byte[] zeros = new byte[byteLength];
        // create instance
        BcSquareVector bcSquareVector = new BcSquareVector();
        bcSquareVector.bytes = zeros;
        bcSquareVector.bitLength = bitLength;
        bcSquareVector.plain = true;

        return bcSquareVector;
    }

    private BcSquareVector() {
        // empty
    }

    /**
     * 复制一个布尔方括号向量。
     *
     * @return 复制布尔方括号向量。
     */
    public BcSquareVector copy() {
        BcSquareVector clone = new BcSquareVector();
        clone.bytes = BytesUtils.clone(bytes);
        clone.bitLength = bitLength;
        clone.plain = plain;

        return clone;
    }

    /**
     * 返回比特长度。
     *
     * @return 比特长度。
     */
    public int bitLength() {
        return bitLength;
    }

    /**
     * 返回字节长度。
     *
     * @return 字节长度。
     */
    public int byteLength() {
        return bytes.length;
    }

    /**
     * 返回是否为明文向量。
     *
     * @return 是否为明文向量。
     */
    public boolean isPlain() {
        return plain;
    }

    /**
     * 返回布尔向量取值。
     *
     * @return 布尔向量取值。
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * 合并两个布尔方括号向量。
     *
     * @param that 另一个布尔方括号向量。
     */
    public void merge(BcSquareVector that) {
        assert this.plain == that.plain : "merged ones must have the same public state";
        // convert to BigInteger and then shift
        BigInteger thisBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(this.bytes);
        BigInteger thatBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(that.bytes);
        thisBigInteger = thisBigInteger.shiftLeft(that.bitLength).add(thatBigInteger);
        // update bit length
        bitLength += that.bitLength;
        int byteLength = CommonUtils.getByteLength(bitLength);
        bytes = BigIntegerUtils.nonNegBigIntegerToByteArray(thisBigInteger, byteLength);
    }

    /**
     * 计算两个布尔方括号向量的XOR。
     *
     * @param that 另一个布尔方括号向量。
     * @return XOR结果。
     */
    public BcSquareVector xor(BcSquareVector that) {
        assert this.bitLength == that.bitLength : "operate ones must have " + bitLength + " bits: " + that.bitLength;
        BcSquareVector result = new BcSquareVector();
        result.plain = plain && that.plain;
        result.bitLength = bitLength;
        result.bytes = BytesUtils.xor(bytes, that.bytes);

        return result;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(bytes)
            .append(bitLength)
            .append(plain)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BcSquareVector) {
            BcSquareVector that = (BcSquareVector) obj;
            return new EqualsBuilder()
                .append(this.bytes, that.bytes)
                .append(this.bitLength, that.bitLength)
                .append(this.plain, that.plain)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format(
            "%s (%s bits): %s", plain ? "Public" : "Secret", bitLength, Hex.toHexString(bytes)
        );
    }
}
