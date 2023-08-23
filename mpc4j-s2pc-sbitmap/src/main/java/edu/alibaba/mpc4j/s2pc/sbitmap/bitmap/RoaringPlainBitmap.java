package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapFactory.SecureBitmapType;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.Container;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.PlainContainer;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.RoaringBitmapUtils;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Plain bitmap
 *
 * @author Li Peng
 * @date 2023/8/11
 */
public class RoaringPlainBitmap implements PlainBitmap {
    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -3567894834063605593L;
    /**
     * total number of bits.
     */
    protected int totalBitNum;
    /**
     * roaring bitmap.
     */
    private RoaringBitmap bitmap;
    /**
     * full state.
     */
    private boolean full;
    /**
     * container size.
     */
    public static final int CONTAINER_SIZE = 1 << 16;

    /**
     * Create a bitmap in plain state.
     *
     * @param totalBitNum total number of bits.
     * @param bitmap      the plain bitmap.
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
    public static RoaringPlainBitmap fromBitVectors(int totalBitNum, int[] keys, Container[] containers) {
        return fromBitVectors(totalBitNum, keys, Arrays.stream(containers).map(Container::getBitVector).toArray(BitVector[]::new));
    }

    /**
     * Create a full secure bitmap in plain state.
     *
     * @param totalBitNum total number of bits.
     * @return the created secure bitmap.
     */
    public static RoaringPlainBitmap fromBitVectors(int totalBitNum, int[] keys, BitVector[] bitVectors) {
        assert keys.length == bitVectors.length : "Length of keys and bitVectors not match";
        RoaringBitmap bitmap = RoaringBitmapUtils.toRoaringBitmap(keys, bitVectors);

        RoaringBitmapUtils.checkContainValidBits(totalBitNum, bitmap);
        RoaringPlainBitmap secureBitmap = new RoaringPlainBitmap();
        secureBitmap.setTotalBitNum(totalBitNum);
        secureBitmap.bitmap = bitmap;
        secureBitmap.full = false;
        return secureBitmap;
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
    public boolean isPlain() {
        return true;
    }

    @Override
    public boolean isFull() {
        return full;
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public Container[] getContainers() {
        return Arrays.stream(RoaringBitmapUtils.toRoaringBitVectors(totalBitNum, bitmap))
            .map(PlainContainer::create).toArray(Container[]::new);
    }

    public void setTotalBitNum(int totalBitNum) {
        this.totalBitNum = totalBitNum;
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
     * @param totalBitNum total number of bits.
     * @param rangeStart  inclusive beginning of range.
     * @param rangeEnd    exclusive ending of range.
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
     * @return the created secure bitmap.
     */
    public static RoaringPlainBitmap ones(int totalBitNum) {
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        RoaringBitmapUtils.checkValidMaxBitNum(totalBitNum);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(0, totalBitNum);

        return fromBitmap(totalBitNum, roaringBitmap);
    }

    /**
     * Transfer to full bitmap.
     *
     * @return full bitmap.
     */
    public RoaringPlainBitmap toFull() {
        BitVector[] vectors = RoaringBitmapUtils.toRoaringBitVectors(totalBitNum, bitmap);
        int totalContainerNum = CommonUtils.getUnitNum(totalBitNum, CONTAINER_SIZE);
        int[] keys = IntStream.range(0, totalContainerNum).toArray();
        return fromBitVectors(totalBitNum, keys, vectors);
    }


    public MutablePlainBitmap toDpRandom(double epsilon) {
        return BitmapUtils.toDpMutablePlainBitmap(this, epsilon);
    }

    /**
     * Transfer to full bitmap.
     *
     * @return full bitmap.
     */
    public void setFull(boolean full) {
        this.full = full;
    }

    /**
     * Get keys of containers.
     *
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
    public PlainBitmap andi(PlainBitmap other) {
        assert other != null : "bitmap must not be null.";
        if (other instanceof RoaringPlainBitmap) {
            // direct AND bitmap
            bitmap.and(((RoaringPlainBitmap) other).getBitmap());
            return this;
        } else {
            // must resize to right size before AND
            return resizeContainer(other.getContainerSize()).andi(other);
        }
    }

    @Override
    public PlainBitmap ori(PlainBitmap other) {
        assert other != null : "bitmap must not be null.";
        if (other instanceof RoaringPlainBitmap) {
            bitmap.or(((RoaringPlainBitmap) other).getBitmap());
            return this;
        } else {
            return resizeContainer(other.getContainerSize()).ori(other);
        }
    }

    @Override
    public int bitCount() {
        return bitmap.getCardinality();
    }

    private MutablePlainBitmap toMutablePlainBitmap() {
        BitVector[] vectors = RoaringBitmapUtils.toRoaringBitVectors(totalBitNum, bitmap);
        return MutablePlainBitmap.create(totalBitNum, getKeys(), vectors);
    }

    @Override
    public MutablePlainBitmap resizeContainer(int newContainerSize) {
        // transfer to MutablePlainBitmap
        MutablePlainBitmap mutablePlainBitmap = toMutablePlainBitmap();
        // resize
        return BitmapUtils.resize(mutablePlainBitmap, newContainerSize);
    }

    @Override
    public boolean isIntermediate() {
        return true;
    }
}
