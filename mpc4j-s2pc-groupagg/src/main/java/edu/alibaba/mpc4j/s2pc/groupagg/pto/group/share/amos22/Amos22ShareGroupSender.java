package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.amos22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes.GroupPartyTypes;

/**
 * Amos22 group aggregation Sender, where the group flag is secret shared.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
public class Amos22ShareGroupSender extends AbstractAmos22ShareGroupParty {
    public Amos22ShareGroupSender(Rpc rpc, Party otherParty, Amos22ShareGroupConfig config) {
        super(Amos22ShareGroupPtoDesc.getInstance(), rpc, otherParty, config, GroupPartyTypes.SENDER);
    }
}
