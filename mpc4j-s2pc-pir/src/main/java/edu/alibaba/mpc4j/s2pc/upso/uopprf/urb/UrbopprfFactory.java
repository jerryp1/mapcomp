package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22.Cgs22UrbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22.Cgs22UrbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22.Cgs22UrbopprfSender;

/**
 * unbalanced related-Batch OPRRF factory.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public class UrbopprfFactory {
    /**
     * private constructor.
     */
    private UrbopprfFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum UrbopprfType {
        /**
         * CGS22, hash num = 3
         */
        CGS22,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static UrbopprfSender createSender(Rpc senderRpc, Party receiverParty, UrbopprfConfig config) {
        UrbopprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CGS22:
                return new Cgs22UrbopprfSender(senderRpc, receiverParty, (Cgs22UrbopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UrbopprfType.class.getSimpleName() + ": " + type.name());
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
    public static UrbopprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, UrbopprfConfig config) {
        UrbopprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CGS22:
                return new Cgs22UrbopprfReceiver(receiverRpc, senderParty, (Cgs22UrbopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UrbopprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static UrbopprfConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Cgs22UrbopprfConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
