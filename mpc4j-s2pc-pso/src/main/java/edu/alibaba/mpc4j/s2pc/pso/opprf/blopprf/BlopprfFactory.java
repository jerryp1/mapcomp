package edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.okvs.OkvsBlopprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.okvs.OkvsBlopprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.okvs.OkvsBlopprfSender;

/**
 * Batched l-bit-input OPRRF factory.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class BlopprfFactory {
    /**
     * private constructor.
     */
    private BlopprfFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum BlopprfType {
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
    public static BlopprfSender createBopprfSender(Rpc senderRpc, Party receiverParty, BlopprfConfig config) {
        BlopprfType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsBlopprfSender(senderRpc, receiverParty, (OkvsBlopprfConfig) config);
            case TABLE:
            default:
                throw new IllegalArgumentException("Invalid " + BlopprfType.class.getSimpleName() + ": " + type.name());
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
    public static BlopprfReceiver createBopprfReceiver(Rpc receiverRpc, Party senderParty, BlopprfConfig config) {
        BlopprfType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsBlopprfReceiver(receiverRpc, senderParty, (OkvsBlopprfConfig) config);
            case TABLE:
            default:
                throw new IllegalArgumentException("Invalid " + BlopprfType.class.getSimpleName() + ": " + type.name());
        }
    }
}
