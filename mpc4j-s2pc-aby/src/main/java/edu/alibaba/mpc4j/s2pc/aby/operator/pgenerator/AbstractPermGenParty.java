package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Abstract permutation generator Party.
 *
 * @author Feng Han
 * @date 2023/11/03
 */
public abstract class AbstractPermGenParty extends AbstractTwoPartyPto implements PermGenParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * max bit number
     */
    protected int maxBitNum;
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
     * l in bytes
     */
    protected int byteL;
    /**
     * inputs
     */
    protected SquareZ2Vector[] inputs;

    protected AbstractPermGenParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, PermGenConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int maxNum, int maxBitNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        MathPreconditions.checkPositive("maxBitNum", maxBitNum);
        this.maxBitNum = maxBitNum;
        initState();
    }

    protected void setPtoInput(SquareZ2Vector[] xiArray) {
        num = xiArray[0].getNum();
        l = xiArray.length;
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("maxBitNum", xiArray.length, maxBitNum);
    }
}
