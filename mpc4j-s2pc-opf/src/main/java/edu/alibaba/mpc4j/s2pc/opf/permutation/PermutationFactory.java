package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.permutation.php24.Php24PermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.php24.Php24PermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.php24.Php24PermutationSender;
import edu.alibaba.mpc4j.s2pc.opf.permutation.php24b.Php24bPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.php24b.Php24bPermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.php24b.Php24bPermutationSender;

/**
 * Permutation factory.
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
         * PHP+24.
         */
        PHP24,
        /**
         * PHP+24b.
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
    public static PermutationSender createSender(Rpc senderRpc, Party receiverParty, PermutationConfig config) {
        PermutationTypes type = config.getPtoType();
        switch (type) {
            case PHP24:
                return new Php24PermutationSender(senderRpc, receiverParty, (Php24PermutationConfig) config);
            case PHP24B:
                return new Php24bPermutationSender(senderRpc, receiverParty, (Php24bPermutationConfig) config);
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
        switch (type) {
            case PHP24:
                return new Php24PermutationReceiver(receiverRpc, senderParty, (Php24PermutationConfig) config);
            case PHP24B:
                return new Php24bPermutationReceiver(receiverRpc, senderParty, (Php24bPermutationConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PermutationTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static PermutationConfig createDefaultConfig(SecurityModel securityModel, Zl zl, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24PermutationConfig.Builder(zl, silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static PermutationConfig createDefaultReverseConfig(SecurityModel securityModel, Zl zl, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24bPermutationConfig.Builder(zl, silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
