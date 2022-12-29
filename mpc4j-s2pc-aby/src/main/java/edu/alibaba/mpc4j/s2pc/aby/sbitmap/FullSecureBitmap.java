package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;
import edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils.RoaringBitmapUtils;
import org.roaringbitmap.RoaringBitmap;

/**
 * Full secure bitmap.
 *
 * @author Weiran Liu
 * @date 2022/12/29
 */
public class FullSecureBitmap implements SecureBitmap {
    /**
     * maximal number of bits.
     */
    protected int maxBitNum;
    /**
     * maximal number of containers.
     */
    protected int maxContainerNum;
    /**
     * maximal number of bytes.
     */
    protected int maxByteNum;
    /**
     * the plain state
     */
    protected boolean plain;
    /**
     * the plain bitmap
     */
    protected RoaringBitmap bitmap;
    /**
     * the non-plain bitmap
     */
    private SquareSbitVector sbitVector;

    /**
     * Create a full secure bitmap in plain state.
     *
     * @param maxBitNum max number of bits.
     * @param bitmap    the plain bitmap.
     * @return the created secure bitmap.
     * @throws IllegalArgumentException if maxBitNum is invalid, or a key is larger than the max number of containers.
     */
    public static FullSecureBitmap fromBitmap(int maxBitNum, RoaringBitmap bitmap) {
        FullSecureBitmap secureBitmap = new FullSecureBitmap();
        secureBitmap.setMaxBitNum(maxBitNum);
        // check all keys are in range [0, maxContainerNum)
        char[] keys = RoaringBitmapUtils.getKeyCharArray(bitmap);
        for (char key : keys) {
            MathPreconditions.checkNonNegativeInRange("key", key, secureBitmap.maxContainerNum);
        }
        secureBitmap.plain = true;
        secureBitmap.bitmap = bitmap;
        secureBitmap.sbitVector = null;

        return secureBitmap;
    }

    /**
     * Create a (plain) full secure bitmap with all 1's in the given range [rangeStart, rangeEnd).
     * The input range must be valid, that is,
     * <p>
     * <li>rangeEnd should be in range (0, maxBitNum)</li>
     * <li>rangeStart should be in range [0, rangeEnd)</li>
     * </p>
     *
     * @param maxBitNum  max number of bits.
     * @param rangeStart inclusive beginning of range.
     * @param rangeEnd   exclusive ending of range.
     * @return the created secure bitmap.
     * @throws IllegalArgumentException if maxBitNum or the range is invalid.
     */
    public static FullSecureBitmap ofRange(int maxBitNum, int rangeStart, int rangeEnd) {
        RoaringBitmapUtils.checkValidBitNum(maxBitNum);
        // check if range is valid
        MathPreconditions.checkPositiveInRange("rangeEnd", rangeEnd, maxBitNum);
        MathPreconditions.checkNonNegativeInRange("rangeStart", rangeStart, rangeEnd);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(rangeStart, rangeEnd);

        return fromBitmap(maxBitNum, roaringBitmap);
    }

    /**
     * Create a (plain) full secure bitmap with all 1's in bit positions.
     *
     * @param maxBitNum max number of bits.
     * @return  the created secure bitmap.
     */
    public static FullSecureBitmap ones(int maxBitNum) {
        RoaringBitmapUtils.checkValidBitNum(maxBitNum);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(0, maxBitNum);

        return fromBitmap(maxBitNum, roaringBitmap);
    }

    /**
     * Create a full secure bitmap in non-plain state.
     *
     * @param vector the secure bit vector.
     * @return the created secure bitmap.
     * @throws IllegalArgumentException if the number of bits in the secure bit vector is invalid.
     */
    public static FullSecureBitmap fromSbitVectors(SquareSbitVector vector) {
        FullSecureBitmap secureBitmap = new FullSecureBitmap();
        int maxBitNum = vector.bitNum();
        secureBitmap.setMaxBitNum(maxBitNum);
        // check that the vector is in non-plain state.
        Preconditions.checkArgument(!vector.isPlain(), "all vectors must in non-plain state");
        secureBitmap.plain = false;
        secureBitmap.bitmap = null;
        secureBitmap.sbitVector = vector;

        return secureBitmap;
    }

    private void setMaxBitNum(int maxBitNum) {
        RoaringBitmapUtils.checkValidBitNum(maxBitNum);
        this.maxBitNum = maxBitNum;
        maxContainerNum = RoaringBitmapUtils.getContainerNum(maxBitNum);
        maxByteNum = RoaringBitmapUtils.getByteNum(maxBitNum);
    }

    /**
     * Returns the secure bit vector. The plain state is identical to the plain state of the secure bitmap.
     *
     * @return the secure bit vector.
     */
    public SquareSbitVector getSbitVector() {
        if (plain) {
            // convert the bitmap to be a bit vector, then create its corresponding secure bit vector.
            BitVector bitVector = RoaringBitmapUtils.toBitVector(maxBitNum, bitmap);
            return SquareSbitVector.create(bitVector, plain);
        } else {
            // directly return the secure bit vector.
            return sbitVector;
        }
    }

    @Override
    public SecureBitmapFactory.SecureBitmapType getType() {
        return SecureBitmapFactory.SecureBitmapType.FULL;
    }

    @Override
    public int maxContainerNum() {
        return maxContainerNum;
    }

    @Override
    public int maxBitNum() {
        return maxBitNum;
    }

    @Override
    public int maxByteNum() {
        return maxByteNum;
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public RoaringBitmap toRoaringBitmap() {
        Preconditions.checkArgument(plain, "secure bitmap must be in plain state");
        return bitmap;
    }
}
