package edu.alibaba.mpc4j.s2pc.opf.groupagg;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggSender;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;

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
            case SORTING:
                return new SortingGroupAggSender(senderRpc, receiverParty, (SortingGroupAggConfig) config);
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
            case SORTING:
                return new SortingGroupAggReceiver(receiverRpc, senderParty, (SortingGroupAggConfig) config);
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
    public static GroupAggConfig createDefaultConfig(SecurityModel securityModel, Zl zl, boolean silent, PrefixAggTypes type) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new MixGroupAggConfig.Builder(zl, silent, type).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
