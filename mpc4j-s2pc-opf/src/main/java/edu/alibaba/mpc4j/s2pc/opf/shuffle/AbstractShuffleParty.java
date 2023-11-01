package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract shuffle sender.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public abstract class AbstractShuffleParty extends AbstractTwoPartyPto implements ShuffleParty {
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

    protected AbstractShuffleParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, ShuffleConfig config) {
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

    @Override
    public SquareZ2Vector[][] shuffle(SquareZ2Vector[][] x, int[] randomPerm) throws MpcAbortException {
        int[] bitLens = Arrays.stream(x).mapToInt(arr -> arr.length).toArray();
        return ShuffleUtils.splitSecret(shuffle(Collections.singletonList(
            ShuffleUtils.mergeSecret(x, envType, parallel)), randomPerm).get(0), bitLens, envType, parallel);
    }

    @Override
    public SquareZ2Vector[][] randomShuffle(SquareZ2Vector[][] x) throws MpcAbortException {
        int[] randomPerm = ShuffleUtils.generateRandomPerm(x[0][0].bitNum());
        return shuffle(x, randomPerm);
    }
}
