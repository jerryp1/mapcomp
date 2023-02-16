package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * DPPRF abstract sender.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractDpprfSender extends AbstractTwoPartyPto implements DpprfSender {
    /**
     * 配置项
     */
    private final DpprfConfig config;
    /**
     * 最大α上界
     */
    protected int maxAlphaBound;
    /**
     * 最大α比特长度
     */
    protected int maxH;
    /**
     * 最大批处理数量
     */
    protected int maxBatchNum;
    /**
     * α上界
     */
    protected int alphaBound;
    /**
     * α比特长度
     */
    protected int h;
    /**
     * 批处理数量
     */
    protected int batchNum;

    protected AbstractDpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, DpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxBatchNum, int maxAlphaBound) {
        MathPreconditions.checkPositive("maxBatchNum", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        MathPreconditions.checkPositive("maxAlphaBound", maxAlphaBound);
        this.maxAlphaBound = maxAlphaBound;
        maxH = LongUtils.ceilLog2(maxAlphaBound, 1);
        initState();
    }

    protected void setPtoInput(int batchNum, int alphaBound) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batchNum", batchNum, maxBatchNum);
        this.batchNum = batchNum;
        MathPreconditions.checkPositiveInRangeClosed("alphaBound", alphaBound, maxAlphaBound);
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound, 1);
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, alphaBound);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preSenderOutput.getNum(), DpprfFactory.getPrecomputeNum(config, batchNum, alphaBound)
        );
    }
}
