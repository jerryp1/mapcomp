package edu.alibaba.mpc4j.s2pc.aby.millionaire;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * Abstract Millionaire Protocol Party.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public abstract class AbstractMillionaireParty extends AbstractTwoPartyPto implements MillionaireParty {
    /**
     * maxBitNum.
     */
    protected int maxBitNum;
    /**
     * the input value bit length.
     */
    protected int l;

    protected AbstractMillionaireParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int l, int maxBitNum) {
        MathPreconditions.checkPositive("maxBitNum", maxBitNum);
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        this.maxBitNum = maxBitNum;
        initState();
    }
}
