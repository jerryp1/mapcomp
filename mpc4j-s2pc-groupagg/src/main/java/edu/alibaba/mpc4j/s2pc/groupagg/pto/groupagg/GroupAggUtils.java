package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * Group aggregation utilities.
 *
 * @author Li Peng
 * @date 2023/11/9
 */
public class GroupAggUtils {
    /**
     * Get a full set of string of integers from [0,2^bitLength).
     */
    public static String[] genStringSetFromRange(int bitLength) {
        Preconditions.checkArgument(bitLength < CommonConstants.MAX_GROUP_BIT_LENGTH,
            "bit length of group out of range");
        int num = 1 << bitLength;
        String[] result = new String[num];
        StringBuilder builder;
        for (int i = 0; i < num; i++) {
            String numBinary = Integer.toBinaryString(i);
            builder = new StringBuilder();
            for (int j = 0; j < bitLength - numBinary.length(); j++) {
                builder.append("0");
            }
            builder.append(numBinary);
            result[i] = builder.toString();
        }
        return result;
    }

    /**
     * Apply permutation to inputs.
     *
     * @param x    inputs.
     * @param perm permutation.
     * @return permuted inputs.
     */
    public static long[] applyPermutation(long[] x, int[] perm) {
        int num = perm.length;
        long[] result = new long[num];
        for (int i = 0; i < num; i++) {
            result[i] = x[perm[i]];
        }
        return result;
    }

    /**
     * Apply permutation to inputs.
     *
     * @param e    inputs.
     * @param perm permutation.
     * @return permuted inputs.
     */
    public static SquareZ2Vector applyPermutation(SquareZ2Vector e, int[] perm) {
        int num = perm.length;
        BitVector result = BitVectorFactory.createZeros(num);
        for (int i = 0; i < num; i++) {
            result.set(i, e.getBitVector().get(perm[i]));
        }
        return SquareZ2Vector.create(result, false);
    }

    /**
     * Apply permutation to inputs.
     *
     * @param x    inputs.
     * @param perm permutation.
     * @return permuted inputs.
     */
    public static String[] applyPermutation(String[] x, int[] perm) {
        int num = perm.length;
        String[] result = new String[num];
        for (int i = 0; i < num; i++) {
            result[i] = x[perm[i]];
        }
        return result;
    }

    /**
     * Obtain indicator of group.
     *
     * @param x input.
     * @return indicator of group.
     */
    public static BitVector obtainGroupIndicator(String[] x) {
        BitVector indicator = BitVectorFactory.createZeros(x.length);
        IntStream.range(0, x.length - 1).forEach(i -> indicator.set(i, !x[i].equals(x[i + 1])));
        indicator.set(x.length - 1, true);
        return indicator;
    }

    /**
     * Transpose osn result.
     *
     * @param osnPartyOutput osn party output.
     * @param l              length.
     * @return osn party output.
     */
    public static SquareZ2Vector[] transposeOsnResult(OsnPartyOutput osnPartyOutput, int l) {
        int fullL = CommonUtils.getByteLength(l) * Byte.SIZE;
        Vector<byte[]> osn = osnPartyOutput.getVector();
        SquareZ2Vector[] transpose = Arrays.stream(TransposeUtils.transposeSplit(osn, fullL))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        return Arrays.stream(transpose, 0, l).toArray(SquareZ2Vector[]::new);
    }

    /**
     * Split single vector into list of vectors.
     *
     * @param x           single vector.
     * @param byteLengths each byte length in result list.
     * @return list of vectors.
     */
    public static List<Vector<byte[]>> split(Vector<byte[]> x, int[] byteLengths) {
        int num = x.size();
        List<Vector<byte[]>> result = new ArrayList<>(byteLengths.length);
        int[] startIndex = new int[byteLengths.length];
        for (int i = 0; i < byteLengths.length; i++) {
            result.add(new Vector<>(num));
            if (i > 0) {
                startIndex[i] = startIndex[i - 1] + byteLengths[i - 1];
            }
        }
        for (int i = 0; i < num; i++) {
            byte[] current = x.elementAt(i);
            for (int j = 0; j < byteLengths.length; j++) {
                byte[] temp = new byte[byteLengths[j]];
                System.arraycopy(current, startIndex[j], temp, 0, byteLengths[j]);
                result.get(j).add(temp);
            }
        }
        return result;
    }

    /**
     * Transfer binary string to corresponding byte array.
     *
     * @param binaryString binary string
     * @return corresponding byte array.
     */
    public static Vector<byte[]> binaryStringToBytes(String[] binaryString) {
        int bitLength = binaryString[0].length();
        Vector<byte[]> result = new Vector<>(binaryString.length);
        for (String str : binaryString) {
            byte[] bytes = new byte[CommonUtils.getByteLength(bitLength)];
            IntStream.range(0, bitLength).forEach(i -> {
                if (str.charAt(i) == '1') {
                    BinaryUtils.setBoolean(bytes, i, true);
                }
            });
            result.add(bytes);
        }
        return result;
    }

    /**
     * Transfer byte array to corresponding binary string.
     *
     * @param bytes     byte array.
     * @param bitLength bit length of byte array.
     * @return corresponding binary string.
     */
    public static String[] bytesToBinaryString(Vector<byte[]> bytes, int bitLength) {
        int byteLength = CommonUtils.getByteLength(bitLength);

        MathPreconditions.checkEqual("bytes.get(0).bitLength", "byteLength",
            bytes.get(0).length, byteLength);
        String[] result = new String[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            byte[] b = bytes.get(i);
            StringBuilder builder = new StringBuilder();
            IntStream.range(0, bitLength).forEach(j -> builder.append(BinaryUtils.getBoolean(b, j) ? "1" : "0"));
            result[i] = builder.toString();
        }
        return result;
    }

    /**
     * Transfer byte array to corresponding binary string.
     *
     * @param bytes      byte array.
     * @param bitLength1 bit length of byte array1.
     * @param bitLength2 bit length of byte array2.
     * @return corresponding binary string.
     */
    public static String[] bytesToBinaryString(Vector<byte[]> bytes, int bitLength1, int bitLength2) {
        int byteLength1 = CommonUtils.getByteLength(bitLength1);
        int byteLength2 = CommonUtils.getByteLength(bitLength2);

        MathPreconditions.checkEqual("bytes.get(0).length", "byteLength1+byteLength2",
            bytes.get(0).length, byteLength1 + byteLength2);
        String[] result = new String[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            byte[] b = bytes.get(i);
            StringBuilder builder = new StringBuilder();
            IntStream.range(0, bitLength1).forEach(j -> builder.append(BinaryUtils.getBoolean(b, j) ? "1" : "0"));
            IntStream.range(0, bitLength2).forEach(j -> builder.append(BinaryUtils.getBoolean(b, j + byteLength1 * Byte.SIZE) ? "1" : "0"));
            result[i] = builder.toString();
        }
        return result;
    }
}
