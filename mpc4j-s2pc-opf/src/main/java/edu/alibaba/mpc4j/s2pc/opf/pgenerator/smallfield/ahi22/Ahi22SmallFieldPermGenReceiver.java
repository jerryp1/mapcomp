package edu.alibaba.mpc4j.s2pc.opf.pgenerator.smallfield.ahi22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.PermGenFactory.PartyTypes;

/**
 * Ahi22 Permutable Sorter Receiver.
 *
 */
public class Ahi22SmallFieldPermGenReceiver extends AbstractAhi22SmallFieldPermGenParty {
    public Ahi22SmallFieldPermGenReceiver(Rpc rpc, Party otherParty, Ahi22SmallFieldPermGenConfig config) {
        super(Ahi22SmallFieldPermGenPtoDesc.getInstance(), rpc, otherParty, config, PartyTypes.RECEIVER);
    }
}
