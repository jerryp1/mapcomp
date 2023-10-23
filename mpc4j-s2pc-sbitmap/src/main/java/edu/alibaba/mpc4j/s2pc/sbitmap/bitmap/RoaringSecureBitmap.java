package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.SecureContainer;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.RoaringBitmapUtils;

import java.util.Arrays;

/**
 * roaring secure bitmap.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public class RoaringSecureBitmap implements SecureBitmap {
    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -5755874084688742399L;
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
     * the keys
     */
    private int[] keys;
    /**
     * Secure Containers.
     */
    private SecureContainer[] containers;
    /**
     * flag of full secure mode.
     */
    private boolean full;
    /**
     * number of containers.
     */
    private int containerNum;
    /**
     * container size.
     */
    private int containerSize;

    /**
     * Create a roaring secure bitmap in non-plain state.
     *
     * @param totalBitNum total number of bits.
     * @param keys        the keys.
     * @param vectors     the secure bit vectors.
     * @return the created secure bitmap.
     */
    public static SecureBitmap fromSbitVectors(int totalBitNum, int[] keys, SquareZ2Vector[] vectors, boolean full) {
        assert keys.length == vectors.length : "Length of keys and bitVectors not match";
        RoaringSecureBitmap secureBitmap = new RoaringSecureBitmap();
        secureBitmap.setTotalBitNum(totalBitNum);
        MathPreconditions.checkEqual("keys.length", "vectors.length", keys.length, vectors.length);
        // check all keys are in range [0, totalContainerNum)
        if (!full) {
            for (int key : keys) {
                MathPreconditions.checkNonNegativeInRange("key", key, secureBitmap.totalContainerNum);
            }
        }
        // check that all vectors are non-plain
        Arrays.stream(vectors).forEach(vector ->
            Preconditions.checkArgument(!vector.isPlain(), "all vectors must in non-plain state")
        );
        secureBitmap.plain = false;
        secureBitmap.keys = keys;
        secureBitmap.containers = Arrays.stream(vectors).map(SecureContainer::create).toArray(SecureContainer[]::new);
        secureBitmap.full = full;
        secureBitmap.containerNum = keys.length;
        secureBitmap.containerSize = vectors[0].bitNum();

        return secureBitmap;
    }

    public static SecureBitmap createEmpty(int totalBitNum) {
        return fromSbitVectors(totalBitNum, new int[0], new SquareZ2Vector[0], false);
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
    @Override
    public int[] getKeys() {
        return keys;
    }

    /**
     * Returns the number of containers in the secure bit vectors.
     *
     * @return the number of containers in the secure bit vectors.
     */
    public int containerNum() {
        return containerNum;
    }

    @Override
    public SecureBitmapFactory.SecureBitmapType getType() {
        return SecureBitmapFactory.SecureBitmapType.ROARING;
    }

    @Override
    public int totalBitNum() {
        return totalBitNum;
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public boolean isFull() {
        return full;
    }

    @Override
    public int getContainerSize() {
        return containerSize;
    }

    @Override
    public SecureContainer[] getContainers() {
        return containers;
    }


}
