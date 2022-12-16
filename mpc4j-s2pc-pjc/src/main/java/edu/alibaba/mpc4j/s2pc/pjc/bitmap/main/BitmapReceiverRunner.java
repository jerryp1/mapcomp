package edu.alibaba.mpc4j.s2pc.pjc.bitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapReceiver;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Bitmap接收方执行类
 *
 * @author Li Peng  
 * @date 2022/12/6
 */
public class BitmapReceiverRunner extends AbstractBitmapRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapReceiverRunner.class);

    /**
     * Bitmap接收方
     */
    BitmapReceiver receiver;
    /**
     * Bitmap配置项
     */
    SecureBitmapConfig bitmapConfig;
    /**
     * 对方public数据
     */
    private final RoaringBitmap x;
    /**
     * 自己数据
     */
    private final RoaringBitmap y;
    /**
     * 结果数据
     */
    private RoaringBitmap z;
    /**
     * x是否为public
     */
    private final boolean xPublic;
    /**
     * y是否为public
     */
    private final boolean yPublic;
    /**
     * 接收方rpc
     */
    private Rpc receiverRpc;
    /**
     * 最大数量
     */
    private final int maxNum;

    public BitmapReceiverRunner(int totalRound, int maxNum, BitmapReceiver receiver, SecureBitmapConfig bitmapConfig,
                                RoaringBitmap x, boolean xPublic, RoaringBitmap y, boolean yPublic) {
        super(totalRound);
        this.receiver = receiver;
        this.bitmapConfig = bitmapConfig;
        this.x = x;
        this.y = y;
        this.xPublic = xPublic;
        this.yPublic = yPublic;
        this.maxNum = maxNum;
        this.receiverRpc = receiver.getRpc();
    }

    @Override
    public void run() throws MpcAbortException {
        receiverRpc.synchronize();
        receiverRpc.reset();
        reset();
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            receiver.getRpc().connect();
            receiver.init(BitmapUtils.getBitLength(maxNum), BitmapUtils.getBitLength(maxNum));
            SecureBitmapContainer x0 = xPublic ? receiver.setPublicRoaringBitmap(x, maxNum) : receiver.setOtherRoaringBitmap(maxNum);
            SecureBitmapContainer y0 = yPublic ? receiver.setPublicRoaringBitmap(y, maxNum) : receiver.setOwnRoaringBitmap(y, maxNum);
            // 执行and运算
            SecureBitmapContainer z0 = receiver.and(x0, y0);
            z = receiver.toOwnRoaringBitmap(z0);
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info("Round {}: Time = {}ms", round, time);
            totalTime += time;
        }
        totalPacketNum = receiverRpc.getSendDataPacketNum();
        totalPayloadByteLength = receiverRpc.getPayloadByteLength();
        totalSendByteLength = receiverRpc.getSendByteLength();
        receiverRpc.reset();
    }
}
