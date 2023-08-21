package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;

/**
 * @author Li Peng
 * @date 2023/8/16
 */
public class SecureBitmapReceiver extends AbstractSecureBitmapParty{
    /**
     * @param ownRpc
     * @param otherParty
     * @param secureBitmapConfig
     */
    public SecureBitmapReceiver(Rpc ownRpc, Party otherParty, SecureBitmapConfig secureBitmapConfig) {
        super(ownRpc, otherParty, secureBitmapConfig);
        this.z2cParty = Z2cFactory.createReceiver(ownRpc, otherParty, secureBitmapConfig.getZ2cConfig());
    }

    @Override
    Bitmap or(PlainBitmap x, SecureBitmap y) {

        return null;
    }
}
