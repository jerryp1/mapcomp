package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapFactory.SecureBitmapType;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapSecurityMode;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.RoaringBitmapUtils;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Li Peng
 * @date 2023/8/14
 */
public class SecureBitmapSender extends AbstractSecureBitmapParty {
    /**
     * @param ownRpc
     * @param otherParty
     * @param secureBitmapConfig
     */
    public SecureBitmapSender(Rpc ownRpc, Party otherParty, SecureBitmapConfig secureBitmapConfig) {
        super(ownRpc, otherParty, secureBitmapConfig);
        this.z2cParty = Z2cFactory.createSender(ownRpc, otherParty, secureBitmapConfig.getZ2cConfig());
    }

    @Override
    Bitmap or(PlainBitmap x, SecureBitmap y) {
        return null;
    }
}
