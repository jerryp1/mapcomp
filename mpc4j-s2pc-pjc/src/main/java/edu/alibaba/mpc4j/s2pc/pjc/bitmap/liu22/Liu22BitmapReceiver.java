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
import org.junit.Assert;
import org.roaringbitmap.RoaringBitmap;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.CONTAINER_BYTE_SIZE;

/**
 * Liu22-Bitmap协议客户端。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public class Liu22BitmapReceiver extends AbstractBitmapParty {
    /**
     * Bc协议接收端
     */
    private final BcParty bcReceiver;
    /**
     * 汉明距离接收端
     */
    private final HammingParty hammingReceiver;

    public Liu22BitmapReceiver(Rpc receiverRpc, Party senderParty, BitmapConfig bitmapConfig) {
        super(Liu22BitmapPtoDesc.getInstance(), receiverRpc, senderParty, bitmapConfig);
        bcReceiver = BcFactory.createReceiver(receiverRpc, senderParty, bitmapConfig.getBcConfig());
        bcReceiver.addLogLevel();
        hammingReceiver = HammingFactory.createReceiver(receiverRpc, senderParty, bitmapConfig.getHammingConfig());
        hammingReceiver.addLogLevel();
    }

    @Override
    public SecureBitmapContainer and(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        Assert.assertEquals(x.getCapacity(), y.getCapacity());
        BcSquareVector vector = bcReceiver.and(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer xor(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        Assert.assertEquals(x.getCapacity(), y.getCapacity());
        BcSquareVector vector = bcReceiver.xor(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer or(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        Assert.assertEquals(x.getCapacity(), y.getCapacity());
        BcSquareVector vector = bcReceiver.or(x.getVector(), y.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer not(SecureBitmapContainer x) throws MpcAbortException {
        BcSquareVector vector = bcReceiver.not(x.getVector());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public int count(SecureBitmapContainer x) throws MpcAbortException {
        if (x.isPublic()) {
            return BytesUtils.bitCount(x.getVector().getBytes());
        }
        int count = hammingReceiver.receiveHammingDistance(x.getVector());
        hammingReceiver.sendHammingDistance(x.getVector());
        return count;
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        bcReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        bcReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        bcReceiver.addLogLevel();
    }

    @Override
    public BitmapType getPtoType() {
        return BitmapType.LIU22;
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        bcReceiver.init(maxRoundNum, updateNum);
        hammingReceiver.init(updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public SecureBitmapContainer setOwnRoaringBitmap(RoaringBitmap roaringBitmap, int maxNum) {
        int maxBitLength = BitmapUtils.getBitLength(maxNum);
        byte[] shares = BitmapUtils.roaringBitmapToBytes(roaringBitmap, maxBitLength);
        BcSquareVector bcSquareVector = bcReceiver.setOwnInputs(shares, maxBitLength);
        return new SecureBitmapContainer(bcSquareVector);
    }

    @Override
    public SecureBitmapContainer setOtherRoaringBitmap(int maxNum) {
        int maxBitLength = BitmapUtils.getBitLength(maxNum);
        BcSquareVector bcSquareVector = bcReceiver.setOtherInputs(maxBitLength);
        return new SecureBitmapContainer(bcSquareVector);
    }

    @Override
    public long[][] revealOwn(SecureBitmapContainer secureBitmapContainer) {
        Preconditions.checkNotNull(secureBitmapContainer);
        byte[] outputs = secureBitmapContainer.getVector().isPublic() ? secureBitmapContainer.getVector().getBytes() :
                bcReceiver.getOwnOutputs(secureBitmapContainer.getVector());
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
        bcReceiver.getOtherOutputs(secureBitmapContainer.getVector());
    }
}
