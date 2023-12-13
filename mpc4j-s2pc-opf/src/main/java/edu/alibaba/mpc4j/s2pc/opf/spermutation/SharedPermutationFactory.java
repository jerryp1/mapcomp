package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24.Php24SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24.Php24SharedPermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24.Php24SharedPermutationSender;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24b.Php24bSharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24b.Php24bSharedPermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24b.Php24bSharedPermutationSender;

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
         * Php+24.
         */
        PHP24,
        /**
         * Php+24b.
         */
        PHP24B,
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
        switch (type) {
            case PHP24:
                return new Php24SharedPermutationSender(senderRpc, receiverParty, (Php24SharedPermutationConfig) config);
            case PHP24B:
                return new Php24bSharedPermutationSender(senderRpc, receiverParty, (Php24bSharedPermutationConfig) config);
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
        switch (type) {
            case PHP24:
                return new Php24SharedPermutationReceiver(receiverRpc, senderParty, (Php24SharedPermutationConfig) config);
            case PHP24B:
                return new Php24bSharedPermutationReceiver(receiverRpc, senderParty, (Php24bSharedPermutationConfig) config);
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
    public static SharedPermutationConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24SharedPermutationConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }

    /**
     * Creates a default reverse permutation config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static SharedPermutationConfig createDefaultReverseConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24bSharedPermutationConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
