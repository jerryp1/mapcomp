package edu.alibaba.mpc4j.s2pc.aby.operator.group.share.amos22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupFactory.GroupPartyTypes;

public class Amos22ShareGroupSender extends AbstractAmos22ShareGroupParty {
    public Amos22ShareGroupSender(Rpc rpc, Party otherParty, Amos22ShareGroupConfig config) {
        super(Amos22ShareGroupPtoDesc.getInstance(), rpc, otherParty, config, GroupPartyTypes.SENDER);
    }
}
