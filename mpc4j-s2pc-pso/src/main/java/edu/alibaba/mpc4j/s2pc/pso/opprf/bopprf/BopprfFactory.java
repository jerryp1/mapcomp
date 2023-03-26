package edu.alibaba.mpc4j.s2pc.pso.opprf.bopprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.opprf.bopprf.okvs.OkvsBopprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.opprf.bopprf.okvs.OkvsBopprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.opprf.bopprf.okvs.OkvsBopprfSender;

/**
 * Batched OPRRF factory.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class BopprfFactory {
    /**
     * private constructor.
     */
    private BopprfFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum BopprfType {
        /**
         * OKVS
         */
        OKVS,
        /**
         * Table
         */
        TABLE,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static BopprfSender createBopprfSender(Rpc senderRpc, Party receiverParty, BopprfConfig config) {
        BopprfType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsBopprfSender(senderRpc, receiverParty, (OkvsBopprfConfig) config);
            case TABLE:
            default:
                throw new IllegalArgumentException("Invalid " + BopprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static BopprfReceiver createBopprfReceiver(Rpc receiverRpc, Party senderParty, BopprfConfig config) {
        BopprfType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsBopprfReceiver(receiverRpc, senderParty, (OkvsBopprfConfig) config);
            case TABLE:
            default:
                throw new IllegalArgumentException("Invalid " + BopprfType.class.getSimpleName() + ": " + type.name());
        }
    }
}
