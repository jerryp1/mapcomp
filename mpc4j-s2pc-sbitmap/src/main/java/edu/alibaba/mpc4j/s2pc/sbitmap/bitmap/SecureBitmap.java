package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.SecureContainer;

/**
 * SecureBitmap data structure interface.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public interface SecureBitmap extends Bitmap {
    /**
     * Return the SecureBitMap type.
     *
     * @return the SecureBitMap type.
     */
    @Override
    SecureBitmapFactory.SecureBitmapType getType();


    /**
     * Returns the total number of bits stored in the secure bitmap.
     *
     * @return the total number of bits stored in the secure bitmap.
     */
    @Override
    int totalBitNum();

    /**
     * Returns the total number of bytes stored in the secure bitmap.
     *
     * @return the total number of bytes stored in the secure bitmap.
     */
    @Override
    int totalByteNum();

    /**
     * Return whether the secure bitmap is in plain state or not.
     *
     * @return whether the secure bitmap is in plain state or not.
     */
    @Override
    boolean isPlain();

    /**
     * Return whether the secure bitmap is in full secure mode or not.
     *
     * @return whether the secure bitmap is in full secure mode or not.
     */
    @Override
    boolean isFull();

    /**
     * Return the containers.
     *
     * @return containers.
     */
    @Override
    SecureContainer[] getContainers();

    /**
     * Return the keys.
     *
     * @return containers.
     */
    @Override
    int[] getKeys();
}
