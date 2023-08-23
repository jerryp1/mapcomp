package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

/**
 * Plain bitmap interface.
 *
 * @author Li Peng
 * @date 2023/8/15
 */
public interface PlainBitmap extends Bitmap {
    /**
     * Inner AND operation of bitmap.
     *
     * @param other the other operator.
     * @return AND result.
     */
    PlainBitmap andi(PlainBitmap other);

    /**
     * Inner OR operation of bitmap.
     *
     * @param other the other operator.
     * @return OR result.
     */
    PlainBitmap ori(PlainBitmap other);

    /**
     * Count the number of valid bits of bitmap.
     *
     * @return count result.
     */
    int bitCount();

    /**
     * Resize the containers to new containerSize.
     *
     * @param containerSize new size of containers.
     * @return resized container.
     */
    MutablePlainBitmap resizeContainer(int containerSize);

    /**
     * Return whether the bitmap is only used in intermediate process.
     *
     * @return Whether the bitmap is only used in intermediate process.
     */
    boolean isIntermediate();
}
