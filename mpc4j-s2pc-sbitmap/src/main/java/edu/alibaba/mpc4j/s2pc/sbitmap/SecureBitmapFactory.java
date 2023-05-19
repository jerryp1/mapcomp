package edu.alibaba.mpc4j.s2pc.sbitmap;

import org.roaringbitmap.RoaringBitmap;

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
    public enum SecureBitmapPtoType {
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
    public enum SecureBitmapType {
        /**
         * fully share the Bitmap.
         */
        FULL,
        /**
         * Roaring share the Bitmap.
         */
        ROARING,
    }

    /**
     * Create a (plain) secure bitmap in plain state.
     *
     * @param type        the secure bitmap tye.
     * @param totalBitNum the total number of bits.
     * @param bitmap      the plain bitmap.
     * @return the created secure bitmap.
     */
    public static SecureBitmap createFromBitMap(SecureBitmapType type, int totalBitNum, RoaringBitmap bitmap) {
        switch (type) {
            case FULL:
                return FullSecureBitmap.fromBitmap(totalBitNum, bitmap);
            case ROARING:
                return RoaringSecureBitmap.fromBitmap(totalBitNum, bitmap);
            default:
                throw new IllegalArgumentException("Invalid " + SecureBitmapType.class.getSimpleName() + " type: " + type.name());
        }
    }

    /**
     * Create a (plain) secure bitmap with all 1's in the given range [rangeStart, rangeEnd).
     * The input range must be valid, that is,
     * <p>
     * <li>rangeEnd should be in range (0, totalBitNum)</li>
     * <li>rangeStart should be in range [0, rangeEnd)</li>
     * </p>
     *
     * @param type        the secure bitmap type.
     * @param totalBitNum total number of bits.
     * @param rangeStart  inclusive beginning of range.
     * @param rangeEnd    exclusive ending of range.
     * @return the created secure bitmap.
     * @throws IllegalArgumentException if totalBitNum or the range is invalid.
     */
    public static SecureBitmap createOfRange(SecureBitmapType type, int totalBitNum, int rangeStart, int rangeEnd) {
        switch (type) {
            case FULL:
                return FullSecureBitmap.ofRange(totalBitNum, rangeStart, rangeEnd);
            case ROARING:
                return RoaringSecureBitmap.ofRange(totalBitNum, rangeStart, rangeEnd);
            default:
                throw new IllegalArgumentException("Invalid " + SecureBitmapType.class.getSimpleName() + " type: " + type.name());
        }
    }

    /**
     * Create a (plain) secure bitmap with all 1's in bit positions.
     *
     * @param type        the secure bitmap type.
     * @param totalBitNum max number of bits.
     * @return the created secure bitmap.
     */
    public static SecureBitmap createOnes(SecureBitmapType type, int totalBitNum) {
        switch (type) {
            case FULL:
                return FullSecureBitmap.ones(totalBitNum);
            case ROARING:
                return RoaringSecureBitmap.ones(totalBitNum);
            default:
                throw new IllegalArgumentException("Invalid " + SecureBitmapType.class.getSimpleName() + " type: " + type.name());
        }
    }
}
