package edu.alibaba.mpc4j.s2pc.pjc.bitmap.liu22;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.AbstractBitmapParty;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapFactory.BitmapType;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer;
import org.roaringbitmap.RoaringBitmap;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.*;

/**
 * Liu22-Bitmap协议服务端。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public class Liu22BitmapSender extends AbstractBitmapParty {
    /**
     * Bc协议服务端
     */
    private final BcParty bcSender;

    public Liu22BitmapSender(Rpc senderRpc, Party receiverParty, Liu22BitmapConfig config) {
        super(Liu22BitmapPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bcSender = BcFactory.createSender(senderRpc, receiverParty, config.getBcConfig());
        bcSender.addLogLevel();
    }

    @Override
    public SecureBitmapContainer and(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        BcSquareVector vector = bcSender.and(x.getVectors(), y.getVectors());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer xor(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        BcSquareVector vector = bcSender.xor(x.getVectors(), y.getVectors());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer or(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException {
        BcSquareVector vector = bcSender.or(x.getVectors(), y.getVectors());
        return new SecureBitmapContainer(vector);
    }

    @Override
    public SecureBitmapContainer not(SecureBitmapContainer x) throws MpcAbortException {
        BcSquareVector vector = bcSender.not(x.getVectors());
        return new SecureBitmapContainer(vector);
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
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public SecureBitmapContainer setOwnRoaringBitmap(RoaringBitmap roaringBitmap) {
        byte[] shares = BitmapUtils.roaringBitmapToBytes(roaringBitmap);
        BcSquareVector BcSquareVector = bcSender.setOwnInputs(shares, BIT_LENGTH);
        return new SecureBitmapContainer(BcSquareVector);
    }

    @Override
    public SecureBitmapContainer setOtherRoaringBitmap() {
        BcSquareVector BcSquareVector = bcSender.setOtherInputs(BYTE_LENGTH, BIT_LENGTH);
        return new SecureBitmapContainer(BcSquareVector);
    }

    @Override
    public long[][] revealOwn(SecureBitmapContainer secureBitmapContainer) {
        Preconditions.checkNotNull(secureBitmapContainer);
        byte[] outputs = bcSender.getOwnOutputs(secureBitmapContainer.getVectors());
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
        bcSender.getOtherOutputs(secureBitmapContainer.getVectors());
    }
}
