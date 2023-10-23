package edu.alibaba.mpc4j.s2pc.aby.basics.a2b;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.dsz15.Dsz15A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.dsz15.Dsz15A2bReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.dsz15.Dsz15A2bSender;

/**
 * A2b Factory.
 *
 * @author Li Peng
 * @date 2023/10/20
 */
public class A2bFactory {
    /**
     * Private constructor.
     */
    private A2bFactory() {
        // empty
    }

    /**
     * B2a type enums.
     */
    public enum A2bTypes {
        /**
         * DSZ15.
         */
        DSZ15,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static A2bParty createSender(Rpc senderRpc, Party receiverParty, A2bConfig config) {
        A2bTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case DSZ15:
                return new Dsz15A2bSender(senderRpc, receiverParty, (Dsz15A2bConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + A2bTypes.class.getSimpleName() + ": " + type.name());
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
    public static A2bParty createReceiver(Rpc receiverRpc, Party senderParty, A2bConfig config) {
        A2bTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case DSZ15:
                return new Dsz15A2bReceiver(receiverRpc, senderParty, (Dsz15A2bConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + A2bTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param zl zl.
     * @return a default config.
     */
    public static A2bConfig createDefaultConfig(SecurityModel securityModel, Zl zl) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new Dsz15A2bConfig.Builder(zl).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
