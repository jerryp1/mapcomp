package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23.Xxx23PermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23.Xxx23PermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23.Xxx23PermutationSender;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23b.Xxx23bPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23b.Xxx23bPermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23b.Xxx23bPermutationSender;

/**
 * Permutation factory.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public class PermutationFactory {
    /**
     * Private constructor.
     */
    private PermutationFactory() {
        // empty
    }

    /**
     * Permutation type enums.
     */
    public enum PermutationTypes {
        /**
         * xxx+23.
         */
        XXX23,
        /**
         * xxx+23b.
         */
        XXX23B,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PermutationSender createSender(Rpc senderRpc, Party receiverParty, PermutationConfig config) {
        PermutationTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case XXX23:
                return new Xxx23PermutationSender(senderRpc, receiverParty, (Xxx23PermutationConfig) config);
            case XXX23B:
                return new Xxx23bPermutationSender(senderRpc, receiverParty, (Xxx23bPermutationConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PermutationTypes.class.getSimpleName() + ": " + type.name());
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
    public static PermutationReceiver createReceiver(Rpc receiverRpc, Party senderParty, PermutationConfig config) {
        PermutationTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case XXX23:
                return new Xxx23PermutationReceiver(receiverRpc, senderParty, (Xxx23PermutationConfig) config);
            case XXX23B:
                return new Xxx23bPermutationReceiver(receiverRpc, senderParty, (Xxx23bPermutationConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PermutationTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}