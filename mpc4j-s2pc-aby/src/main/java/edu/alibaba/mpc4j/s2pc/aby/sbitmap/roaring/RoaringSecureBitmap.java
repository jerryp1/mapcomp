package edu.alibaba.mpc4j.s2pc.aby.sbitmap.roaring;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;
import edu.alibaba.mpc4j.s2pc.aby.sbitmap.AbstractSecureBitmap;
import edu.alibaba.mpc4j.s2pc.aby.sbitmap.RoaringBitmapUtils;
import edu.alibaba.mpc4j.s2pc.aby.sbitmap.SecureBitmapFactory;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;

/**
 * Secure Bitmap with raoring format.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public class RoaringSecureBitmap extends AbstractSecureBitmap {
    /**
     * the keys
     */
    private final char[] keys;
    /**
     * the non-plain RoaringBitMap
     */
    private final SquareSbitVector[] vectors;

    /**
     * Create a RoaringSecureBitmap in plain state.
     *
     * @param maxBitNum     max number of bits.
     * @param roaringBitmap the plain RoaringBitmap.
     * @throws IllegalArgumentException if the maxBitNum is invalid, or the given RoaringBitmap contains elements larger
     *                                  than the max number of bits.
     */
    public RoaringSecureBitmap(int maxBitNum, RoaringBitmap roaringBitmap) {
        super(maxBitNum, roaringBitmap);
        keys = null;
        vectors = null;
    }

    /**
     * Create a RoaringSecureBitmap in non-plain state.
     *
     * @param maxBitNum max number of bits.
     * @param keys      the keys.
     * @param vectors   the non-plain secure bit vectors.
     * @throws IllegalArgumentException if the maxBitNum is invalid, or the given RoaringBitmap contains elements larger
     *                                  than the max number of bits.
     */
    public RoaringSecureBitmap(int maxBitNum, char[] keys, SquareSbitVector[] vectors) {
        super(maxBitNum);
        MathPreconditions.checkEqual("keys.length", "vectors.length", keys.length, vectors.length);
        // check all keys are in range [0, maxContainerNum)
        for (char key : keys) {
            MathPreconditions.checkNonNegativeInRange("key", key, maxContainerNum);
        }
        this.keys = keys;
        // check that all vectors are non-plain
        Arrays.stream(vectors).forEach(vector ->
            Preconditions.checkArgument(!vector.isPlain(), "all vectors must in non-plain state")
        );
        this.vectors = vectors;
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
     * Returns the secure bit vectors.
     *
     * @return the secure bit vectors.
     */
    public SquareSbitVector[] getVectors() {
        return vectors;
    }

    /**
     * Returns the number of secure bit vectors, which is equal to the number of containers.
     *
     * @return the number of secure bit vectors.
     */
    public int getVectorNum() {
        return vectors.length;
    }

    @Override
    public SecureBitmapFactory.SecureBitMapType getType() {
        return SecureBitmapFactory.SecureBitMapType.ROARING;
    }
}
