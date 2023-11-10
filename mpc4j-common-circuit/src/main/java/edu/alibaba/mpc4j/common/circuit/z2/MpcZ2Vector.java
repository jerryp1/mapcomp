package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.stream.IntStream;

/**
 * MPC Bit Vector.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public interface MpcZ2Vector extends MpcVector {
    /**
     * Get the inner bit vector.
     *
     * @return the inner bit vector.
     */
    BitVector getBitVector();

    /**
     * Gets the number of bit shares.
     *
     * @return the number of bit shares.
     */
    default int bitNum() {
        return getNum();
    }

    /**
     * Gets the num in bytes.
     *
     * @return the num in bytes.
     */
    int byteNum();

    /**
     * Reverse the inputs.
     *
     * @param inputs the inputs.
     * @return the reversed inputs.
     */
    static MpcZ2Vector[] reverse(MpcZ2Vector[] inputs) {
        MpcZ2Vector[] result = new MpcZ2Vector[inputs.length];
        IntStream.range(0, inputs.length).forEach(i -> result[i] = inputs[inputs.length - i - 1]);
        return result;
    }

    /**
     * extend the bits of specific positions with fixed skip length from the end to the front.
     * if destBitLen % skipLen > 0, then there are 0s in the first group.
     * For example, given data = abc, skipLen = 2 and destBitLen = 5
     * the return vectors are [abcbc]
     * given data = abcde, skipLen = 4 and totalBitNum = 13
     * the return vectors are [a000a,bcdebcde]
     *
     * @param destBitLen the bit length of target value
     * @param skipLen the skip length in extending
     */
    MpcZ2Vector extendBitsWithSkip(int destBitLen, int skipLen);

    /**
     * get the bits of specific positions with fixed skip length from the end to the front.
     * For example, given data = abcdefg, skipLen = 2 and totalBitNum = 3
     * the return vectors are [ade, cfg]
     * given data = a,bcdefghi, skipLen = 1 and totalBitNum = 4
     * the return vectors are [bdfh, cegi]
     *
     * @param totalBitNum how many bits need to take out
     * @param skipLen the fixed skip length
     */
    MpcZ2Vector[] getBitsWithSkip(int totalBitNum, int skipLen);

    /**
     * set the values of specific continuous positions.
     *
     * @param startByteIndex set the values of bytes[startByteIndex, startByteIndex + data.length].
     * @param data src data
     */
    default void setValues(int startByteIndex, byte[] data){
        assert startByteIndex >= 0 && data != null;
        MathPreconditions.checkGreaterOrEqual("byteNum >= startByteIndex + data.length", byteNum(), startByteIndex + data.length);
        getBitVector().setValues(startByteIndex, data);
    }

    /**
     * 基于一定间隔，得到部分bit的数据
     * @param startPos 从哪一个位置开始取
     * @param num 取多少个bit
     * @param skipLen 取位的间隔是多少个bit
     */
    MpcZ2Vector getPointsWithFixedSpace(int startPos, int num, int skipLen);

    /**
     * 基于一定间隔，设置部分bit的数据
     * @param source 从哪一个wire取数据
     * @param startPos 从哪一个位置开始置位
     * @param num 设置多少个bit
     * @param skipLen 置位的间隔是多少个bit
     */
    default void setPointsWithFixedSpace(MpcZ2Vector source, int startPos, int num, int skipLen){
        assert isPlain() == source.isPlain();
        getBitVector().setPointsWithFixedSpace(source.getBitVector(), startPos, num, skipLen);
    }
}
