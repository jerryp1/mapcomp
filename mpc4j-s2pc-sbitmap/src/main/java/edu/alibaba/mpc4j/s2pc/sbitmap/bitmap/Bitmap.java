package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.Container;

/**
 * Bitmap interface.
 *
 * @author Li Peng
 * @date 2023/8/11
 */
public interface Bitmap {
    /**
     * Returns the SecureBitMap type.
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
     * Returns whether the secure bitmap is in plain state or not.
     *
     * @return whether the secure bitmap is in plain state or not.
     */
    boolean isPlain();

    /**
     * Returns whether the secure bitmap is in full secure mode or not.
     *
     * @return whether the secure bitmap is in full secure mode or not.
     */
    boolean isFull();

    /**
     * Return the size of container of bitmap.
     *
     * @return the size of container of bitmap.
     */
    int getContainerSize();

    /**
     * Return the keys of containers.
     *
     * @return the keys of containers.
     */
    int[] getKeys();

    /**
     * Return the containers.
     *
     * @return the containers.
     */
    Container[] getContainers();

    /**
     * Return the number of containers.
     *
     * @return the number of containers.
     */
    default int getContainerNum() {
        return getContainers().length;
    }

    ;
}
