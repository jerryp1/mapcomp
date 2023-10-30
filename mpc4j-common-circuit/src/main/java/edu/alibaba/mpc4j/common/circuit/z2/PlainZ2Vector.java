package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Plain Z2 Vector.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public class PlainZ2Vector implements MpcZ2Vector {
    /**
     * Creates a plain z2 vector with the assigned bit vector.
     *
     * @param bitVector the assigned bit vector.
     * @return a plain zl vector.
     */
    public static PlainZ2Vector create(BitVector bitVector) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = bitVector;
        return plainZ2Vector;
    }

    /**
     * Create a plain z2 vector with the assigned value.
     *
     * @param bitNum the bit num.
     * @param bytes  the assigned bits represented by bytes.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector create(int bitNum, byte[] bytes) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.create(bitNum, bytes);

        return plainZ2Vector;
    }

    /**
     * Creates a random plain z2 vector.
     *
     * @param bitNum the bit num.
     * @param random the random states.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createRandom(int bitNum, Random random) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createRandom(bitNum, random);
        return plainZ2Vector;
    }

    /**
     * Creates a plain all-one z2 vector.
     *
     * @param bitNum the bit num.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createOnes(int bitNum) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createOnes(bitNum);
        return plainZ2Vector;
    }

    /**
     * Creates a plain all-zero z2 vector.
     *
     * @param bitNum the bit num.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createZeros(int bitNum) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createZeros(bitNum);
        return plainZ2Vector;
    }

    /**
     * Creates an empty plain z2 vector.
     *
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createEmpty() {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createEmpty();
        return plainZ2Vector;
    }

    /**
     * the bit vector
     */
    private BitVector bitVector;

    /**
     * private constructor.
     */
    private PlainZ2Vector() {
        // empty
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
        return true;
    }

    @Override
    public PlainZ2Vector copy() {
        PlainZ2Vector clone = new PlainZ2Vector();
        clone.bitVector = bitVector.copy();

        return clone;
    }

    @Override
    public int getNum() {
        return bitVector.bitNum();
    }

    @Override
    public PlainZ2Vector split(int splitNum) {
        BitVector splitVector = bitVector.split(splitNum);
        return PlainZ2Vector.create(splitVector);
    }

    @Override
    public void reduce(int reduceNum) {
        bitVector.reduce(reduceNum);
    }

    @Override
    public void merge(MpcVector other) {
        PlainZ2Vector that = (PlainZ2Vector) other;
        bitVector.merge(that.getBitVector());
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
        return PlainZ2Vector.create(destBitLen, destByte);
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
        return Arrays.stream(res).map(x -> PlainZ2Vector.create(totalCompareNum, x)).toArray(PlainZ2Vector[]::new);
    }
}
