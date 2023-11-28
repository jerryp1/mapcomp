package edu.alibaba.mpc4j.s2pc.aby.operator.group.share;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.amos22.Amos22ShareGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.amos22.Amos22ShareGroupReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.amos22.Amos22ShareGroupSender;

public class ShareGroupFactory extends GroupFactory {
    public enum ShareGroupType {
        /**
         * AMOS22
         */
        AMOS22_SHARE,
    }
    public static ShareGroupParty createSender(Rpc senderRpc, Party receiverParty, ShareGroupConfig config){
        ShareGroupType type = config.getPtoType();
        switch (type){
            case AMOS22_SHARE:
                return new Amos22ShareGroupSender(senderRpc, receiverParty, (Amos22ShareGroupConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ShareGroupType.class.getSimpleName() + ": " + type.name());
        }
    }

    public static ShareGroupParty createReceiver(Rpc receiverRpc, Party senderParty, ShareGroupConfig config){
        ShareGroupType type = config.getPtoType();
        switch (type){
            case AMOS22_SHARE:
                return new Amos22ShareGroupReceiver(receiverRpc, senderParty, (Amos22ShareGroupConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ShareGroupType.class.getSimpleName() + ": " + type.name());
        }
    }
}
