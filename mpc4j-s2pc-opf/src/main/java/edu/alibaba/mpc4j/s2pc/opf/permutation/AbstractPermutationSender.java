package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Vector;

/**
 * Abstract permutation sender.
 */
public abstract class AbstractPermutationSender extends AbstractTwoPartyPto implements PermutationSender {
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
     * Zl instance
     */
    protected Zl zl;
    /**
     * l.
     */
    protected int l;
    /**
     * inputs
     */
    protected SquareZ2Vector[] inputs;

    protected AbstractPermutationSender(PtoDesc ptoDesc, Rpc rpc, Party otherParty, PermutationConfig config) {
        super(ptoDesc, rpc, otherParty, config);
        zl = config.getZl();
        l = zl.getL();
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZlVector perm, ZlVector x) {
        num = perm.getNum();
        MathPreconditions.checkEqual("permutation.length", "input.length", perm.getNum(), x.getNum());
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
    }

    protected void setPtoInput(Vector<byte[]> perms, Vector<byte[]> x) {
        num = perms.size();
        MathPreconditions.checkEqual("permutation.length", "input.length", perms.size(), x.size());
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
    }
}
