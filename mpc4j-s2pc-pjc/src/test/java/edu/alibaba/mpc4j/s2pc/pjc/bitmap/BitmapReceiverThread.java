package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.roaringbitmap.RoaringBitmap;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.BIT_LENGTH;

/**
 * Bitmap发送方线程
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
class BitmapReceiverThread extends Thread {
    /**
     * bitmap接收方
     */
    private final BitmapParty receiver;
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

    BitmapReceiverThread(BitmapParty receiver, RoaringBitmap x, boolean xPublic, RoaringBitmap y, boolean yPublic) {
        // 当x非public时应为null
        assert (x == null) == !xPublic;
        this.receiver = receiver;
        this.x = x;
        this.xPublic = xPublic;
        this.y = y;
        this.yPublic = yPublic;
    }

    RoaringBitmap getOutput() {
        return z;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(BIT_LENGTH, BIT_LENGTH);
            SecureBitmapContainer x0 = xPublic ? receiver.setPublicRoaringBitmap(x) : receiver.setOtherRoaringBitmap();
            SecureBitmapContainer y0 = yPublic ? receiver.setPublicRoaringBitmap(y) : receiver.setOwnRoaringBitmap(y);
            SecureBitmapContainer z0 = receiver.and(x0, y0);
            z = receiver.toOwnRoaringBitmap(z0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
