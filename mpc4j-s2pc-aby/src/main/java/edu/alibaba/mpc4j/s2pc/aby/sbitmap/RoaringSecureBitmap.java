package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
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
     * @param maxBitNum max number of bits.
     * @param bitmap    the plain bitmap.
     * @return the created secure bitmap.
     * @throws IllegalArgumentException if maxBitNum is invalid, or a key is larger than the max number of containers.
     */
    public static RoaringSecureBitmap fromBitmap(int maxBitNum, RoaringBitmap bitmap) {
        RoaringSecureBitmap secureBitmap = new RoaringSecureBitmap();
        secureBitmap.setMaxBitNum(maxBitNum);
        // check all keys are in range [0, maxContainerNum)
        char[] keys = RoaringBitmapUtils.getKeyCharArray(bitmap);
        for (char key : keys) {
            MathPreconditions.checkNonNegativeInRange("key", key, secureBitmap.maxContainerNum);
        }
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
    public static RoaringSecureBitmap ofRange(int maxBitNum, int rangeStart, int rangeEnd) {
        RoaringBitmapUtils.checkValidBitNum(maxBitNum);
        // check if range is valid
        MathPreconditions.checkPositiveInRange("rangeEnd", rangeEnd, maxBitNum);
        MathPreconditions.checkNonNegativeInRange("rangeStart", rangeStart, rangeEnd);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(rangeStart, rangeEnd);

        return fromBitmap(maxBitNum, roaringBitmap);
    }

    /**
     * Create a (plain) roaring secure bitmap with all 1's in bit positions.
     *
     * @param maxBitNum max number of bits.
     * @return  the created secure bitmap.
     */
    public static RoaringSecureBitmap ones(int maxBitNum) {
        RoaringBitmapUtils.checkValidBitNum(maxBitNum);
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(0, maxBitNum);

        return fromBitmap(maxBitNum, roaringBitmap);
    }

    /**
     * Create a roaring secure bitmap in non-plain state.
     *
     * @param maxBitNum max number of bits.
     * @param keys      the keys.
     * @param vectors   the secure bit vectors.
     * @return the created secure bitmap.
     * @throws IllegalArgumentException if maxBitNum is invalid, or a key is larger than the max number of containers.
     */
    public static RoaringSecureBitmap fromSbitVectors(int maxBitNum, char[] keys, SquareSbitVector[] vectors) {
        RoaringSecureBitmap secureBitmap = new RoaringSecureBitmap();
        secureBitmap.setMaxBitNum(maxBitNum);
        MathPreconditions.checkEqual("keys.length", "vectors.length", keys.length, vectors.length);
        // check all keys are in range [0, maxContainerNum)
        for (char key : keys) {
            MathPreconditions.checkNonNegativeInRange("key", key, secureBitmap.maxContainerNum);
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

    private void setMaxBitNum(int maxBitNum) {
        RoaringBitmapUtils.checkValidBitNum(maxBitNum);
        this.maxBitNum = maxBitNum;
        maxContainerNum = RoaringBitmapUtils.getContainerNum(maxBitNum);
        maxByteNum = RoaringBitmapUtils.getByteNum(maxBitNum);
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
            BitVector[] bitVectors = RoaringBitmapUtils.toRoaringBitVectors(maxBitNum, bitmap);
            return Arrays.stream(bitVectors)
                .map(bitVector -> SquareSbitVector.create(bitVector, plain))
                .toArray(SquareSbitVector[]::new);
        } else {
            // directly return the secure bit vector array.
            return sbitVectors;
        }
    }

    /**
     * Returns the number of secure bit vectors, which is equal to the number of containers.
     *
     * @return the number of secure bit vectors.
     */
    public int getVectorNum() {
        return sbitVectors.length;
    }

    @Override
    public SecureBitmapFactory.SecureBitmapType getType() {
        return SecureBitmapFactory.SecureBitmapType.ROARING;
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
