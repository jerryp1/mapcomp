package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * abstract single single-point GF2K VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractSspGf2kVoleSender extends AbstractTwoPartyPto implements SspGf2kVoleSender {
    /**
     * config
     */
    private final SspGf2kVoleConfig config;
    /**
     * max num
     */
    protected int maxNum;
    /**
     * α
     */
    protected int alpha;
    /**
     * num
     */
    protected int num;

    protected AbstractSspGf2kVoleSender(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SspGf2kVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int alpha, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        MathPreconditions.checkNonNegativeInRange("α", alpha, num);
        this.alpha = alpha;
        extraInfo++;
    }

    protected void setPtoInput(int alpha, int num, Gf2kVoleSenderOutput preSenderOutput) {
        setPtoInput(alpha, num);
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preSenderOutput.getNum(), SspGf2kVoleFactory.getPrecomputeNum(config, num)
        );
    }
}
