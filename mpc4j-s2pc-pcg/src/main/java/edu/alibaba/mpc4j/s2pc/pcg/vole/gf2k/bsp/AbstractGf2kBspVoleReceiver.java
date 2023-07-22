package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

import java.util.Arrays;

/**
 * abstract GF2K-BSP-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public abstract class AbstractGf2kBspVoleReceiver extends AbstractTwoPartyPto implements Gf2kBspVoleReceiver {
    /**
     * config
     */
    protected final Gf2kBspVoleConfig config;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * max num for each GF2K-SSP-VOLE
     */
    private int maxNum;
    /**
     * max batch num
     */
    protected int maxBatchNum;
    /**
     * num for each GF2K-SSP-VOLE
     */
    protected int num;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractGf2kBspVoleReceiver(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kBspVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta, int maxBatchNum, int maxEachNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("maxEachNum", maxEachNum);
        this.maxNum = maxEachNum;
        MathPreconditions.checkPositive("maxBatchNum", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void setPtoInput(int batchNum, int eachNum) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("eachNum", eachNum, maxNum);
        this.num = eachNum;
        MathPreconditions.checkPositiveInRangeClosed("batchNum", batchNum, maxBatchNum);
        this.batchNum = batchNum;
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int eachNum, Gf2kVoleReceiverOutput preReceiverOutput) {
        setPtoInput(batchNum, eachNum);
        Preconditions.checkArgument(Arrays.equals(delta, preReceiverOutput.getDelta()));
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preReceiverOutput.getNum(), Gf2kBspVoleFactory.getPrecomputeNum(config, batchNum, eachNum)
        );
    }
}
