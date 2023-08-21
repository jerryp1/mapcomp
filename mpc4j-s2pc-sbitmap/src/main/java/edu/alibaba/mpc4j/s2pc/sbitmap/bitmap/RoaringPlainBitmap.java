package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapFactory.SecureBitmapType;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.Container;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.RoaringBitmapUtils;
import org.roaringbitmap.RoaringBitmap;

/**
 * Plain bitmap
 * @author Li Peng
 * @date 2023/8/11
 */
public class RoaringPlainBitmap implements PlainBitmap {
    /**
     * total number of bits.
     */
    protected int totalBitNum;
    /**
     * total number of containers.
     */
    protected int totalContainerNum;
    /**
     * total number of bytes.
     */
    protected int totalByteNum;
    /**
     * Roaring bitmap.
     */
    private RoaringBitmap bitmap;
    /**
     * Full state.
     */
    private boolean full;

    /**
     * Create a full secure bitmap in plain state.
     *
     * @param totalBitNum total number of bits.
     * @param bitmap    the plain bitmap.
     * @return the created secure bitmap.
     */
    public static RoaringPlainBitmap fromBitmap(int totalBitNum, RoaringBitmap bitmap) {
        RoaringBitmapUtils.checkContainValidBits(totalBitNum, bitmap);
        RoaringPlainBitmap secureBitmap = new RoaringPlainBitmap();
        secureBitmap.setTotalBitNum(totalBitNum);
        secureBitmap.bitmap = bitmap;
        secureBitmap.full = false;

        return secureBitmap;
    }

    /**
     * Create a full secure bitmap in plain state.
     *
     * @param totalBitNum total number of bits.
     * @return the created secure bitmap.
     */
    public static RoaringPlainBitmap fromBitVectors(int totalBitNum, int[] keys, BitVector[] bitVectors) {
        assert keys.length == bitVectors.length : "Length of keys and bitVectors not match";
        // TODO 在调用该方法前，必须要确保bitVectors和keys的长度是标准的。
//        RoaringBitmapUtils.toRoaringBitmap(keys, bitVectors);
//
//        RoaringBitmapUtils.checkContainValidBits(totalBitNum, bitmap);
//        RoaringPlainBitmap secureBitmap = new RoaringPlainBitmap();
//        secureBitmap.setTotalBitNum(totalBitNum);
//        secureBitmap.bitmap = bitmap;
//        secureBitmap.full = false;

//        return secureBitmap;
        return null;
    }

    public RoaringBitmap getBitmap() {
        return bitmap;
    }

    @Override
    public SecureBitmapType getType() {
        return SecureBitmapType.PLAIN;
    }

    @Override
    public int totalBitNum() {
        return bitmap.getSizeInBytes() * Byte.SIZE;
    }

    @Override
    public int totalByteNum() {
        return bitmap.getSizeInBytes();
    }

    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public boolean isFull() {
        return full;
    }

    @Override
    public int getContainerSize() {
        return 1 << 16;
    }

    @Override
    public BitVector[] getContainers() {
        return new BitVector[0];
    }

    @Override
    public Container[] getContainer() {
        return new Container[0];
    }

    public void setTotalBitNum(int totalBitNum) {
        this.totalBitNum = totalBitNum;
    }

    public void setTotalByteNum(int totalByteNum) {
        this.totalByteNum = totalByteNum;
    }

    public void setBitmap(RoaringBitmap bitmap) {
        this.bitmap = bitmap;
    }



    /**
     * Create a (plain) full secure bitmap with all 1's in the given range [rangeStart, rangeEnd).
     * The input range must be valid, that is,
     * <p>
     * <li>rangeEnd should be in range (0, totalBitNum)</li>
     * <li>rangeStart should be in range [0, rangeEnd)</li>
     * </p>
     *
     * @param totalBitNum  total number of bits.
     * @param rangeStart inclusive beginning of range.
     * @param rangeEnd   exclusive ending of range.
     * @return the created secure bitmap.
     */
    public static RoaringPlainBitmap ofRange(int totalBitNum, int rangeStart, int rangeEnd) {
        // check if range is valid
        MathPreconditions.checkPositiveInRange("rangeEnd", rangeEnd, totalBitNum);
        MathPreconditions.checkNonNegativeInRange("rangeStart", rangeStart, rangeEnd);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(rangeStart, rangeEnd);

        return fromBitmap(totalBitNum, roaringBitmap);
    }

    /**
     * Create a (plain) full secure bitmap with all 1's in bit positions.
     *
     * @param totalBitNum total number of bits.
     * @return  the created secure bitmap.
     */
    public static RoaringPlainBitmap ones(int totalBitNum) {
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(0, totalBitNum);

        return fromBitmap(totalBitNum, roaringBitmap);
    }

    /**
     * Transfer to full bitmap.
     * @return full bitmap.
     */
    public RoaringPlainBitmap toFull() {
        RoaringPlainBitmap plainBitmap = RoaringPlainBitmap.fromBitmap(totalBitNum, RoaringBitmapUtils.toFullRoaringBitmap(bitmap));
        plainBitmap.setFull(true);
        return plainBitmap;
    }


    public MutablePlainBitmap toDpRandom(double epsilon) {
        return BitmapUtils.toWindowPlainBitmap(this, epsilon);
    }

    /**
     * Transfer to full bitmap.
     * @return full bitmap.
     */
    public void setFull(boolean full) {
        this.full = full;
    }

    /**
     * Get keys of containers.
     * @return keys of containers.
     */
    @Override
    public int[] getKeys() {
        return RoaringBitmapUtils.getKeyIntArray(bitmap);
    }

    public int getTotalBitNum() {
        return totalBitNum;
    }

    @Override
    public PlainBitmap and(PlainBitmap other) throws MpcAbortException {
        assert other!=null : "bitmap must not be null.";
        if (other instanceof RoaringPlainBitmap) {
            // direct AND bitmap
            bitmap.and(((RoaringPlainBitmap) other).getBitmap());
            return this;
        } else {
            // must resize to right size before AND
            return resizeBlock(other.getContainerSize()).and(other);
        }
    }

    @Override
    public PlainBitmap or(PlainBitmap other) throws MpcAbortException {
        assert other!=null : "bitmap must not be null.";
        if (other instanceof RoaringPlainBitmap) {
            bitmap.or(((RoaringPlainBitmap) other).getBitmap());
            return this;
        } else {
            return resizeBlock(other.getContainerSize()).or(other);
        }
    }

    @Override
    public int bitCount() throws MpcAbortException {
        return bitmap.getCardinality();
    }

    @Override
    public MutablePlainBitmap resizeBlock(int blockSize) {
        // TODO 非常重要
        return null;
    }
}
