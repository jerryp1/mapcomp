package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.amos22.Amos22ShareGroupConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.amos22.Amos22ShareGroupReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.amos22.Amos22ShareGroupSender;

/**
 * group aggregation factory
 *
 */
public class ShareGroupFactory extends GroupTypes {
    /**
     * group aggregation type enums.
     */
    public enum ShareGroupType {
        /**
         * AMOS22
         */
        AMOS22_SHARE,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ShareGroupParty createSender(Rpc senderRpc, Party receiverParty, ShareGroupConfig config) {
        ShareGroupType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case AMOS22_SHARE:
                return new Amos22ShareGroupSender(senderRpc, receiverParty, (Amos22ShareGroupConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ShareGroupType.class.getSimpleName() + ": " + type.name());
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
    public static ShareGroupParty createReceiver(Rpc receiverRpc, Party senderParty, ShareGroupConfig config) {
        ShareGroupType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case AMOS22_SHARE:
                return new Amos22ShareGroupReceiver(receiverRpc, senderParty, (Amos22ShareGroupConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ShareGroupType.class.getSimpleName() + ": " + type.name());
        }
    }
}
