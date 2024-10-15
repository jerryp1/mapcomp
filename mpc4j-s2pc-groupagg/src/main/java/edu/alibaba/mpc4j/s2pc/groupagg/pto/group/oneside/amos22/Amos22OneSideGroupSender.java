package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside.amos22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes.GroupPartyTypes;

/**
 * Amos22 group aggregation Sender, where the group flag is plaintext to receiver.
 *
 */
public class Amos22OneSideGroupSender extends AbstractAmos22OneSideGroupParty {
    public Amos22OneSideGroupSender(Rpc rpc, Party otherParty, Amos22OneSideGroupConfig config) {
        super(Amos22OneSideGroupPtoDesc.getInstance(), rpc, otherParty, config, GroupPartyTypes.SENDER);
    }
}
