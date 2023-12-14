package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bitmap.BitmapGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bitmap.BitmapGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bitmap.BitmapGroupAggSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bsorting.BitmapSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bsorting.BitmapSortingGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bsorting.BitmapSortingGroupAggSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.mix.MixGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.mix.MixGroupAggSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.omix.OptimizedMixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.omix.OptimizedMixGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.omix.OptimizedMixGroupAggSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside.OneSideGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside.OnesideGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside.OnesideGroupAggSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.osorting.OptimizedSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.osorting.OptimizedSortingGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.osorting.OptimizedSortingGroupAggSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.sorting.SortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.sorting.SortingGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.sorting.SortingGroupAggSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.tsorting.TrivialSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.tsorting.TrivialSortingGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.tsorting.TrivialSortingGroupAggSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;

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
        O_MIX,
        SORTING,
        O_SORTING,
        T_SORTING,
        B_SORTING,
        ONE_SIDE,
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
            case BITMAP:
                return new BitmapGroupAggSender(senderRpc, receiverParty, (BitmapGroupAggConfig) config);
            case MIX:
                return new MixGroupAggSender(senderRpc, receiverParty, (MixGroupAggConfig) config);
            case O_MIX:
                return new OptimizedMixGroupAggSender(senderRpc, receiverParty, (OptimizedMixGroupAggConfig) config);
            case SORTING:
                return new SortingGroupAggSender(senderRpc, receiverParty, (SortingGroupAggConfig) config);
            case O_SORTING:
                return new OptimizedSortingGroupAggSender(senderRpc, receiverParty, (OptimizedSortingGroupAggConfig) config);
            case T_SORTING:
                return new TrivialSortingGroupAggSender(senderRpc, receiverParty, (TrivialSortingGroupAggConfig) config);
            case B_SORTING:
                return new BitmapSortingGroupAggSender(senderRpc, receiverParty, (BitmapSortingGroupAggConfig) config);
            case ONE_SIDE:
                return new OnesideGroupAggSender(senderRpc, receiverParty, (OneSideGroupAggConfig) config);
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
            case BITMAP:
                return new BitmapGroupAggReceiver(receiverRpc, senderParty, (BitmapGroupAggConfig) config);
            case MIX:
                return new MixGroupAggReceiver(receiverRpc, senderParty, (MixGroupAggConfig) config);
            case O_MIX:
                return new OptimizedMixGroupAggReceiver(receiverRpc, senderParty, (OptimizedMixGroupAggConfig) config);
            case SORTING:
                return new SortingGroupAggReceiver(receiverRpc, senderParty, (SortingGroupAggConfig) config);
            case O_SORTING:
                return new OptimizedSortingGroupAggReceiver(receiverRpc, senderParty, (OptimizedSortingGroupAggConfig) config);
            case T_SORTING:
                return new TrivialSortingGroupAggReceiver(receiverRpc, senderParty, (TrivialSortingGroupAggConfig) config);
            case B_SORTING:
                return new BitmapSortingGroupAggReceiver(receiverRpc, senderParty, (BitmapSortingGroupAggConfig) config);
            case ONE_SIDE:
                return new OnesideGroupAggReceiver(receiverRpc, senderParty, (OneSideGroupAggConfig) config);
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
