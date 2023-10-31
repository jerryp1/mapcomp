package edu.alibaba.mpc4j.common.circuit.z2.psorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.util.Arrays;
import java.util.stream.IntStream;

public class PSorterUtils {

    /**
     * 得到1<<(log2-1)长度的mask的值
     */
    public static byte[][] returnCompareResultMask(int log2) {
        byte[][] compareResultMask = new byte[log2 - 1][];
        // 前三个分别是01010101..., 00110011..., 00001111...
        int byteNum = log2 < 4 ? 1 : 1 << (log2 - 4);
        IntStream.range(0, log2 - 1).parallel().forEach(i -> {
            byte[] tmpByte = new byte[byteNum];
            if (i == 0) {
                Arrays.fill(tmpByte, (byte) 0b01010101);
            } else if (i == 1) {
                Arrays.fill(tmpByte, (byte) 0b00110011);
            } else if (i == 2) {
                Arrays.fill(tmpByte, (byte) 0b00001111);
            } else {
                int interval = 1 << (i - 3);
                int groupNum = 1 << (log2 - 2 - i);
                IntStream.range(0, groupNum).forEach(j ->
                    Arrays.fill(tmpByte, (2 * j + 1) * interval, (2 * j + 2) * interval, (byte) 255));
            }
            compareResultMask[i] = tmpByte;
        });
        return compareResultMask;
    }

    /**
     * 得到指定范围的binary sharing，plain的值是[0, length - 1)
     */
    public static BitVector[] getBinaryIndex(int length) {
        byte[][] compareResultMask = returnCompareResultMask(LongUtils.ceilLog2(length) + 1);
        int bitShift = (1 << LongUtils.ceilLog2(length)) - length;
        if (bitShift == 0) {
            if (length < 8) {
                return IntStream.range(0, compareResultMask.length).mapToObj(i -> {
                    BytesUtils.reduceByteArray(compareResultMask[compareResultMask.length - 1 - i], length);
                    return BitVectorFactory.create(length, compareResultMask[compareResultMask.length - 1 - i]);
                }).toArray(BitVector[]::new);
            } else {
                return IntStream.range(0, compareResultMask.length).mapToObj(i ->
                    BitVectorFactory.create(length, compareResultMask[compareResultMask.length - 1 - i])).toArray(BitVector[]::new);
            }
        } else {
            return IntStream.range(0, compareResultMask.length).mapToObj(i ->
                    BitVectorFactory.create(length, BytesUtils.keepLastBits(BytesUtils.shiftRight(compareResultMask[compareResultMask.length - 1 - i], bitShift), length)))
                .toArray(BitVector[]::new);
        }
    }

