package edu.alibaba.mpc4j.s2pc.aby.millionaire;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2.CheetahMillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2.CheetahMillionaireReceiver;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2.CheetahMillionaireSender;

/**
 * Millionaire Protocol Factory.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class MillionaireFactory {
    /**
     * Private constructor.
     */
    private MillionaireFactory() {
        // empty
    }

    /**
     * Protocol enums.
     */
    public enum MillionaireType {
        /**
         * Cheetah
         */
        CHEETAH,
    }

    /**
     * Build Sender.
     *
     * @param senderRpc     sender rpc.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return sender.
     */
    public static MillionaireParty createSender(Rpc senderRpc, Party receiverParty, MillionaireConfig config) {
        MillionaireType type = config.getPtoType();
        switch (type) {
            case CHEETAH:
                return new CheetahMillionaireSender(senderRpc, receiverParty, (CheetahMillionaireConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + MillionaireFactory.MillionaireType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Build Receiver.
     *
     * @param receiverRpc receiver rpc.
     * @param senderParty sender party.
     * @param config      config.
     * @return receiver.
     */
    public static MillionaireParty createReceiver(Rpc receiverRpc, Party senderParty, MillionaireConfig config) {
        MillionaireType type = config.getPtoType();
        switch (type) {
            case CHEETAH:
                return new CheetahMillionaireReceiver(receiverRpc, senderParty, (CheetahMillionaireConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + MillionaireFactory.MillionaireType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static MillionaireConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new CheetahMillionaireConfig.Builder(securityModel, silent)
                        .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}