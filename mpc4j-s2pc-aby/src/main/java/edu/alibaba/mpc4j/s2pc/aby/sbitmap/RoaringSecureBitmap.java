package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.matrix.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;
import edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils.RoaringBitmapUtils;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;

/**
 * roaring secure bitmap.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public class RoaringSecureBitmap implements SecureBitmap {
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
     * the keys
     */
    private char[] keys;
    /**
     * the non-plain bitmap
     */
    private SquareSbitVector[] sbitVectors;

    /**
     * Create a roaring secure bitmap in plain state.
     *
     * @param totalBitNum total number of bits.
     * @param bitmap      the plain bitmap.
     * @return the created secure bitmap.
     */
    public static RoaringSecureBitmap fromBitmap(int totalBitNum, RoaringBitmap bitmap) {
        RoaringBitmapUtils.checkContainValidBits(totalBitNum, bitmap);
        RoaringSecureBitmap secureBitmap = new RoaringSecureBitmap();
        secureBitmap.setTotalBitNum(totalBitNum);
        secureBitmap.plain = true;
        secureBitmap.bitmap = bitmap;
        secureBitmap.keys = null;
        secureBitmap.sbitVectors = null;

        return secureBitmap;
    }

    /**
     * Create a (plain) roaring secure bitmap with all 1's in the given range [rangeStart, rangeEnd).
     * The input range must be valid, that is,
     * <p>
     * <li>rangeEnd should be in range (0, totalBitNum)</li>
     * <li>rangeStart should be in range [0, rangeEnd)</li>
     * </p>
     *
     * @param totalBitNum max number of bits.
     * @param rangeStart  inclusive beginning of range.
     * @param rangeEnd    exclusive ending of range.
     * @return the created secure bitmap.
     */
    public static RoaringSecureBitmap ofRange(int totalBitNum, int rangeStart, int rangeEnd) {
        // check if range is valid
        MathPreconditions.checkPositiveInRange("rangeEnd", rangeEnd, totalBitNum);
        MathPreconditions.checkNonNegativeInRange("rangeStart", rangeStart, rangeEnd);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(rangeStart, rangeEnd);

        return fromBitmap(totalBitNum, roaringBitmap);
    }

    /**
     * Create a (plain) roaring secure bitmap with all 1's in bit positions.
     *
     * @param totalBitNum total number of bits.
     * @return the created secure bitmap.
     */
    public static RoaringSecureBitmap ones(int totalBitNum) {
        RoaringBitmapUtils.checkValidMaxBitNum(totalBitNum);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(0, totalBitNum);

        return fromBitmap(totalBitNum, roaringBitmap);
    }

    /**
     * Create a roaring secure bitmap in non-plain state.
     *
     * @param totalBitNum total number of bits.
     * @param keys        the keys.
     * @param vectors     the secure bit vectors.
     * @return the created secure bitmap.
     */
    public static RoaringSecureBitmap fromSbitVectors(int totalBitNum, char[] keys, SquareSbitVector[] vectors) {
        RoaringSecureBitmap secureBitmap = new RoaringSecureBitmap();
        secureBitmap.setTotalBitNum(totalBitNum);
        MathPreconditions.checkEqual("keys.length", "vectors.length", keys.length, vectors.length);
        // check all keys are in range [0, totalContainerNum)
        for (char key : keys) {
            MathPreconditions.checkNonNegativeInRange("key", key, secureBitmap.totalContainerNum);
        }
        // check that all vectors are non-plain
        Arrays.stream(vectors).forEach(vector ->
            Preconditions.checkArgument(!vector.isPlain(), "all vectors must in non-plain state")
        );
        secureBitmap.plain = false;
        secureBitmap.bitmap = null;
        secureBitmap.keys = keys;
        secureBitmap.sbitVectors = vectors;

        return secureBitmap;
    }

    private void setTotalBitNum(int totalBitNum) {
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        this.totalBitNum = totalBitNum;
        totalContainerNum = RoaringBitmapUtils.getContainerNum(totalBitNum);
        totalByteNum = RoaringBitmapUtils.getByteNum(totalBitNum);
    }

    /**
     * Returns the keys.
     *
     * @return the keys.
     */
    public char[] getKeys() {
        return keys;
    }

    /**
     * Returns the secure bit vectors. The plain state is identical to the plain state of the secure bitmap.
     *
     * @return the secure bit vectors.
     */
    public SquareSbitVector[] getSbitVectors() {
        if (plain) {
            // convert the bitmap to be a bit vector array, then create its corresponding secure bit vector array.
            BitVector[] bitVectors = RoaringBitmapUtils.toRoaringBitVectors(totalBitNum, bitmap);
            return Arrays.stream(bitVectors)
                .map(bitVector -> SquareSbitVector.create(bitVector, plain))
                .toArray(SquareSbitVector[]::new);
        } else {
            // directly return the secure bit vector array.
            return sbitVectors;
        }
    }

    /**
     * Returns the number of containers in the secure bit vectors.
     *
     * @return the number of containers in the secure bit vectors.
     */
    public int containerNum() {
        return sbitVectors.length;
    }

    @Override
    public SecureBitmapFactory.SecureBitmapType getType() {
        return SecureBitmapFactory.SecureBitmapType.ROARING;
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
