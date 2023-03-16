package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * abstract single single-point GF2K VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractSspGf2kVoleReceiver extends AbstractTwoPartyPto implements SspGf2kVoleReceiver {
    /**
     * config
     */
    private final SspGf2kVoleConfig config;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * max num
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;

    protected AbstractSspGf2kVoleReceiver(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, SspGf2kVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }

    protected void setPtoInput(int num, Gf2kVoleReceiverOutput preReceiverOutput) {
        setPtoInput(num);
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preReceiverOutput.getNum(), SspGf2kVoleFactory.getPrecomputeNum(config, num)
        );
    }
}
