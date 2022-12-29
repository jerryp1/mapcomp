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
     * Returns the maximal number of containers allowed in the SecureBitmap.
     *
     * @return the maximal number of containers allowed in the SecureBitmap.
     */
    int maxContainerNum();

    /**
     * Returns the maximal number of bits stored in the SecureBitmap, which must divide maxContainerNum().
     *
     * @return the maximal number of bits stored in the SecureBitmap.
     */
    int maxBitNum();

    /**
     * Returns the maximal number of bytes stored in the SecureBitmap.
     *
     * @return the maximal number of bytes stored in the SecureBitmap.
     */
    int maxByteNum();

    /**
     * Return whether the SecureBitMap is in plain state or not.
     *
     * @return whether the SecureBitMap is in plain state or not.
     */
    boolean isPlain();

    /**
     * Returns the corresponding RoaringBitmap if the SecureBitMap is in plain state.
     *
     * @return the corresponding RoaringBitmap.
     * @throws IllegalStateException if the SecureBitMap is not in plain state.
     */
    RoaringBitmap toRoaringBitmap();
}
