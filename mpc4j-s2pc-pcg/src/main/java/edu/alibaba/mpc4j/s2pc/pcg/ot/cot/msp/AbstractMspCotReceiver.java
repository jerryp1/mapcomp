package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * MSP-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractMspCotReceiver extends AbstractTwoPartyPto implements MspCotReceiver {
    /**
     * 配置项
     */
    private final MspCotConfig config;
    /**
     * 最大数量
     */
    protected int maxNum;
    /**
     * 最大稀疏点数量
     */
    protected int maxT;
    /**
     * 数量
     */
    protected int num;
    /**
     * 稀疏点数量
     */
    protected int t;

    protected AbstractMspCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, MspCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxT, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        MathPreconditions.checkPositiveInRangeClosed("maxT", maxT, maxNum);
        this.maxT = maxT;
        initState();
    }

    protected void setPtoInput(int t, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        MathPreconditions.checkPositiveInRangeClosed("t", t, maxT);
        this.t = t;
        extraInfo++;
    }

    protected void setPtoInput(int t, int num, CotReceiverOutput preReceiverOutput) {
        setPtoInput(t, num);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preReceiverOutput.getNum(), MspCotFactory.getPrecomputeNum(config, t, num)
        );
    }
}
