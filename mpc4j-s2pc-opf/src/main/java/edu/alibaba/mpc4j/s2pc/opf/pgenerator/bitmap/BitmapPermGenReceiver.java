package edu.alibaba.mpc4j.s2pc.opf.pgenerator.bitmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.PermGenFactory.PartyTypes;

/**
 * Permutable sorter receiver for bitmap
 *
 */
public class BitmapPermGenReceiver extends AbstractBitMapPermGenParty {
    public BitmapPermGenReceiver(Rpc rpc, Party otherParty, BitmapPermGenConfig config) {
        super(BitmapPermGenPtoDesc.getInstance(), rpc, otherParty, config, PartyTypes.RECEIVER);
    }
}
