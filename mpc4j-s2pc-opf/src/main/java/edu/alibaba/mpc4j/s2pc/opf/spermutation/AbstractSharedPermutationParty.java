package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleUtils;

import java.util.Arrays;
import java.util.Vector;

/**
 * Abstract shared permutation sender.
 *
 */
public abstract class AbstractSharedPermutationParty extends AbstractTwoPartyPto implements SharedPermutationParty {
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

    protected AbstractSharedPermutationParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, SharedPermutationConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(Vector<byte[]> perms, Vector<byte[]> x) {
        num = perms.size();
        MathPreconditions.checkEqual("perms.size", "perms.size", perms.size(), x.size());
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
    }

    @Override
    public SquareZ2Vector[][] permute(SquareZ2Vector[] perms, SquareZ2Vector[][] x) throws MpcAbortException {
        Vector<byte[]> permRows = ShuffleUtils.mergeSecret(new SquareZ2Vector[][]{perms}, envType, parallel);
        Vector<byte[]> dataRows = ShuffleUtils.mergeSecret(x, envType, parallel);
        Vector<byte[]> res = permute(permRows, dataRows);
        return ShuffleUtils.splitSecret(res, Arrays.stream(x).mapToInt(single -> single.length).toArray(), envType, parallel);
    }
}
