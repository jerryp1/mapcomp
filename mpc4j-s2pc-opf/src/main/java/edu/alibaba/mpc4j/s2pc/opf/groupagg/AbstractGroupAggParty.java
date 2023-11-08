package edu.alibaba.mpc4j.s2pc.opf.groupagg;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Abstract shuffle sender.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public abstract class AbstractGroupAggParty extends AbstractTwoPartyPto implements GroupAggParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num of elements in single vector.
     */
    protected int num;
    /**
     * inputs
     */
    protected SquareZ2Vector[] inputs;

    protected List<String> ownDistinctGroup;

    protected int otherDistinctGroupNum;

    protected int ownGroupByteLength;
    protected int otherGroupByteLength;

    protected AbstractGroupAggParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, GroupAggConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(List<Vector<byte[]>> x) {
        num = x.get(0).size();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
    }

    /**
     * Merge list of vectors into single vector.
     *
     * @param x input vectors.
     * @return merged vector.
     */
    protected Vector<byte[]> merge(List<Vector<byte[]>> x) {
        Vector<byte[]> result = new Vector<>();
        int byteLen = x.stream().mapToInt(single -> single.elementAt(0).length).sum();
        for (int i = 0; i < num; i++) {
            byte[] allByteArrays = new byte[byteLen];
            ByteBuffer buff = ByteBuffer.wrap(allByteArrays);
            for (Vector<byte[]> bytes : x) {
                buff.put(bytes.elementAt(i));
            }
            result.add(buff.array());
        }
        return result;
    }

    /**
     * Split single vector into list of vectors.
     *
     * @param x           single vector.
     * @param byteLengths each byte length in result list.
     * @return list of vectors.
     */
    protected List<Vector<byte[]>> split(Vector<byte[]> x, int[] byteLengths) {
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

    /**
     * merge multiple grouping key into single one
     *
     * @param strs strings
     * @return
     */
    protected String[] mergeString(String[][] strs) {
        return IntStream.range(0, strs[0].length).mapToObj(i ->
            Arrays.stream(strs).map(str -> str[i]).reduce("", String::concat)).toArray(String[]::new);
    }

    protected int[] obtainPerms(String[] keys) {
        Tuple[] tuples = IntStream.range(0, num).mapToObj(j -> new Tuple(keys[j], j)).toArray(Tuple[]::new);
        Arrays.sort(tuples);
        return IntStream.range(0, num).map(j -> tuples[j].getValue()).toArray();
    }

    private static class Tuple implements Comparable<Tuple> {
        private final String key;
        private final int value;

        public Tuple(String key, int value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }

        @Override
        public int compareTo(Tuple o) {
            return key.compareTo(o.getKey());
        }
    }

    /**
     * Apply permutation to inputs.
     *
     * @param x    inputs.
     * @param perm permutation.
     * @return permuted inputs.
     */
    protected long[] applyPermutation(long[] x, int[] perm) {
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
     * @param x    inputs.
     * @param perm permutation.
     * @return permuted inputs.
     */
    protected String[] applyPermutation(String[] x, int[] perm) {
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
    protected BitVector obtainGroupIndicator(String[] x) {
        BitVector indicator = BitVectorFactory.createZeros(x.length);
        IntStream.range(0, x.length - 1).forEach(i -> indicator.set(i, !x[i].equals(x[i + 1])));
        indicator.set(x.length - 1, true);
        return indicator;
    }

    protected SquareZ2Vector[] transposeOsnResult(OsnPartyOutput osnPartyOutput, int l) {
        byte[][] osnBytes = IntStream.range(0, osnPartyOutput.getN())
            .mapToObj(osnPartyOutput::getShare).toArray(byte[][]::new);
        ZlDatabase zlDatabase = ZlDatabase.create(l, osnBytes);
        return Arrays.stream(zlDatabase.bitPartition(EnvType.STANDARD, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
    }

    protected SquareZlVector prefixAgg(SquareZlVector input) {
        return null;
    }

    /**
     * Generates random permutation.
     *
     * @param num the number of inputs.
     * @return a random permutation of num.
     */
    protected int[] genRandomPerm(int num) {
        // generate random permutation
        List<Integer> randomPermList = IntStream.range(0, num)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(randomPermList, secureRandom);
        return randomPermList.stream().mapToInt(permutation -> permutation).toArray();
    }

    protected int[] combinePerm(int[] perm1, int[] perm2) {
        MathPreconditions.checkEqual("perm1.length", "perm2.length", perm1.length, perm2.length);
        int[] result = new int[perm1.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = perm2[perm1[i]];
        }
        return result;
    }

    List<Vector<byte[]>> splitBytes(Vector<byte[]> input, int[] nums) {
        MathPreconditions.checkEqual("length of input", "nums", Arrays.stream(nums).sum(), input.get(0).length);
        List<Vector<byte[]>> result = new ArrayList<>();
        IntStream.range(0, nums.length).forEach(i -> result.add(new Vector<>()));
        for (int i = 0; i < input.size(); i++) {
            int start = 0;
            for (int k : nums) {
                byte[] bytes = new byte[k];
                System.arraycopy(input.elementAt(i), start, bytes, 0, k);
                start += k;
            }
        }
        return result;
    }
}
