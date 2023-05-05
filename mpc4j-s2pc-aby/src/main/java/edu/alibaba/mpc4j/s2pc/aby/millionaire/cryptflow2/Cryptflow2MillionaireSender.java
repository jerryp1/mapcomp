package edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.AbstractMillionaireParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;

/**
 * Cryptflow2 Millionaire Protocol Sender.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class Cryptflow2MillionaireSender extends AbstractMillionaireParty {
    /**
     * COT sender.
     */
    private final CotSender cotSender;

    public Cryptflow2MillionaireSender(Rpc senderRpc, Party receiverParty, Cryptflow2MillionaireConfig config) {
        super(Cryptflow2MillionairePtoDesc.getInstance(), senderRpc, receiverParty, config);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPtos(cotSender);
    }
}
