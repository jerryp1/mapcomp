package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Abstract shared permutation sender.
 *
 * @author Li Peng
 * @date 2023/10/25
 */
public abstract class AbstractSharedPermutationParty extends AbstractTwoPartyPto implements SharedPermutationParty {
    /**
     * max l
     */
    protected int maxL;
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num of elements in single vector.
     */
    protected int num;
    /**
     * l.
     */
    protected int l;
    /**
     * l in bytes
     */
    protected int byteL;
    /**
     * inputs
     */
    protected SquareZ2Vector[] inputs;

    protected AbstractSharedPermutationParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, SharedPermutationConfig config) {
        super(ptoDesc, rpc, otherParty, config);
        l = config.getZl().getL();
        byteL = config.getZl().getByteL();
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        initState();
    }

    protected void setPtoInput(Vector<byte[]> perms, Vector<byte[]> x) {
        num = perms.size();
        MathPreconditions.checkEqual("perms.size", "perms.size", perms.size(), x.size());
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
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

    /**
     * Merge list of vectors into single vector.
     *
     * @param x input vectors.
     * @return merged vector.
     */
    protected Vector<byte[]> merge(List<Vector<byte[]>> x) {
        Vector<byte[]> result = new Vector<>();
        for (int i = 0; i < num; i++) {
            byte[] allByteArrays = new byte[x.size() * byteL];
            ByteBuffer buff = ByteBuffer.wrap(allByteArrays);
            for (Vector<byte[]> bytes : x) {
                buff.put(bytes.elementAt(i));
            }
            result.add(buff.array());
        }
        // update byteL
        byteL = byteL * x.size();
        return result;
    }

    /**
     * Split single vector into list of vectors.
     *
     * @param x      single vector.
     * @param length number of result list.
     * @return list of vectors.
     */
    protected List<Vector<byte[]>> split(Vector<byte[]> x, int length) {
        // update byteL
        byteL = byteL / length;
        List<Vector<byte[]>> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(new Vector<>());
        }
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < length; j++) {
                byte[] temp = new byte[byteL];
                System.arraycopy(x.elementAt(i), j * byteL, temp, 0, byteL);
                result.get(j).add(temp);
            }
        }
        return result;
    }

    /**
     * Reverse the permutation.
     *
     * @param perm permutation.
     * @return reversed permutation.
     */
    protected int[] reversePermutation(int[] perm) {
        int[] result = new int[perm.length];
        for (int i = 0; i < perm.length; i++) {
            result[perm[i]] = i;
        }
        return result;
    }
}
