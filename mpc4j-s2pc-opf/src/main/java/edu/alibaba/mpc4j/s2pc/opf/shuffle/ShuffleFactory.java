package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.php24.Php24ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.php24.Php24ShuffleReceiver;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.php24.Php24ShuffleSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.php24b.Php24bShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.php24b.Php24bShuffleReceiver;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.php24b.Php24bShuffleSender;

/**
 * Shuffle factory.
 *
 */
public class ShuffleFactory {
    /**
     * Private constructor.
     */
    private ShuffleFactory() {
        // empty
    }

    /**
     * Shuffle type enums.
     */
    public enum ShuffleTypes {
        /**
         * Php+24 shuffle.
         */
        PHP24,
        /**
         * Php+24b un-shuffle.
         */
        PHP24b,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ShuffleParty createSender(Rpc senderRpc, Party receiverParty, ShuffleConfig config) {
        ShuffleTypes type = config.getPtoType();
        switch (type) {
            case PHP24:
                return new Php24ShuffleSender(senderRpc, receiverParty, (Php24ShuffleConfig) config);
            case PHP24b:
                return new Php24bShuffleSender(senderRpc, receiverParty, (Php24bShuffleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ShuffleTypes.class.getSimpleName() + ": " + type.name());
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
    public static ShuffleParty createReceiver(Rpc receiverRpc, Party senderParty, ShuffleConfig config) {
        ShuffleTypes type = config.getPtoType();
        switch (type) {
            case PHP24:
                return new Php24ShuffleReceiver(receiverRpc, senderParty, (Php24ShuffleConfig) config);
            case PHP24b:
                return new Php24bShuffleReceiver(receiverRpc, senderParty, (Php24bShuffleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ShuffleTypes.class.getSimpleName() + ": " + type.name());
        }
    }


    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static ShuffleConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24ShuffleConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }

    /**
     * Creates a default reverse shuffle (un-shuffle) config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static ShuffleConfig createDefaultUnShuffleConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24bShuffleConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
