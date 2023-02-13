package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * 预计算COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public abstract class AbstractPreCotSender extends AbstractTwoPartyPto implements PreCotSender {
    /**
     * 预计算发送方输出
     */
    protected CotSenderOutput preSenderOutput;

    protected AbstractPreCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, PreCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(CotSenderOutput preSenderOutput) {
        checkReadyState();
        MathPreconditions.checkPositive("preCotNum", preSenderOutput.getNum());
        this.preSenderOutput = preSenderOutput;
        extraInfo++;
    }
}
