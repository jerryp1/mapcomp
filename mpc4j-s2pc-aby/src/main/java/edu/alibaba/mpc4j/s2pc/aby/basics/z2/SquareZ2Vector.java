package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Square Z2 vector ([x]). The share is of the form: x = x_0 ⊕ x_1.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class SquareZ2Vector implements MpcZ2Vector {
    /**
     * the bit vector
     */
    private BitVector bitVector;
    /**
     * the plain state.
     */
    private boolean plain;

    /**
     * Create a share bit vector.
     *
     * @param bitNum the bit num.
     * @param bytes  the assigned bits represented by bytes.
     * @param plain  the plain state.
     * @return a share bit vector.
     */
    public static SquareZ2Vector create(int bitNum, byte[] bytes, boolean plain) {
        SquareZ2Vector shareBitVector = new SquareZ2Vector();
        shareBitVector.bitVector = BitVectorFactory.create(bitNum, bytes);
        shareBitVector.plain = plain;

        return shareBitVector;
    }

    /**
     * Create a share bit vector.
     *
     * @param bitVector the bit vector.
     * @param plain     the plain state.
     * @return a share bit vector.
     */
    public static SquareZ2Vector create(BitVector bitVector, boolean plain) {
        SquareZ2Vector shareBitVector = new SquareZ2Vector();
        shareBitVector.bitVector = bitVector;
        shareBitVector.plain = plain;

        return shareBitVector;
    }

    /**
     * Create a (plain) share bit vector with all bits equal assigned boolean.
     *
     * @param bitNum the bit num.
     * @param value  assigned value.
     * @return a share bit vector.
     */
    public static SquareZ2Vector create(int bitNum, boolean value) {
        return value ? SquareZ2Vector.createOnes(bitNum) : SquareZ2Vector.createZeros(bitNum);
    }

    /**
     * Create a (plain) random share bit vector.
     *
     * @param bitNum       the bit num.
     * @param secureRandom the random states.
     * @return a share bit vector.
     */
    public static SquareZ2Vector createRandom(int bitNum, SecureRandom secureRandom) {
        SquareZ2Vector shareBitVector = new SquareZ2Vector();
        shareBitVector.bitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        shareBitVector.plain = true;

        return shareBitVector;
    }

    /**
     * Create a (plain) all-one share bit vector.
     *
     * @param bitNum the bit num.
     * @return a share bit vector.
     */
    public static SquareZ2Vector createOnes(int bitNum) {
        SquareZ2Vector squareShareBitVector = new SquareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createOnes(bitNum);
        squareShareBitVector.plain = true;

        return squareShareBitVector;
    }

    /**
     * Create a (plain) all-zero bit vector.
     *
     * @param bitNum the bit num.
     * @return a share bit vector.
     */
    public static SquareZ2Vector createZeros(int bitNum) {
        SquareZ2Vector squareShareBitVector = new SquareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createZeros(bitNum);
        squareShareBitVector.plain = true;

        return squareShareBitVector;
    }

    public static SquareZ2Vector createZeros(int bitNum, boolean isPlain) {
        SquareZ2Vector squareShareBitVector = new SquareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createZeros(bitNum);
        squareShareBitVector.plain = isPlain;

        return squareShareBitVector;
    }

    /**
     * Create an empty share bit vector.
     *
     * @param plain the plain state.
     * @return a share bit vector.
     */
    public static SquareZ2Vector createEmpty(boolean plain) {
        SquareZ2Vector squareShareBitVector = new SquareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createEmpty();
        squareShareBitVector.plain = plain;

        return squareShareBitVector;
    }

    private SquareZ2Vector() {
        // empty
    }

    @Override
    public SquareZ2Vector copy() {
        SquareZ2Vector clone = new SquareZ2Vector();
        clone.bitVector = bitVector.copy();
        clone.plain = plain;

        return clone;
    }

    @Override
    public int getNum() {
        return bitVector.bitNum();
    }

    @Override
    public int byteNum() {
        return bitVector.byteNum();
    }

    @Override
    public BitVector getBitVector() {
        return bitVector;
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public SquareZ2Vector split(int bitNum) {
        BitVector splitBitVector = bitVector.split(bitNum);
        return SquareZ2Vector.create(splitBitVector, plain);
    }

    @Override
    public void reduce(int bitNum) {
        bitVector.reduce(bitNum);
    }

    @Override
    public void merge(MpcVector other) {
        SquareZ2Vector that = (SquareZ2Vector) other;
        assert this.plain == that.isPlain() : "merged ones must have the same public state";
        bitVector.merge(that.getBitVector());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(bitVector)
            .append(plain)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SquareZ2Vector) {
            SquareZ2Vector that = (SquareZ2Vector) obj;
            return new EqualsBuilder()
                .append(this.bitVector, that.bitVector)
                .append(this.plain, that.plain)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", plain ? "plain" : "secret", bitVector.toString());
    }

    @Override
    public MpcZ2Vector extendBitsWithSkip(int destBitLen, int skipLen) {
        MathPreconditions.checkEqual("skipLen", "2^k", 1 << LongUtils.ceilLog2(skipLen), skipLen);
        int destByteNum = CommonUtils.getByteLength(destBitLen);
        byte[] destByte = new byte[destByteNum];
        int notFullNum = destBitLen % (skipLen << 1) - skipLen > 0 ? 1 : 0;
        int groupNum = destBitLen / (skipLen << 1) + notFullNum;
        // 如果第一个的length和其他的不一致，则先处理第一个
        if (notFullNum > 0) {
            // 如果第一个组是未满的，则之前的比较一定是从第一个开始取数比较的
            int destOffset = (destByteNum << 3) - destBitLen;
            int firstLen = destBitLen % skipLen;
            for (int i = 0; i < firstLen; i++, destOffset++) {
                if (this.getBitVector().get(i)) {
                    BinaryUtils.setBoolean(destByte, destOffset, true);
                    BinaryUtils.setBoolean(destByte, destOffset + skipLen, true);
                }
            }
        }
        // 处理后续满skipLen的数据
        byte[] srcByte = this.getBitVector().getBytes();
        if (skipLen >= 8) {
            int eachByteNum = skipLen >> 3, eachPartNum = eachByteNum << 1;
            int srcEndIndex = srcByte.length, destEndIndex = destByteNum;
            for (int i = groupNum - 1; i >= notFullNum; i--, destEndIndex -= eachPartNum, srcEndIndex -= eachByteNum) {
                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte, destEndIndex - eachByteNum, eachByteNum);
                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte, destEndIndex - eachPartNum, eachByteNum);
            }
        } else {
            // todo 先用最简单的方法处理
            int eachPartBit = skipLen << 1;
            int currentDestIndex = (destByteNum << 3) - eachPartBit, currentSrcIndex = (srcByte.length << 3) - skipLen;
            for (int i = groupNum - 1; i >= notFullNum; i--) {
                for (int j = 0; j < skipLen; j++) {
                    if (BinaryUtils.getBoolean(srcByte, currentSrcIndex + j)) {
                        BinaryUtils.setBoolean(destByte, currentDestIndex + j, true);
                        BinaryUtils.setBoolean(destByte, currentDestIndex + j + skipLen, true);
                    }
                }
                currentSrcIndex -= skipLen;
                currentDestIndex -= eachPartBit;
            }
        }
        return SquareZ2Vector.create(destBitLen, destByte, this.isPlain());
    }

    @Override
    public MpcZ2Vector[] getBitsWithSkip(int totalCompareNum, int skipLen) {
        MathPreconditions.checkEqual("skipLen", "2^k", 1 << LongUtils.ceilLog2(skipLen), skipLen);
        byte[] src = this.getBitVector().getBytes();
        int destByteNum = CommonUtils.getByteLength(totalCompareNum);
        byte[][] res = new byte[2][destByteNum];
        int groupNum = totalCompareNum / skipLen + (totalCompareNum % skipLen > 0 ? 1 : 0);

        // 如果第一个的length和其他的不一致，则先处理第一个
        if (totalCompareNum % skipLen > 0) {
            int destOffset = (destByteNum << 3) - totalCompareNum;
            int firstLen = totalCompareNum % skipLen;
            for (int i = 0; i < firstLen; i++, destOffset++) {
                if (this.getBitVector().get(i)) {
                    BinaryUtils.setBoolean(res[0], destOffset, true);
                }
                if (this.getBitVector().get(i + skipLen)) {
                    BinaryUtils.setBoolean(res[1], destOffset, true);
                }
            }
        }
        // 处理后续满skipLen的数据
        int notFullNum = totalCompareNum % skipLen > 0 ? 1 : 0;
        if (skipLen >= 8) {
            int eachByteNum = skipLen >> 3, eachPartNum = eachByteNum << 1;
            int srcEndIndex = src.length, destEndIndex = destByteNum;
            for (int i = groupNum - 1; i >= notFullNum; i--, destEndIndex -= eachByteNum, srcEndIndex -= eachPartNum) {
                System.arraycopy(src, srcEndIndex - eachByteNum, res[1], destEndIndex - eachByteNum, eachByteNum);
                System.arraycopy(src, srcEndIndex - eachPartNum, res[0], destEndIndex - eachByteNum, eachByteNum);
            }
        } else {
            int andNum = (1 << skipLen) - 1;
            // todo 先用最简单的方法处理
            int currentDestIndex = (destByteNum << 3) - skipLen, currentSrcIndex = (src.length << 3) - skipLen;
            for (int i = groupNum - 1; i >= notFullNum; i--) {
                for (int j = 0; j < skipLen; j++) {
                    if (BinaryUtils.getBoolean(src, currentSrcIndex + j)) {
                        BinaryUtils.setBoolean(res[1], currentDestIndex + j, true);
                    }
                }
                currentSrcIndex -= skipLen;
                for (int j = 0; j < skipLen; j++) {
                    if (BinaryUtils.getBoolean(src, currentSrcIndex + j)) {
                        BinaryUtils.setBoolean(res[0], currentDestIndex + j, true);
                    }
                }
                currentSrcIndex -= skipLen;
                currentDestIndex -= skipLen;
            }
        }
        return Arrays.stream(res).map(x -> SquareZ2Vector.create(totalCompareNum, x, this.isPlain())).toArray(SquareZ2Vector[]::new);
    }
}
