package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * PCOT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public abstract class AbstractPcotReceiver extends AbstractSecureTwoPartyPto implements PcotReceiver {
    /**
     * 配置项
     */
    private final PcotConfig config;
    /**
     * 预计算接收方输出
     */
    protected CotReceiverOutput preReceiverOutput;
    /**
     * 接收方真实选择比特
     */
    protected boolean[] choices;

    protected AbstractPcotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, PcotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public PcotFactory.PcotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput() {
        initialized = false;
    }

    protected void setPtoInput(CotReceiverOutput preReceiverOutput, boolean[] choices) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert preReceiverOutput.getNum() > 0;
        assert preReceiverOutput.getNum() == choices.length;
        this.preReceiverOutput = preReceiverOutput;
        this.choices = choices;
        extraInfo++;
    }
}
