package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23.Xxx23ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23b.Xxx23bSharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23b.Xxx23bSharedPermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23b.Xxx23bSharedPermutationSender;

/**
 * Shared permutation factory.
 *
 * @author Li Peng
 * @date 2023/10/25
 */
public class SharedPermutationFactory {
    /**
     * Private constructor.
     */
    private SharedPermutationFactory() {
        // empty
    }

    /**
     * Shared permutation type enums.
     */
    public enum SharedPermutationTypes {
        /**
         * xxx+23.
         */
        XXX23,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static SharedPermutationParty createSender(Rpc senderRpc, Party receiverParty, SharedPermutationConfig config) {
        SharedPermutationTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case XXX23:
                return new Xxx23bSharedPermutationSender(senderRpc, receiverParty, (Xxx23bSharedPermutationConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SharedPermutationTypes.class.getSimpleName() + ": " + type.name());
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
    public static SharedPermutationParty createReceiver(Rpc receiverRpc, Party senderParty, SharedPermutationConfig config) {
        SharedPermutationTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case XXX23:
                return new Xxx23bSharedPermutationReceiver(receiverRpc, senderParty, (Xxx23bSharedPermutationConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SharedPermutationTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static SharedPermutationConfig createDefaultConfig(SecurityModel securityModel, Zl zl, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Xxx23bSharedPermutationConfig.Builder(zl, silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
