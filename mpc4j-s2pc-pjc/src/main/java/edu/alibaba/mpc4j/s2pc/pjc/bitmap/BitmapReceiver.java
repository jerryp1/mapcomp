package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingParty;
import org.roaringbitmap.RoaringBitmap;

import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapPtoDesc.getInstance;
import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.CONTAINER_BYTE_SIZE;

/**
 * Liu22-Bitmap协议客户端。
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public class BitmapReceiver extends AbstractBitmapParty {
    /**
     * Bc协议接收端
     */
    private final Z2cParty bcReceiver;
    /**
     * 汉明距离接收端
     */
    private final HammingParty hammingReceiver;

    public BitmapReceiver(Rpc receiverRpc, Party senderParty, BitmapConfig bitmapConfig) {
        super(getInstance(), receiverRpc, senderParty, bitmapConfig);
        bcReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, bitmapConfig.getBcConfig());
        addSubPtos(bcReceiver);
        hammingReceiver = HammingFactory.createReceiver(receiverRpc, senderParty, bitmapConfig.getHammingConfig());
        addSubPtos(hammingReceiver);
    }

    @Override
    public SecureBitmapContainer and(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        assert x.getCapacity() == y.getCapacity();
        SquareZ2Vector vector = bcReceiver.and(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer xor(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        assert x.getCapacity() == y.getCapacity();
        SquareZ2Vector vector = bcReceiver.xor(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer or(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        assert x.getCapacity() == y.getCapacity();
        SquareZ2Vector vector = bcReceiver.or(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer not(SecureBitmapContainer x) throws MpcAbortException {
        SquareZ2Vector vector = bcReceiver.not(x.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public int count(SecureBitmapContainer x) throws MpcAbortException {
        if (x.isPublic()) {
            return BytesUtils.bitCount(x.getVector().getBitVector().getBytes());
        }
        int count = hammingReceiver.receiveHammingDistance(x.getVector());
        hammingReceiver.sendHammingDistance(x.getVector());
        return count;
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bcReceiver.init(maxRoundNum, updateNum);
        hammingReceiver.init(updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SecureBitmapContainer setOwnRoaringBitmap(RoaringBitmap roaringBitmap, int maxNum) {
        byte[] y = BitmapUtils.roaringBitmapToBytes(roaringBitmap, maxNum);
        BitVector yBitVector = BitVectorFactory.create(maxNum, y);
        SquareZ2Vector y1 = bcReceiver.shareOwn(yBitVector);
        return new SecureBitmapContainer(y1);
    }

    @Override
    public SecureBitmapContainer setOtherRoaringBitmap(int maxNum) throws MpcAbortException {
        SquareZ2Vector x1 = bcReceiver.shareOther(maxNum);
        return new SecureBitmapContainer(x1);
    }

    @Override
    public long[][] revealOwn(SecureBitmapContainer secureBitmapContainer) throws MpcAbortException {
        Preconditions.checkNotNull(secureBitmapContainer);
        byte[] outputs = bcReceiver.revealOwn(secureBitmapContainer.getVector()).getBytes();
        int containerNum = secureBitmapContainer.getContainerNum();
        byte[] containerOutputs = new byte[containerNum * CONTAINER_BYTE_SIZE];
        System.arraycopy(outputs, 0, containerOutputs, 0, outputs.length);
        return IntStream.range(0, containerNum).mapToObj(i -> {
            byte[] containerBytes = new byte[CONTAINER_BYTE_SIZE];
            System.arraycopy(containerOutputs, i * CONTAINER_BYTE_SIZE, containerBytes, 0, CONTAINER_BYTE_SIZE);
            return LongUtils.byteArrayToLongArray(containerBytes, ByteOrder.LITTLE_ENDIAN);
        }).toArray(long[][]::new);
    }

    @Override
    public void revealOther(SecureBitmapContainer secureBitmapContainer) {
        Preconditions.checkNotNull(secureBitmapContainer);
        bcReceiver.revealOther(secureBitmapContainer.getVector());
    }
}
