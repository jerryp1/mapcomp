package edu.alibaba.mpc4j.s2pc.opf.groupagg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
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
    public static String[] genStringSetFromRange(int bitLength) {
        Preconditions.checkArgument(bitLength < CommonConstants.MAX_GROUP_BIT_LENGTH, "bit length of group out of range");
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
     * merge multiple grouping key into single one
     *
     * @param strs strings
     * @return grouping keys
     */
    public static String[] mergeString(String[][] strs) {
        return IntStream.range(0, strs[0].length).mapToObj(i ->
            Arrays.stream(strs).map(str -> str[i]).reduce("", String::concat)).toArray(String[]::new);
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
        for (int i = 0; i < num;i++) {
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

    public static SquareZ2Vector[] transposeOsnResult(OsnPartyOutput osnPartyOutput, int l) {
        int fullL = CommonUtils.getByteLength(l) * Byte.SIZE;
        byte[][] osnBytes = IntStream.range(0, osnPartyOutput.getN())
            .mapToObj(osnPartyOutput::getShare).toArray(byte[][]::new);
        SquareZ2Vector[] transpose = Arrays.stream(ZlDatabase.create(fullL, osnBytes).bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        return IntStream.range(0, l).mapToObj(i -> transpose[i]).toArray(SquareZ2Vector[]::new);
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
            result.add(new Vector<>());
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
}