    public static byte[] extendBitsWithSkip(MpcZ2Vector data, int destBitLen, int skipLen) {
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
                if (data.getBitVector().get(i)) {
                    BinaryUtils.setBoolean(destByte, destOffset, true);
                    BinaryUtils.setBoolean(destByte, destOffset + skipLen, true);
                }
            }
        }
        // 处理后续满skipLen的数据
        byte[] srcByte = data.getBitVector().getBytes();
        if (skipLen >= 8) {
            int eachByteNum = skipLen >> 3, eachPartNum = eachByteNum << 1;
            int srcEndIndex = srcByte.length, destEndIndex = destByteNum;
            for (int i = groupNum - 1; i >= notFullNum; i--, destEndIndex -= eachPartNum, srcEndIndex -= eachByteNum) {
                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte, destEndIndex - eachByteNum, eachByteNum);
                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte, destEndIndex - eachPartNum, eachByteNum);
            }
        } else {
            int andNum = (1<<skipLen) - 1;
            int[] destShiftLeftBit;
            if (skipLen == 4) {
                destShiftLeftBit = new int[]{0, 0};
            } else if (skipLen == 2) {
                destShiftLeftBit = new int[]{0, 4, 0, 4};
            } else {
                destShiftLeftBit = new int[]{0, 2, 4, 6, 0, 2, 4, 6};
            }
            int groupInEachSrcByte = Byte.SIZE / skipLen;
            int[] groupInByteNum = new int[]{groupInEachSrcByte >> 1, groupInEachSrcByte};
            int currentDestByteIndex = destByteNum - 1, currentSrcByteIndex = srcByte.length - 1;

            int fullByteNum = (groupNum - notFullNum) / groupInEachSrcByte * groupInEachSrcByte;
            for (int i = 0; i < fullByteNum; i += groupInEachSrcByte, currentSrcByteIndex--) {
                int j = 0, currentSrc = srcByte[currentSrcByteIndex];
                for (int splitTwoByte = 0; splitTwoByte < 2; splitTwoByte++) {
                    byte record = 0x00;
                    for (; j < groupInByteNum[splitTwoByte]; j++) {
                        int tmp = (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                        byte dupValue = (byte) (tmp ^ (tmp << skipLen));
                        record ^= dupValue;
                    }
                    destByte[currentDestByteIndex--] = record;
                }
            }
            // 处理不满byte的数据
            if (fullByteNum != groupNum - notFullNum) {
                int lastGroupNum = groupNum - notFullNum - fullByteNum;
                int j = 0, currentSrc = srcByte[currentSrcByteIndex];
                for (int splitTwoByte = 0; splitTwoByte < 2 && j < lastGroupNum; splitTwoByte++) {
                    byte record = 0x00;
                    for (; j < groupInByteNum[splitTwoByte] && j < lastGroupNum; j++) {
                        int tmp = (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                        byte dupValue = (byte) (tmp ^ (tmp << skipLen));
                        record ^= dupValue;
                    }
                    destByte[currentDestByteIndex--] ^= record;
                }
            }
        }
//        else {
//            // todo 先用最简单的方法处理
//            int eachPartBit = skipLen << 1;
//            int currentDestIndex = (destByteNum << 3) - eachPartBit, currentSrcIndex = (srcByte.length << 3) - skipLen;
//            for (int i = groupNum - 1; i >= notFullNum; i--) {
//                for (int j = 0; j < skipLen; j++) {
//                    if (BinaryUtils.getBoolean(srcByte, currentSrcIndex + j)) {
//                        BinaryUtils.setBoolean(destByte, currentDestIndex + j, true);
//                        BinaryUtils.setBoolean(destByte, currentDestIndex + j + skipLen, true);
//                    }
//                }
//                currentSrcIndex -= skipLen;
//                currentDestIndex -= eachPartBit;
//            }
//        }
//
//        byte[] baselineByte = new byte[destByteNum];
//        if (notFullNum > 0) {
//            // 如果第一个组是未满的，则之前的比较一定是从第一个开始取数比较的
//            int destOffset = (destByteNum << 3) - destBitLen;
//            int firstLen = destBitLen % skipLen;
//            for (int i = 0; i < firstLen; i++, destOffset++) {
//                if (data.getBitVector().get(i)) {
//                    BinaryUtils.setBoolean(baselineByte, destOffset, true);
//                    BinaryUtils.setBoolean(baselineByte, destOffset + skipLen, true);
//                }
//            }
//        }
//        int eachPartBit = skipLen << 1;
//        int currentDestIndex = (destByteNum << 3) - eachPartBit, currentSrcIndex = (srcByte.length << 3) - skipLen;
//        for (int i = groupNum - 1; i >= notFullNum; i--) {
//            for (int j = 0; j < skipLen; j++) {
//                if (BinaryUtils.getBoolean(srcByte, currentSrcIndex + j)) {
//                    BinaryUtils.setBoolean(baselineByte, currentDestIndex + j, true);
//                    BinaryUtils.setBoolean(baselineByte, currentDestIndex + j + skipLen, true);
//                }
//            }
//            currentSrcIndex -= skipLen;
//            currentDestIndex -= eachPartBit;
//        }
//        if(!Arrays.equals(baselineByte, destByte)){
//            int a = 0;
//        }
        return destByte;
    }

    public static byte[][] getBitsWithSkip(MpcZ2Vector data, int totalBitNum, int skipLen) {
        MathPreconditions.checkEqual("skipLen", "2^k", 1 << LongUtils.ceilLog2(skipLen), skipLen);
        byte[] srcByte = data.getBitVector().getBytes();
        int destByteNum = CommonUtils.getByteLength(totalBitNum);
        byte[][] destByte = new byte[2][destByteNum];
        int groupNum = totalBitNum / skipLen + (totalBitNum % skipLen > 0 ? 1 : 0);

        // 如果第一个的length和其他的不一致，则先处理第一个
        if (totalBitNum % skipLen > 0) {
            int destOffset = (destByteNum << 3) - totalBitNum;
            int firstLen = totalBitNum % skipLen;
            for (int i = 0; i < firstLen; i++, destOffset++) {
                if (data.getBitVector().get(i)) {
                    BinaryUtils.setBoolean(destByte[0], destOffset, true);
                }
                if (data.getBitVector().get(i + skipLen)) {
                    BinaryUtils.setBoolean(destByte[1], destOffset, true);
                }
            }
        }
        // 处理后续满skipLen的数据
        int notFullNum = totalBitNum % skipLen > 0 ? 1 : 0;
        if (skipLen >= 8) {
            int eachByteNum = skipLen >> 3, eachPartNum = eachByteNum << 1;
            int srcEndIndex = srcByte.length, destEndIndex = destByteNum;
            for (int i = groupNum - 1; i >= notFullNum; i--, destEndIndex -= eachByteNum, srcEndIndex -= eachPartNum) {
                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte[1], destEndIndex - eachByteNum, eachByteNum);
                System.arraycopy(srcByte, srcEndIndex - eachPartNum, destByte[0], destEndIndex - eachByteNum, eachByteNum);
            }
        }
        else {
            int andNum = (1 << skipLen) - 1;
            int[] destShiftLeftBit;
            if (skipLen == 4) {
                destShiftLeftBit = new int[]{0, 4};
            } else if (skipLen == 2) {
                destShiftLeftBit = new int[]{0, 2, 4, 6};
            } else {
                destShiftLeftBit = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
            }
            int groupInEachDestByte = Byte.SIZE / skipLen;
            int[] groupInByteNum = new int[]{groupInEachDestByte >> 1, groupInEachDestByte};
            int currentDestByteIndex = destByteNum - 1, currentSrcByteIndex = srcByte.length - 1;

            int fullByteNum = (groupNum - notFullNum) / groupInEachDestByte * groupInEachDestByte;
            for (int i = 0; i < fullByteNum; i += groupInEachDestByte) {
                int j = 0, record0 = 0x00, record1 = 0x00;
                for (int splitTwoByte = 0; splitTwoByte < 2; splitTwoByte++, currentSrcByteIndex--) {
                    int currentSrc = srcByte[currentSrcByteIndex] & 0xff;
                    for (; j < groupInByteNum[splitTwoByte]; j++) {
                        record1 ^= (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                        record0 ^= (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                    }
                }
                destByte[0][currentDestByteIndex] = (byte) record0;
                destByte[1][currentDestByteIndex--] = (byte) record1;
            }
            // 处理不满byte的数据
            if (fullByteNum != groupNum - notFullNum) {
                int lastGroupNum = groupNum - notFullNum - fullByteNum;
                int j = 0, record0 = 0x00, record1 = 0x00;
                for (int splitTwoByte = 0; splitTwoByte < 2 && j < lastGroupNum; splitTwoByte++, currentSrcByteIndex--) {
                    int currentSrc = srcByte[currentSrcByteIndex] & 0xff;
                    for (; j < groupInByteNum[splitTwoByte] && j < lastGroupNum; j++) {
                        record1 ^= (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                        record0 ^= (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                    }
                }
                destByte[0][currentDestByteIndex] ^= (byte) record0;
                destByte[1][currentDestByteIndex] ^= (byte) record1;
            }
        }

//        byte[][] baselineByte = new byte[2][destByteNum];
//        // 如果第一个的length和其他的不一致，则先处理第一个
//        if (totalBitNum % skipLen > 0) {
//            int destOffset = (destByteNum << 3) - totalBitNum;
//            int firstLen = totalBitNum % skipLen;
//            for (int i = 0; i < firstLen; i++, destOffset++) {
//                if (data.getBitVector().get(i)) {
//                    BinaryUtils.setBoolean(baselineByte[0], destOffset, true);
//                }
//                if (data.getBitVector().get(i + skipLen)) {
//                    BinaryUtils.setBoolean(baselineByte[1], destOffset, true);
//                }
//            }
//        }
//        int currentDestIndex = (destByteNum << 3) - skipLen, currentSrcIndex = (srcByte.length << 3) - skipLen;
//        for (int i = groupNum - 1; i >= notFullNum; i--) {
//            for (int j = 0; j < skipLen; j++) {
//                if (BinaryUtils.getBoolean(srcByte, currentSrcIndex + j)) {
//                    BinaryUtils.setBoolean(baselineByte[1], currentDestIndex + j, true);
//                }
//            }
//            currentSrcIndex -= skipLen;
//            for (int j = 0; j < skipLen; j++) {
//                if (BinaryUtils.getBoolean(srcByte, currentSrcIndex + j)) {
//                    BinaryUtils.setBoolean(baselineByte[0], currentDestIndex + j, true);
//                }
//            }
//            currentSrcIndex -= skipLen;
//            currentDestIndex -= skipLen;
//        }
//        if(!Arrays.equals(baselineByte[0], destByte[0])){
//            int a = 0;
//        }
//        if(!Arrays.equals(baselineByte[1], destByte[1])){
//            int b = 0;
//        }


        return destByte;
    }

}
