package edu.alibaba.mpc4j.common.circuit.z2.psorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;
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
     * 得到log2维度的index的值，小维度开始到大维度
     */
    public static BigInteger[] genIndex(int log2, byte[][] compareResultMask) {
        switch (log2) {
            case 1:
                return new BigInteger[]{BigInteger.ONE};
            case 2:
                return new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(5)};
            case 3:
                return new BigInteger[]{BigInteger.valueOf(15), BigInteger.valueOf(51), BigInteger.valueOf(85)};
            default: {
                int len = 1 << log2;
                int halfLen = len >> 1;
                BigInteger[] result = new BigInteger[log2];
                result[0] = BigInteger.ONE.shiftLeft(halfLen).subtract(BigInteger.ONE);
                for (int i = 1; i < log2; i++) {
                    BigInteger originTmp = BigIntegerUtils.byteArrayToNonNegBigInteger(compareResultMask[log2 - 1 - i]);
                    result[i] = originTmp.shiftLeft(halfLen).xor(originTmp);
                }
                return result;
            }
        }
    }

    /**
     * 得到指定范围的binary sharing，plain的值是[0, length - 1)
     */
    public static BitVector[] getBinaryIndex(int length) {
        byte[][] compareResultMask = returnCompareResultMask(LongUtils.ceilLog2(length));
        return getBinaryIndex(length, compareResultMask);
    }

    public static BitVector[] getBinaryIndex(int length, byte[][] compareResultMask) {
        BigInteger[] shareIndex = genIndex(LongUtils.ceilLog2(length), compareResultMask);
        BitVector[] res = new BitVector[shareIndex.length];
        int bitShift = (1 << LongUtils.ceilLog2(length)) - length;
        int byteNum = CommonUtils.getByteLength(length);
        IntStream.range(0, shareIndex.length).forEach(i -> {
            shareIndex[i] = shareIndex[i].shiftRight(bitShift);
            byte[] tmp = BigIntegerUtils.nonNegBigIntegerToByteArray(shareIndex[i], byteNum);
            res[i] = BitVectorFactory.create(length, tmp);
        });
        return res;
    }

}
