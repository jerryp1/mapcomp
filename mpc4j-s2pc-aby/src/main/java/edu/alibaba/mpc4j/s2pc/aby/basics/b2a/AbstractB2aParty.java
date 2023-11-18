package edu.alibaba.mpc4j.s2pc.aby.basics.b2a;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Abstract B2a Party.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public abstract class AbstractB2aParty extends AbstractTwoPartyPto implements B2aParty {
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
    protected SquareZ2Vector[] input;

    protected AbstractB2aParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, B2aConfig config) {
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

    protected void setPtoInput(SquareZ2Vector[] xi) {
        l = zl.getL();
        num = xi[0].getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
        byteL = zl.getByteL();
        if (xi.length < l) {
            // fill top elements with zeros.
            input = new SquareZ2Vector[l];
            for (int i = 0; i < l;i++) {
                input[i] = (i < l-xi.length) ? SquareZ2Vector.createZeros(num, false):xi[i -xi.length];
            }
            System.out.println();
        } else {
            input = xi;
        }
    }
}
