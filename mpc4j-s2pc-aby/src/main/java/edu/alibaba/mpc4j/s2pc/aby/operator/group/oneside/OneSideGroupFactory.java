package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.Amos22OneSideGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.Amos22OneSideGroupReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.Amos22OneSideGroupSender;

public class OneSideGroupFactory {
    public enum OneSideGroupPartyTypes {
        SENDER,
        RECEIVER
    }
    /**
     * permutation generator type enums.
     */
    public enum OneSideGroupType {
        /**
         * AMOS22
         */
        AMOS22_ONE_SIDE,
    }
    public enum AggTypes{
        MAX,
        MIN,
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


    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static OneSideGroupConfig createDefaultConfig(SecurityModel securityModel,boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Amos22OneSideGroupConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }

}
