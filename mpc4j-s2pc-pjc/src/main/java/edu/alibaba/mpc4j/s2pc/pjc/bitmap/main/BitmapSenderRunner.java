package edu.alibaba.mpc4j.s2pc.pjc.bitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapSender;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Bitmap发送方执行类
 *
 * @author Li Peng  
 * @date 2022/12/6
 */
public class BitmapSenderRunner extends AbstractBitmapRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapSenderRunner.class);

    /**
     * Bitmap发送方
     */
    BitmapSender sender;
    /**
     * Bitmap配置项
     */
    SecureBitmapConfig bitmapConfig;
    /**
     * 自己数据
     */
    private final RoaringBitmap x;
    /**
     * 对方的public数据
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

    public BitmapSenderRunner(int totalRound, int maxNum, BitmapSender sender, SecureBitmapConfig bitmapConfig,
                              RoaringBitmap x, boolean xPublic, RoaringBitmap y, boolean yPublic) {
        super(totalRound);
        this.sender = sender;
        this.bitmapConfig = bitmapConfig;
        this.x = x;
        this.y = y;
        this.xPublic = xPublic;
        this.yPublic = yPublic;
        this.maxNum = maxNum;
        this.receiverRpc = sender.getRpc();
    }

    @Override
    public void run() throws MpcAbortException {
        receiverRpc.synchronize();
        receiverRpc.reset();
        reset();
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            sender.getRpc().connect();
            sender.init(BitmapUtils.getBitLength(maxNum), BitmapUtils.getBitLength(maxNum));
            SecureBitmapContainer x0 = xPublic ? sender.setPublicRoaringBitmap(x, maxNum) : sender.setOtherRoaringBitmap(maxNum);
            SecureBitmapContainer y0 = yPublic ? sender.setPublicRoaringBitmap(y, maxNum) : sender.setOwnRoaringBitmap(y, maxNum);
            // 执行and运算
            SecureBitmapContainer z0 = sender.and(x0, y0);
            z = sender.toOwnRoaringBitmap(z0);
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
