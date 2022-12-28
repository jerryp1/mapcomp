package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

/**
 * SecureBitMap Factory.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public class SecureBitmapFactory {
    /**
     * private constructor.
     */
    private SecureBitmapFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum SecureBitMapPtoType {
        /**
         * fully share the Bitmap.
         */
        FULL_SHARE,
        /**
         * Roaring share the Bitmap.
         */
        ROARING_SHARE,
        /**
         * Roaring share the Bitmap in a differentially-private manner.
         */
        DP_ROARING_SHARE,
    }

    /**
     * SecureBitmap data structure type
     */
    public enum SecureBitMapType {
        /**
         * fully share the Bitmap.
         */
        FULL,
        /**
         * Roaring share the Bitmap.
         */
        ROARING,
    }
}
