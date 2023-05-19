package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20.Egk20ZlDaBitGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20.Egk20ZlDaBitGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20.Egk20ZlDaBitGenSender;

/**
 * Zl daBit generation factory.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public class ZlDaBitGenFactory implements PtoFactory {
    /**
     * private constructor
     */
    private ZlDaBitGenFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum ZlDaBitGenType {
        /**
         * EGK20
         */
        EGK20,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlDaBitGenParty createSender(Rpc senderRpc, Party receiverParty, ZlDaBitGenConfig config) {
        ZlDaBitGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case EGK20:
                return new Egk20ZlDaBitGenSender(senderRpc, receiverParty, (Egk20ZlDaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDaBitGenType.class.getSimpleName() + ": " + type.name());
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
    public static ZlDaBitGenParty createReceiver(Rpc receiverRpc, Party senderParty, ZlDaBitGenConfig config) {
        ZlDaBitGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case EGK20:
                return new Egk20ZlDaBitGenReceiver(receiverRpc, senderParty, (Egk20ZlDaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDaBitGenType.class.getSimpleName() + ": " + type.name());
        }
    }
}
