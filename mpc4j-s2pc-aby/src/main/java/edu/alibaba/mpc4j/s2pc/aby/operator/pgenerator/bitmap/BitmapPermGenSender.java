package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.PartyTypes;

public class BitmapPermGenSender extends AbstractBitMapPermGenParty {
    public BitmapPermGenSender(Rpc rpc, Party otherParty, BitmapPermGenConfig config) {
        super(BitmapPermGenPtoDesc.getInstance(), rpc, otherParty, config, PartyTypes.SENDER);
    }
}
