package edu.alibaba.mpc4j.s2pc.aby.basics.a2b;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Abstract A2b Party.
 *
 * @author Li Peng
 * @date 2023/10/20
 */
public abstract class AbstractA2bParty extends AbstractTwoPartyPto implements A2bParty {
    /**
     * max l
     */
    protected int maxL;
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num
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
     * l in bytes
     */
    protected int byteL;
    /**
     * input
     */
    protected SquareZlVector input;

    protected AbstractA2bParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, A2bConfig config) {
        super(ptoDesc, rpc, otherParty, config);
        zl = config.getZl();
    }


    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        // not support for l >= 64 due to limitation of Zl64database.
        MathPreconditions.checkPositiveInRangeClosed("maxL", maxL, Long.SIZE - 1);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi) {
        l = zl.getL();
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
        byteL = zl.getByteL();
        input = xi;
    }
}
