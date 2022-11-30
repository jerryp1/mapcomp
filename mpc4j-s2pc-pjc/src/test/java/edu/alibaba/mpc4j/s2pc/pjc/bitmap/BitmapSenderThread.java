package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.junit.Assert;
import org.roaringbitmap.RoaringBitmap;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.BIT_LENGTH;

/**
 * Bitmap发送方线程
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
class BitmapSenderThread extends Thread {
    /**
     * bitmap发送方
     */
    private final BitmapParty sender;
    /**
     * 自己数据
     */
    private final RoaringBitmap x;
    /**
     * 对方的public数据
     */
    private final RoaringBitmap y;
//    /**
//     * 结果数据
//     */
//    private RoaringBitmap z;
    /**
     * x是否为public
     */
    private final boolean xPublic;
    /**
     * y是否为public
     */
    private final boolean yPublic;

    BitmapSenderThread(BitmapParty sender, RoaringBitmap x, boolean xPublic, RoaringBitmap y, boolean yPublic) {
        // 当y非public时应为null
        Assert.assertEquals((y == null), !yPublic);
        this.sender = sender;
        this.x = x;
        this.xPublic = xPublic;
        this.y = y;
        this.yPublic = yPublic;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(BIT_LENGTH, BIT_LENGTH);
            SecureBitmapContainer x0 = xPublic ? sender.setPublicRoaringBitmap(x) : sender.setOwnRoaringBitmap(x);
            SecureBitmapContainer y0 = yPublic ? sender.setPublicRoaringBitmap(y) : sender.setOtherRoaringBitmap();
            SecureBitmapContainer z0 = sender.and(x0, y0);
            sender.toOtherRoaringBitmap(z0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
