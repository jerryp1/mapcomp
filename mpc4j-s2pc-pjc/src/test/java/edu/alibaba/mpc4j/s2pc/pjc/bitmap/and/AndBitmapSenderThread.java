package edu.alibaba.mpc4j.s2pc.pjc.bitmap.and;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapParty;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer;
import org.junit.Assert;
import org.roaringbitmap.RoaringBitmap;


/**
 * Bitmap发送方线程
 *
 * @author Li Peng   
 * @date 2022/11/24
 */
class AndBitmapSenderThread extends Thread {
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
    /**
     * x是否为public
     */
    private final boolean xPublic;
    /**
     * y是否为public
     */
    private final boolean yPublic;
    /**
     * 最大元素数量
     */
    private final int maxNum;
    /**
     * count
     */
    int count;

    AndBitmapSenderThread(BitmapParty sender, RoaringBitmap x, boolean xPublic, RoaringBitmap y, boolean yPublic, int maxNum) {
        // 当y非public时应为null
        Assert.assertEquals((y == null), !yPublic);
        this.sender = sender;
        this.x = x;
        this.xPublic = xPublic;
        this.y = y;
        this.yPublic = yPublic;
        this.maxNum = maxNum;
    }

    @Override
    public void run() {
        try {
            sender.init(maxNum, maxNum);
            SecureBitmapContainer x0 = xPublic ? sender.setPublicRoaringBitmap(x, maxNum) : sender.setOwnRoaringBitmap(x, maxNum);
            SecureBitmapContainer y0 = yPublic ? sender.setPublicRoaringBitmap(y, maxNum) : sender.setOtherRoaringBitmap(maxNum);
            SecureBitmapContainer z0 = sender.and(x0, y0);
            sender.toOtherRoaringBitmap(z0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
