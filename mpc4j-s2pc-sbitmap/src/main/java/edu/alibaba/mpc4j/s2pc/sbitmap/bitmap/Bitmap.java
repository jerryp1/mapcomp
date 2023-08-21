package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Bitmap接口
 * @author Li Peng
 * @date 2023/8/11
 */
public interface Bitmap {
    /**
     * Return the SecureBitMap type.
     *
     * @return the SecureBitMap type.
     */
    SecureBitmapFactory.SecureBitmapType getType();
    /**
     * Returns the total number of bits stored in the secure bitmap.
     *
     * @return the total number of bits stored in the secure bitmap.
     */
    int totalBitNum();

    /**
     * Returns the total number of bytes stored in the secure bitmap.
     *
     * @return the total number of bytes stored in the secure bitmap.
     */
    int totalByteNum();

    /**
     * Return whether the secure bitmap is in plain state or not.
     *
     * @return whether the secure bitmap is in plain state or not.
     */
    boolean isPlain();

    /**
     * Return whether the secure bitmap is in full secure mode or not.
     *
     * @return whether the secure bitmap is in full secure mode or not.
     */
    boolean isFull();

    /**
     * Return the size of container of bitmap.
     * @return the size of container of bitmap.
     */
    int getContainerSize();


    int[] getKeys();
}
