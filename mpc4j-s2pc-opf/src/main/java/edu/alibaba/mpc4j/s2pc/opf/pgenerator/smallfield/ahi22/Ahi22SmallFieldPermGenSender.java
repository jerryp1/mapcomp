package edu.alibaba.mpc4j.s2pc.opf.pgenerator.smallfield.ahi22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.PermGenFactory.PartyTypes;

/**
 * Ahi22 Permutable Sorter Sender.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public class Ahi22SmallFieldPermGenSender extends AbstractAhi22SmallFieldPermGenParty {
    public Ahi22SmallFieldPermGenSender(Rpc rpc, Party otherParty, Ahi22SmallFieldPermGenConfig config) {
        super(Ahi22SmallFieldPermGenPtoDesc.getInstance(), rpc, otherParty, config, PartyTypes.SENDER);
    }
}
