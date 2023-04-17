package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
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
    private SquareShareZ2Vector sbitVector;

    /**
     * Create a full secure bitmap in plain state.
     *
     * @param totalBitNum total number of bits.
     * @param bitmap    the plain bitmap.
     * @return the created secure bitmap.
     */
    public static FullSecureBitmap fromBitmap(int totalBitNum, RoaringBitmap bitmap) {
        RoaringBitmapUtils.checkContainValidBits(totalBitNum, bitmap);
        FullSecureBitmap secureBitmap = new FullSecureBitmap();
        secureBitmap.setTotalBitNum(totalBitNum);
        secureBitmap.plain = true;
        secureBitmap.bitmap = bitmap;
        secureBitmap.sbitVector = null;

        return secureBitmap;
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
    public static FullSecureBitmap ofRange(int totalBitNum, int rangeStart, int rangeEnd) {
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
    public static FullSecureBitmap ones(int totalBitNum) {
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(0, totalBitNum);

        return fromBitmap(totalBitNum, roaringBitmap);
    }

    /**
     * Create a full secure bitmap in non-plain state.
     *
     * @param vector the secure bit vector.
     * @return the created secure bitmap.
     * @throws IllegalArgumentException if the number of bits in the secure bit vector is invalid.
     */
    public static FullSecureBitmap fromSbitVectors(SquareShareZ2Vector vector) {
        FullSecureBitmap secureBitmap = new FullSecureBitmap();
        int totalBitNum = vector.getNum();
        secureBitmap.setTotalBitNum(totalBitNum);
        // check that the vector is in non-plain state.
        Preconditions.checkArgument(!vector.isPlain(), "all vectors must in non-plain state");
        secureBitmap.plain = false;
        secureBitmap.bitmap = null;
        secureBitmap.sbitVector = vector;

        return secureBitmap;
    }

    private void setTotalBitNum(int totalBitNum) {
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        this.totalBitNum = totalBitNum;
        totalContainerNum = RoaringBitmapUtils.getContainerNum(totalBitNum);
        totalByteNum = RoaringBitmapUtils.getByteNum(totalBitNum);
    }

    /**
     * Returns the secure bit vector. The plain state is identical to the plain state of the secure bitmap.
     *
     * @return the secure bit vector.
     */
    public SquareShareZ2Vector getSbitVector() {
        if (plain) {
            // convert the bitmap to be a bit vector, then create its corresponding secure bit vector.
            BitVector bitVector = RoaringBitmapUtils.toBitVector(totalBitNum, bitmap);
            return SquareShareZ2Vector.create(bitVector, plain);
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
    public int totalContainerNum() {
        return totalContainerNum;
    }

    @Override
    public int totalBitNum() {
        return totalBitNum;
    }

    @Override
    public int totalByteNum() {
        return totalByteNum;
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public RoaringBitmap toBitmap() {
        Preconditions.checkArgument(plain, "secure bitmap must be in plain state");
        return bitmap;
    }
}
