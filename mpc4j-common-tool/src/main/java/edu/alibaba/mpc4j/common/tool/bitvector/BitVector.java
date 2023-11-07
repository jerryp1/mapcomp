package edu.alibaba.mpc4j.common.tool.bitvector;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.math.BigInteger;

/**
 * Bit Vector.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public interface BitVector {
    /**
     * Get BitVector type.
     *
     * @return BitVector type.
     */
    BitVectorFactory.BitVectorType getType();

    /**
     * Set the value at the given index.
     *
     * @param index the index.
     * @param value the value.
     */
    void set(int index, boolean value);

    /**
     * Get the value at the index.
     *
     * @param index the index.
     * @return the value at the index.
     */
    boolean get(int index);

    /**
     * Copy the bit vector.
     *
     * @return the copied bit vector.
     */
    BitVector copy();

    /**
     * Replace the bit vector with the copied given bit vector.
     *
     * @param that the other bit vector.
     */
    void replaceCopy(BitVector that);

    /**
     * Get the number of bits in the bit vector.
     *
     * @return the number of bits in the bit vector.
     */
    int bitNum();

    /**
     * Get the number of bytes in the bit vector.
     *
     * @return the number of bytes in the bit vector.
     */
    int byteNum();

    /**
     * Get the bit vector represented by bytes.
     *
     * @return the bit vector represented by bytes.
     */
    byte[] getBytes();

    /**
     * Get the bit vector represented by non-negative BigInteger. Return 0 if the number of bits is 0.
     *
     * @return the bit vector represented by non-negative BigInteger.
     */
    BigInteger getBigInteger();

    /**
     * Split a bit vector with the given number of bits. The current bit vector keeps the remaining bits.
     *
     * @param bitNum the assigned number of bits.
     * @return the split bit vector.
     */
    BitVector split(int bitNum);

    /**
     * Reduce the bit vector with the given number of bits.
     *
     * @param bitNum the assigned number of bits.
     */
    void reduce(int bitNum);

    /**
     * Merge the other bit vector.
     *
     * @param that the other bit vector.
     */
    void merge(BitVector that);

    /**
     * XOR operation.
     *
     * @param that the other bit vector.
     * @return the XOR result.
     */
    BitVector xor(BitVector that);

    /**
     * Inner XOR operation.
     *
     * @param that the other bit vector.
     */
    void xori(BitVector that);

    /**
     * AND operation.
     *
     * @param that the other bit vector.
     * @return the AND result.
     */
    BitVector and(BitVector that);

    /**
     * Inner AND operation.
     *
     * @param that the other bit vector.
     */
    void andi(BitVector that);

    /**
     * OR operation.
     *
     * @param that the other bit vector.
     * @return the OR result.
     */
    BitVector or(BitVector that);

    /**
     * Inner OR operation.
     *
     * @param that the other bit vector.
     */
    void ori(BitVector that);

    /**
     * NOT operation.
     *
     * @return the NOT result.
     */
    BitVector not();

    /**
     * Inner NOT operation.
     */
    void noti();

    /**
     * set the values of specific continuous positions.
     *
     * @param startByteIndex set the values of bytes[startByteIndex, startByteIndex + data.length].
     * @param data src data
     */
    void setValues(int startByteIndex, byte[] data);

    /**
     * pad zeros in the front of bits to make the valid bit length = targetBitLength
     *
     * @param targetBitLength the target bit length
     */
    void extendLength(int targetBitLength);

    BitVector[] splitWithPadding(int[] bitNums);

    /**
     * 基于一定间隔，得到部分bit的数据，如果最后一个超出了范围，则取最后一个bit
     * @param startPos 从哪一个位置开始取
     * @param num 取多少个bit
     * @param skipLen 取位的间隔是多少个bit
     */
    default BitVector getPointsWithFixedSpace(int startPos, int num, int skipLen){
        MathPreconditions.checkNonNegative("startPos", startPos);
        MathPreconditions.checkPositive("num", num);
        MathPreconditions.checkPositive("skipLen", skipLen);
        MathPreconditions.checkGreaterOrEqual("bitNum() > startPos + (num - 2) * skipLen", bitNum(), startPos + (num - 2) * skipLen);
        BitVector res = BitVectorFactory.createZeros(num);
        for(int i = 0, pos = startPos; i < num; i++, pos += skipLen){
            pos = (i == num - 1 && pos >= bitNum()) ? bitNum() - 1 : pos;
            if(get(pos)){
                res.set(i, true);
            }
        }
        return res;
    }
    /**
     * 基于一定间隔，设置部分bit的数据，如果最后一个超出了范围，则设置最后一个bit
     * @param source 从哪一个wire取数据
     * @param startPos 从哪一个位置开始置位
     * @param num 设置多少个bit
     * @param skipLen 置位的间隔是多少个bit
     */
    default void setPointsWithFixedSpace(BitVector source, int startPos, int num, int skipLen){
        MathPreconditions.checkNonNegative("startPos", startPos);
        MathPreconditions.checkPositive("num", num);
        MathPreconditions.checkPositive("skipLen", skipLen);
        MathPreconditions.checkGreaterOrEqual("bitNum() > startPos + (num - 2) * skipLen", bitNum(), startPos + (num - 2) * skipLen);
        for(int i = 0, targetIndex = startPos; i < num; i++, targetIndex += skipLen){
            targetIndex = (i == num - 1 && targetIndex >= bitNum()) ? bitNum() - 1 : targetIndex;
            set(targetIndex, source.get(i));
        }
    }
}
