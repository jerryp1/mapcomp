package edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.AbstractMillionaireParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;

/**
 * Cryptflow2 Millionaire Protocol Receiver.
 *
 * @author Li Peng
 * @date 2023/4/25
 */
public class Cryptflow2MillionaireReceiver extends AbstractMillionaireParty {
    /**
     * COT receiver.
     */
    private final CotReceiver cotReceiver;

    public Cryptflow2MillionaireReceiver(Rpc receiverRpc, Party senderParty, Cryptflow2MillionaireConfig config) {
        super(Cryptflow2MillionairePtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPtos(cotReceiver);
    }
}
