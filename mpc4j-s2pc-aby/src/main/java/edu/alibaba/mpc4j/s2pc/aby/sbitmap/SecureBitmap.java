package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

import org.roaringbitmap.RoaringBitmap;

/**
 * SecureBitmap data structure interface.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public interface SecureBitmap {
    /**
     * Return the SecureBitMap type.
     *
     * @return the SecureBitMap type.
     */
    SecureBitmapFactory.SecureBitmapType getType();

    /**
     * Returns the total number of containers allowed in the secure bitmap.
     *
     * @return the total number of containers allowed in the secure bitmap.
     */
    int totalContainerNum();

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
     * Returns the corresponding bitmap if the secure bitmap is in plain state.
     *
     * @return the corresponding bitmap.
     * @throws IllegalStateException if the secure bitmap is not in plain state.
     */
    RoaringBitmap toBitmap();
}
