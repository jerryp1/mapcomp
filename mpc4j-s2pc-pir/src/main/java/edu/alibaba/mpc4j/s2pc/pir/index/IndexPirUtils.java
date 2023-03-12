package edu.alibaba.mpc4j.s2pc.pir.index;

import java.util.Arrays;

/**
 * 索引PIR协议工具类。
 *
 * @author Liqiang Peng
 * @date 2023/3/1
 */
public class IndexPirUtils {

    /**
     * 返回多项式包含的元素数量。
     *
     * @param elementByteLength 元素字节长度。
     * @param polyModulusDegree 多项式阶。
     * @param coeffBitLength    系数比特长度。
     * @return 多项式包含的元素数量。
     */
    public static int elementSizeOfPlaintext(int elementByteLength, int polyModulusDegree, int coeffBitLength) {
        int coeffSizeOfElement = coeffSizeOfElement(elementByteLength, coeffBitLength);
        int elementSizeOfPlaintext = polyModulusDegree / coeffSizeOfElement;
        assert elementSizeOfPlaintext > 0 :
            "N should be larger than the of coefficients needed to represent a database element";
        return elementSizeOfPlaintext;
    }

    /**
     * 返回表示单个元素所需的系数个数。
     *
     * @param elementByteLength 元素字节长度。
     * @param coeffBitLength    系数比特长度。
     * @return 表示单个元素所需的系数个数。
     */
    public static int coeffSizeOfElement(int elementByteLength, int coeffBitLength) {
        return (int) Math.ceil(Byte.SIZE * elementByteLength / (double) coeffBitLength);
    }

    /**
     * 将字节数组转换为指定比特长度的long型数组。
     *
     * @param limit     long型数值的比特长度。
     * @param offset    移位。
     * @param size      待转换的字节数组长度。
     * @param byteArray 字节数组。
     * @return long型数组。
     */
    public static long[] convertBytesToCoeffs(int limit, int offset, int size, byte[] byteArray) {
        // 需要使用的系数个数
        int longArraySize = (int) Math.ceil(Byte.SIZE * size / (double) limit);
        long[] longArray = new long[longArraySize];
        int room = limit;
        int flag = 0;
        for (int i = 0; i < size; i++) {
            int src = byteArray[i+offset];
            if (src < 0) {
                src &= 0xFF;
            }
            int rest = Byte.SIZE;
            while (rest != 0) {
                if (room == 0) {
                    flag++;
                    room = limit;
                }
                int shift = Math.min(room, rest);
                long temp = longArray[flag] << shift;
                longArray[flag] = temp | (src >> (Byte.SIZE - shift));
                int remain = (1 << (Byte.SIZE - shift)) - 1;
                src = (src & remain) << shift;
                room -= shift;
                rest -= shift;
            }
        }
        longArray[flag] = longArray[flag] << room;
        return longArray;
    }


    /**
     * 将long型数组转换为字节数组。
     *
     * @param longArray long型数组。
     * @param logt      系数比特长度。
     * @return 字节数组。
     */
    public static byte[] convertCoeffsToBytes(long[] longArray, int logt) {
        int longArrayLength = longArray.length;
        byte[] byteArray = new byte[longArrayLength * logt / Byte.SIZE];
        int room = Byte.SIZE;
        int j = 0;
        for (long l : longArray) {
            long src = l;
            int rest = logt;
            while (rest != 0 && j < byteArray.length) {
                int shift = Math.min(room, rest);
                byteArray[j] = (byte) (byteArray[j] << shift);
                byteArray[j] = (byte) (byteArray[j] | (src >> (logt - shift)));
                src = src << shift;
                room -= shift;
                rest -= shift;
                if (room == 0) {
                    j++;
                    room = Byte.SIZE;
                }
            }
        }
        return byteArray;
    }

    /**
     * 计算各维度的坐标。
     *
     * @param retrievalIndex 索引值。
     * @param dimensionSize  各维度的长度。
     * @return 各维度的坐标。
     */
    public static int[] computeIndices(int retrievalIndex, int[] dimensionSize) {
        long product = Arrays.stream(dimensionSize).asLongStream().reduce(1, (a, b) -> a * b);
        int[] indices = new int[dimensionSize.length];
        for (int i = 0; i < dimensionSize.length; i++) {
            product /= dimensionSize[i];
            int ji = (int) (retrievalIndex / product);
            indices[i] = ji;
            retrievalIndex -= ji * product;
        }
        return indices;
    }

    /**
     * 返回输入数据的比特长度。
     *
     * @param input 输入数据。
     * @return 比特长度。
     */
    public static int getNumberOfBits(long input) {
        int count = 0;
        while (input != 0) {
            count++;
            input /= 2;
        }
        return count;
    }

    /**
     * 返回与输入数据临近的2的次方。
     *
     * @param input 输入数据。
     * @return 与输入数据临近的2的次方。
     */
    public static int getNextPowerOfTwo(int input) {
        if ((input & (input - 1)) == 0) {
            return input;
        }
        int numberOfBits = getNumberOfBits(input);
        return (1 << numberOfBits);
    }
}
