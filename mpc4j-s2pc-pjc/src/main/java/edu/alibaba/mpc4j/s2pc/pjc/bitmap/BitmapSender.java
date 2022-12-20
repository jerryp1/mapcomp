package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory.BitVectorType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.bc.SquareSbitVector;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingParty;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapPtoDesc.BitmapType;
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
     * Bc协议发送端
     */
    private final BcParty bcSender;
    /**
     * 汉明距离计算发送端
     */
    private final HammingParty hammingSender;

    public BitmapSender(Rpc senderRpc, Party receiverParty, BitmapConfig bitmapConfig) {
        super(BitmapPtoDesc.getInstance(), senderRpc, receiverParty, bitmapConfig);
        bcSender = BcFactory.createSender(senderRpc, receiverParty, bitmapConfig.getBcConfig());
        bcSender.addLogLevel();
        hammingSender = HammingFactory.createSender(senderRpc, receiverParty, bitmapConfig.getHammingConfig());
        hammingSender.addLogLevel();
    }

    @Override
    public SecureBitmapContainer and(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        SquareSbitVector vector = bcSender.and(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer xor(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        SquareSbitVector vector = bcSender.xor(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer or(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        SquareSbitVector vector = bcSender.or(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer not(SecureBitmapContainer x) throws MpcAbortException {
        SquareSbitVector vector = bcSender.not(x.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public int count(SecureBitmapContainer x) throws MpcAbortException {
        if (x.isPublic()) {
            return BytesUtils.bitCount(x.getVector().getBytes());
        }
        hammingSender.sendHammingDistance(x.getVector());
        return hammingSender.receiveHammingDistance(x.getVector());
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        bcSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        bcSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        bcSender.addLogLevel();
    }

    @Override
    public BitmapType getPtoType() {
        return BitmapType.BITMAP;
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        bcSender.init(maxRoundNum, updateNum);
        hammingSender.init(updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public SecureBitmapContainer setOwnRoaringBitmap(RoaringBitmap roaringBitmap, int maxNum) {
        byte[] x = BitmapUtils.roaringBitmapToBytes(roaringBitmap, maxNum);
        BitVector xBitVector = BitVectorFactory.create(BitVectorType.BYTES_BIT_VECTOR, maxNum, x);
        SquareSbitVector x0 = bcSender.shareOwn(xBitVector);
        return new SecureBitmapContainer(x0);
    }

    @Override
    public SecureBitmapContainer setOtherRoaringBitmap(int maxNum) throws MpcAbortException {
        SquareSbitVector y0 = bcSender.shareOther(maxNum);
        return new SecureBitmapContainer(y0);
    }

    @Override
    public long[][] revealOwn(SecureBitmapContainer secureBitmapContainer) throws MpcAbortException {
        Preconditions.checkNotNull(secureBitmapContainer);
        byte[] outputs = bcSender.revealOwn(secureBitmapContainer.getVector()).getBytes();
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
        bcSender.revealOther(secureBitmapContainer.getVector());
    }
}
