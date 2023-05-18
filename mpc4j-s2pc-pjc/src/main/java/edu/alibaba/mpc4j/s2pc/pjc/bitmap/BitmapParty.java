package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import org.roaringbitmap.BitmapContainer;
import org.roaringbitmap.Container;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;

/**
 * Bitmap参与方类
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public interface BitmapParty extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxRoundNum 最大单轮数量。
     * @param updateNum   更新数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxRoundNum, int updateNum) throws MpcAbortException;

    /**
     * 执行AND运算，得到zi，满足z0 ⊕ z1 = z = x & y = (x0 ⊕ x1) & (y0 ⊕ y1)。
     *
     * @param x x。
     * @param y y。
     * @return zi。
     * @throws MpcAbortException 如果协议异常中止。
     */
    SecureBitmapContainer and(SecureBitmapContainer x, SecureBitmapContainer y) throws MpcAbortException;


    /**
     * 执行XOR运算，得到zi，满足z0 ⊕ z1 = z = x ^ y = (x0 ⊕ x1) ^ (y0 ⊕ y1)。
     *
     * @param xi xi。
     * @param yi yi。
     * @return zi。
     */
    SecureBitmapContainer xor(SecureBitmapContainer xi, SecureBitmapContainer yi) throws MpcAbortException;

    /**
     * 执行NOT运算，得到zi，满足z0 ⊕ z1 = z = !x = (x0 ⊕ x1)。
     *
     * @param xi xi。
     * @return zi。
     * @throws MpcAbortException 如果协议异常中止。
     */
    SecureBitmapContainer not(SecureBitmapContainer xi) throws MpcAbortException;

    /**
     * 执行OR运算，得到zi，满足z0 ⊕ z1 = z = x | y = (x0 ⊕ x1) | (y0 ⊕ y1)。
     *
     * @param xi xi。
     * @param yi yi。
     * @return zi。
     * @throws MpcAbortException 如果协议异常中止。
     */
    SecureBitmapContainer or(SecureBitmapContainer xi, SecureBitmapContainer yi) throws MpcAbortException;

    /**
     * 执行count运算，获得当前secureBitmapContainer中的元素个数。
     *
     * @param x x。
     * @return 元素个数。
     * @throws MpcAbortException 如果协议异常中止。
     */
    int count(SecureBitmapContainer x) throws MpcAbortException;

    /**
     * RoaringBitmap -> SecureContainer
     *
     * @param roaringBitmap roaringBitmap
     * @param maxNum        max num of elements
     * @return SecureBitmapContainer
     */
    SecureBitmapContainer setOwnRoaringBitmap(RoaringBitmap roaringBitmap, int maxNum);

    /**
     * RoaringBitmap -> SecureContainer
     *
     * @param maxNum max num of elements
     * @return SecureBitmapContainer
     */
    SecureBitmapContainer setOtherRoaringBitmap(int maxNum) throws MpcAbortException;

    default SecureBitmapContainer setPublicRoaringBitmap(RoaringBitmap roaringBitmap, int maxNum) {
        byte[] bytes = BitmapUtils.roaringBitmapToBytes(roaringBitmap, maxNum);
        SquareZ2Vector vector = SquareZ2Vector.create(maxNum, bytes, true);
        return new SecureBitmapContainer(vector);
    }

    /**
     * SecureContainer -> RoaringBitmap
     *
     * @return roaringBitmap
     */
    default RoaringBitmap toOwnRoaringBitmap(SecureBitmapContainer secureBitmapContainer) throws MpcAbortException {
        // 先reveal所有元素
        long[][] bitmaps = revealOwn(secureBitmapContainer);
        RoaringBitmap roaringBitmap = new RoaringBitmap();
        // 获得各container内元素数量
        int[] cardinalities = Arrays.stream(bitmaps).mapToInt(bitmap ->
                Arrays.stream(bitmap).mapToInt(Long::bitCount).sum())
                .toArray();
        for (int i = 0; i < bitmaps.length; i++) {
            if (cardinalities[i] == 0) {
                continue;
            }
            Container c = new BitmapContainer(bitmaps[i], cardinalities[i]);
            // 将bitmap做转换，容量较小的转换为ArrayContainer
            c.repairAfterLazy();
            roaringBitmap.append((char) i, c);
        }
        return roaringBitmap;
    }

    /**
     * SecureContainer -> RoaringBitmap
     *
     * @param secureBitmapContainer secureBitmapContainer
     */
    default void toOtherRoaringBitmap(SecureBitmapContainer secureBitmapContainer) {
        // reveal所有元素
        revealOther(secureBitmapContainer);
    }

    /**
     * 获得自身数据明文
     *
     * @param secureBitmapContainer secureBitmapContainer
     * @return 以long[][]表示的数据明文
     */
    long[][] revealOwn(SecureBitmapContainer secureBitmapContainer) throws MpcAbortException;

    /**
     * 辅助获得对方数据明文
     *
     * @param secureBitmapContainer secureBitmapContainer
     */
    void revealOther(SecureBitmapContainer secureBitmapContainer);
}
