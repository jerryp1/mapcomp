package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * PCOT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public abstract class AbstractPcotSender extends AbstractSecureTwoPartyPto implements PcotSender {
    /**
     * 配置项
     */
    private final PcotConfig config;
    /**
     * 预计算发送方输出
     */
    protected CotSenderOutput preSenderOutput;

    protected AbstractPcotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, PcotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public PcotFactory.PcotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput() {
        initialized = false;
    }

    protected void setPtoInput(CotSenderOutput preSenderOutput) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert preSenderOutput.getNum() > 0;
        this.preSenderOutput = preSenderOutput;
        extraInfo++;
    }
}
