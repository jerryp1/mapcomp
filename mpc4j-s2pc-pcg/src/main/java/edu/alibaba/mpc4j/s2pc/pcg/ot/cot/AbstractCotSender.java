package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public abstract class AbstractCotSender extends AbstractTwoPartyPto implements CotSender {
    /**
     * 配置项
     */
    protected final CotConfig config;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 最大单轮数量
     */
    protected int maxRoundNum;
    /**
     * 更新数量
     */
    protected long updateNum;
    /**
     * 单次数量
     */
    protected int num;

    public AbstractCotSender(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, CotConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta, int maxRoundNum, int updateNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositiveInRangeClosed("maxRoundNum", maxRoundNum, config.maxBaseNum());
        this.maxRoundNum = maxRoundNum;
        MathPreconditions.checkGreaterOrEqual("updateNum", updateNum, maxRoundNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxRoundNum);
        this.num = num;
        extraInfo ++;
    }
}
