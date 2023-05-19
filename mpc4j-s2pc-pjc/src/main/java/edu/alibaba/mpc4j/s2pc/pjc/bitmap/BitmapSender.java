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
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingParty;
import org.roaringbitmap.RoaringBitmap;

import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.CONTAINER_BYTE_SIZE;

/**
 * Liu22-Bitmap协议服务端。
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public class BitmapSender extends AbstractBitmapParty {
    /**
     * Z2 circuit sender
     */
    private final Z2cParty z2cSender;
    /**
     * hamming sender
     */
    private final HammingParty hammingSender;

    public BitmapSender(Rpc senderRpc, Party receiverParty, BitmapConfig bitmapConfig) {
        super(BitmapPtoDesc.getInstance(), senderRpc, receiverParty, bitmapConfig);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, bitmapConfig.getZ2cConfig());
        addSubPtos(z2cSender);
        hammingSender = HammingFactory.createSender(senderRpc, receiverParty, bitmapConfig.getHammingConfig());
        addSubPtos(hammingSender);
    }

    @Override
    public SecureBitmapContainer and(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        SquareZ2Vector vector = z2cSender.and(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer xor(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        SquareZ2Vector vector = z2cSender.xor(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer or(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        SquareZ2Vector vector = z2cSender.or(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer not(SecureBitmapContainer x) throws MpcAbortException {
        SquareZ2Vector vector = z2cSender.not(x.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public int count(SecureBitmapContainer x) throws MpcAbortException {
        if (x.isPublic()) {
            return BytesUtils.bitCount(x.getVector().getBitVector().getBytes());
        }
        hammingSender.sendHammingDistance(x.getVector());
        return hammingSender.receiveHammingDistance(x.getVector());
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cSender.init(maxRoundNum, updateNum);
        hammingSender.init(updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SecureBitmapContainer setOwnRoaringBitmap(RoaringBitmap roaringBitmap, int maxNum) {
        byte[] x = BitmapUtils.roaringBitmapToBytes(roaringBitmap, maxNum);
        BitVector xBitVector = BitVectorFactory.create(maxNum, x);
        SquareZ2Vector x0 = z2cSender.shareOwn(xBitVector);
        return new SecureBitmapContainer(x0);
    }

    @Override
    public SecureBitmapContainer setOtherRoaringBitmap(int maxNum) throws MpcAbortException {
        SquareZ2Vector y0 = z2cSender.shareOther(maxNum);
        return new SecureBitmapContainer(y0);
    }

    @Override
    public long[][] revealOwn(SecureBitmapContainer secureBitmapContainer) throws MpcAbortException {
        Preconditions.checkNotNull(secureBitmapContainer);
        byte[] outputs = z2cSender.revealOwn(secureBitmapContainer.getVector()).getBytes();
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
        z2cSender.revealOther(secureBitmapContainer.getVector());
    }
}
