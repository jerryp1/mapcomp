package edu.alibaba.mpc4j.s2pc.pjc.bitmap.liu22;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingParty;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.AbstractBitmapParty;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapFactory.BitmapType;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer;
import org.roaringbitmap.RoaringBitmap;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.CONTAINER_BYTE_SIZE;

/**
 * Liu22-Bitmap协议服务端。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public class Liu22BitmapSender extends AbstractBitmapParty {
    /**
     * Bc协议发送端
     */
    private final BcParty bcSender;
    /**
     * 汉明距离计算发送端
     */
    private final HammingParty hammingSender;

    public Liu22BitmapSender(Rpc senderRpc, Party receiverParty, BitmapConfig bitmapConfig) {
        super(Liu22BitmapPtoDesc.getInstance(), senderRpc, receiverParty, bitmapConfig);
        bcSender = BcFactory.createSender(senderRpc, receiverParty, bitmapConfig.getBcConfig());
        bcSender.addLogLevel();
        hammingSender = HammingFactory.createSender(senderRpc, receiverParty, bitmapConfig.getHammingConfig());
        hammingSender.addLogLevel();
    }

    @Override
    public SecureBitmapContainer and(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        BcSquareVector vector = bcSender.and(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer xor(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        BcSquareVector vector = bcSender.xor(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer or(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        BcSquareVector vector = bcSender.or(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer not(SecureBitmapContainer x) throws MpcAbortException {
        BcSquareVector vector = bcSender.not(x.getVector());
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
        return BitmapType.LIU22;
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
        int maxBitLength = BitmapUtils.getBitLength(maxNum);
        byte[] shares = BitmapUtils.roaringBitmapToBytes(roaringBitmap, maxBitLength);
        BcSquareVector BcSquareVector = bcSender.setOwnInputs(shares, maxBitLength);
        return new SecureBitmapContainer(BcSquareVector);
    }

    @Override
    public SecureBitmapContainer setOtherRoaringBitmap(int maxNum) {
        int maxBitLength = BitmapUtils.getBitLength(maxNum);
        BcSquareVector BcSquareVector = bcSender.setOtherInputs(maxBitLength);
        return new SecureBitmapContainer(BcSquareVector);
    }

    @Override
    public long[][] revealOwn(SecureBitmapContainer secureBitmapContainer) {
        Preconditions.checkNotNull(secureBitmapContainer);
        byte[] outputs = bcSender.getOwnOutputs(secureBitmapContainer.getVector());
        int containerNum = secureBitmapContainer.getContainerNum();
        return IntStream.range(0, containerNum).mapToObj(i -> {
            byte[] containerBytes = new byte[CONTAINER_BYTE_SIZE];
            System.arraycopy(outputs, i * CONTAINER_BYTE_SIZE, containerBytes, 0, CONTAINER_BYTE_SIZE);
            return LongUtils.byteArrayToLongArrayLE(containerBytes);
        }).toArray(long[][]::new);
    }

    @Override
    public void revealOther(SecureBitmapContainer secureBitmapContainer) {
        Preconditions.checkNotNull(secureBitmapContainer);
        bcSender.getOtherOutputs(secureBitmapContainer.getVector());
    }
}
