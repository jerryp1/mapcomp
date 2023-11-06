package edu.alibaba.mpc4j.s2pc.opf.groupagg;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23.Xxx23ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23.Xxx23ShuffleReceiver;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23.Xxx23ShuffleSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23b.Xxx23bShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23b.Xxx23bShuffleReceiver;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23b.Xxx23bShuffleSender;

/**
 * Group aggregation factory.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class GroupAggFactory {
    /**
     * Private constructor.
     */
    private GroupAggFactory() {
        // empty
    }

    /**
     * Group aggregation type enums.
     */
    public enum GroupAggTypes {
        BITMAP,
        MIX,
        SORTING,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static GroupAggParty createSender(Rpc senderRpc, Party receiverParty, GroupAggConfig config) {
        GroupAggTypes type = config.getPtoType();
        switch (type) {
            case MIX:
                return new MixGroupAggSender(senderRpc, receiverParty, (MixGroupAggConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + GroupAggTypes.class.getSimpleName() + ": " + type.name());
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
    public static GroupAggParty createReceiver(Rpc receiverRpc, Party senderParty, GroupAggConfig config) {
        GroupAggTypes type = config.getPtoType();
        switch (type) {
            case MIX:
                return new MixGroupAggReceiver(receiverRpc, senderParty, (MixGroupAggConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + GroupAggTypes.class.getSimpleName() + ": " + type.name());
        }
    }


    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static GroupAggConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new MixGroupAggConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
