package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23.Xxx23ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23.Xxx23ShuffleReceiver;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23.Xxx23ShuffleSender;

/**
 * Shuffle factory.
 *
 * @author Li Peng
 * @date 2023/10/11
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
    public static ShuffleParty createSender(Rpc senderRpc, Party receiverParty, ShuffleConfig config) {
        ShuffleTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case XXX23:
                return new Xxx23ShuffleSender(senderRpc, receiverParty, (Xxx23ShuffleConfig) config);
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
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case XXX23:
                return new Xxx23ShuffleReceiver(receiverRpc, senderParty, (Xxx23ShuffleConfig) config);
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
    public static ShuffleConfig createDefaultConfig(SecurityModel securityModel, Zl zl, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Xxx23ShuffleConfig.Builder(zl, silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
