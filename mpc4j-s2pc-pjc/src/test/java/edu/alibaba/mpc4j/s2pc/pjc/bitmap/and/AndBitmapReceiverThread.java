package edu.alibaba.mpc4j.s2pc.pjc.bitmap.and;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapParty;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer;
import org.roaringbitmap.RoaringBitmap;


/**
 * Bitmap发送方线程
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
class AndBitmapReceiverThread extends Thread {
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
    /**
     * 最大元素数量
     */
    private final int maxNum;

    AndBitmapReceiverThread(BitmapParty receiver, RoaringBitmap x, boolean xPublic, RoaringBitmap y, boolean yPublic, int maxNum) {
        // 当x非public时应为null
        assert (x == null) == !xPublic;
        this.receiver = receiver;
        this.x = x;
        this.xPublic = xPublic;
        this.y = y;
        this.yPublic = yPublic;
        this.maxNum = maxNum;
    }

    RoaringBitmap getOutput() {
        return z;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(BitmapUtils.getBitLength(maxNum), BitmapUtils.getBitLength(maxNum));
            SecureBitmapContainer x0 = xPublic ? receiver.setPublicRoaringBitmap(x, maxNum) : receiver.setOtherRoaringBitmap(maxNum);
            SecureBitmapContainer y0 = yPublic ? receiver.setPublicRoaringBitmap(y, maxNum) : receiver.setOwnRoaringBitmap(y, maxNum);
            SecureBitmapContainer z0 = receiver.and(x0, y0);
            z = receiver.toOwnRoaringBitmap(z0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
