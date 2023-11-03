package edu.alibaba.mpc4j.s2pc.aby.basics.a2b;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Arrays;

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
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi) {
        // todo 如果要检查l的大小，为什么不在init的时候检查，这里没有用到xi的信息
        l = zl.getL();
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
        byteL = zl.getByteL();
        input = xi;
    }

    protected void setPtoInputs(SquareZlVector[] xi) {
        assert xi != null;
        l = zl.getL();
        for (SquareZlVector zlVector : xi) {
            MathPreconditions.checkEqual("l", "xi[i].l", zlVector.getZlVector().getZl().getL(), l);
        }
        num = Arrays.stream(xi).mapToInt(SquareZlVector::getNum).sum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
        byteL = zl.getByteL();
    }
}
