package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Abstract Bit2a Party.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public abstract class AbstractBit2aParty extends AbstractTwoPartyPto implements Bit2aParty {
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
    protected SquareZ2Vector input;

    protected AbstractBit2aParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, Bit2aConfig config) {
        super(ptoDesc, rpc, otherParty, config);
        zl = config.getZl();
    }


    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZ2Vector xi) {
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        l = zl.getL();
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
        byteL = zl.getByteL();
        input = xi;
    }
}
