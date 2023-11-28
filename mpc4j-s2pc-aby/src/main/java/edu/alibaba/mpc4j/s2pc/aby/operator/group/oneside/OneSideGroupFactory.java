package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.Amos22OneSideGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.Amos22OneSideGroupReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.Amos22OneSideGroupSender;

public class OneSideGroupFactory extends GroupFactory {
    /**
     * permutation generator type enums.
     */
    public enum OneSideGroupType {
        /**
         * AMOS22
         */
        AMOS22_ONE_SIDE,
    }

    public static OneSideGroupParty createSender(Rpc senderRpc, Party receiverParty, OneSideGroupConfig config){
        OneSideGroupType type = config.getPtoType();
        switch (type){
            case AMOS22_ONE_SIDE:
                return new Amos22OneSideGroupSender(senderRpc, receiverParty, (Amos22OneSideGroupConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OneSideGroupType.class.getSimpleName() + ": " + type.name());
        }
    }

    public static OneSideGroupParty createReceiver(Rpc receiverRpc, Party senderParty, OneSideGroupConfig config){
        OneSideGroupType type = config.getPtoType();
        switch (type){
            case AMOS22_ONE_SIDE:
                return new Amos22OneSideGroupReceiver(receiverRpc, senderParty, (Amos22OneSideGroupConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OneSideGroupType.class.getSimpleName() + ": " + type.name());
        }
    }

}
