package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Abstract shuffle sender.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public abstract class AbstractShuffleParty extends AbstractTwoPartyPto implements ShuffleParty {
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

    protected AbstractShuffleParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, ShuffleConfig config) {
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

    protected void setPtoInput(List<Vector<byte[]>> x) {
        num = x.get(0).size();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
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
}
