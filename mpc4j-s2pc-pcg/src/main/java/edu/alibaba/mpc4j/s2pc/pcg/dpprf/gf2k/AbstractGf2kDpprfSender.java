package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * GF2K-DPPRF发送方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractGf2kDpprfSender extends AbstractSecureTwoPartyPto implements Gf2kDpprfSender {
    /**
     * 配置项
     */
    private final Gf2kDpprfConfig config;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 最大α上界
     */
    protected int maxAlphaBound;
    /**
     * 最大l取值
     */
    protected int maxL;
    /**
     * 最大批处理数量
     */
    protected int maxBatchNum;
    /**
     * α上界
     */
    protected int alphaBound;
    /**
     * l取值
     */
    protected int l;
    /**
     * 批处理数量
     */
    protected int batchNum;

    protected AbstractGf2kDpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kDpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public Gf2kDpprfFactory.Gf2kDpprfType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int maxBatchNum, int maxAlphaBound) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        this.delta = BytesUtils.clone(delta);
        assert maxBatchNum > 0 : "maxBatchNum must be greater than 0:" + maxBatchNum;
        this.maxBatchNum = maxBatchNum;
        assert maxAlphaBound > 0 : "maxAlphaBound must be greater than 0: " + maxAlphaBound;
        this.maxAlphaBound = maxAlphaBound;
        maxL = LongUtils.ceilLog2(maxAlphaBound);
        initialized = false;
    }

    protected void setPtoInput(int batchNum, int alphaBound) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert batchNum > 0 && batchNum <= maxBatchNum : "BatchNum must be in range (0, " + maxBatchNum + "]: " + batchNum;
        this.batchNum = batchNum;
        assert alphaBound > 0 && alphaBound <= maxAlphaBound
            : "alphaBound must be in range (0, " + maxAlphaBound + "]: " + alphaBound;
        this.alphaBound = alphaBound;
        l = LongUtils.ceilLog2(alphaBound);
        extraInfo++;
    }

    protected void setPtoInput(int batch, int alphaBound, CotSenderOutput preSenderOutput) {
        setPtoInput(batch, alphaBound);
        assert preSenderOutput.getNum() >= Gf2kDpprfFactory.getPrecomputeNum(config, batch, alphaBound);
    }
}
