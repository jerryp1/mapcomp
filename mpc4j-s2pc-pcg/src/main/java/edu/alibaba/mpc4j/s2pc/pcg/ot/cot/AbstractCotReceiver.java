package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.Arrays;

/**
 * COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public abstract class AbstractCotReceiver extends AbstractTwoPartyPto implements CotReceiver {
    /**
     * 配置项
     */
    protected final CotConfig config;
    /**
     * 最大单轮数量
     */
    protected int maxRoundNum;
    /**
     * 更新数量
     */
    protected long updateNum;
    /**
     * 选择比特
     */
    protected boolean[] choices;
    /**
     * 选择比特数量
     */
    protected int num;

    public AbstractCotReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, CotConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxRoundNum, int updateNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxRoundNum", maxRoundNum, config.maxBaseNum());
        this.maxRoundNum = maxRoundNum;
        MathPreconditions.checkGreaterOrEqual("updateNum", updateNum, maxRoundNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(boolean[] choices) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", choices.length, maxRoundNum);
        // 拷贝一份
        this.choices = Arrays.copyOf(choices, choices.length);
        num = choices.length;
        extraInfo++;
    }
}
