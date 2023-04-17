package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfSender;

/**
 * unbalanced batched OPRRF factory.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public class UbopprfFactory {
    /**
     * private constructor.
     */
    private UbopprfFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum UbopprfType {
        /**
         * OKVS
         */
        OKVS,
        /**
         * TABLE
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
    public static UbopprfSender createSender(Rpc senderRpc, Party receiverParty, UbopprfConfig config) {
        UbopprfType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsUbopprfSender(senderRpc, receiverParty, (OkvsUbopprfConfig) config);
            case TABLE:
            default:
                throw new IllegalArgumentException("Invalid " + UbopprfType.class.getSimpleName() + ": " + type.name());
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
    public static UbopprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, UbopprfConfig config) {
        UbopprfType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsUbopprfReceiver(receiverRpc, senderParty, (OkvsUbopprfConfig) config);
            case TABLE:
            default:
                throw new IllegalArgumentException("Invalid " + UbopprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static UbopprfConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new OkvsUbopprfConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
