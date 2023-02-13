package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * 预计算COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public abstract class AbstractPreCotReceiver extends AbstractTwoPartyPto implements PreCotReceiver {
    /**
     * 预计算接收方输出
     */
    protected CotReceiverOutput preReceiverOutput;
    /**
     * 接收方真实选择比特
     */
    protected boolean[] choices;

    protected AbstractPreCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, PreCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(CotReceiverOutput preReceiverOutput, boolean[] choices) {
        checkReadyState();
        MathPreconditions.checkPositive("preCotNum", preReceiverOutput.getNum());
        MathPreconditions.checkEqual("num", "preCotNum", choices.length, preReceiverOutput.getNum());
        this.preReceiverOutput = preReceiverOutput;
        this.choices = choices;
        extraInfo++;
    }
}
