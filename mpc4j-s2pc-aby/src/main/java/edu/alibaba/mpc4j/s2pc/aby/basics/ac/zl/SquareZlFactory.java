package edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * square Zl factory.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class SquareZlFactory implements PtoFactory {
    /**
     * private constructor
     */
    private SquareZlFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum SquareZlType {
        /**
         * Bea91
         */
        Bea91,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static SquareZlParty createSender(Rpc senderRpc, Party receiverParty, SquareZlConfig config) {
        SquareZlType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea91:
            default:
                throw new IllegalArgumentException("Invalid " + SquareZlType.class.getSimpleName() + ": " + type.name());
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
    public static SquareZlParty createReceiver(Rpc receiverRpc, Party senderParty, SquareZlConfig config) {
        SquareZlType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea91:
            default:
                throw new IllegalArgumentException("Invalid " + SquareZlType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static SquareZlConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
