package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.OneSideGroupPartyTypes;

public class Amos22OneSideGroupSender extends AbstractAmos22OneSideGroupParty {
    public Amos22OneSideGroupSender(Rpc rpc, Party otherParty, Amos22OneSideGroupConfig config) {
        super(Amos22OneSideGroupPtoDesc.getInstance(), rpc, otherParty, config, OneSideGroupPartyTypes.SENDER);
    }
}
