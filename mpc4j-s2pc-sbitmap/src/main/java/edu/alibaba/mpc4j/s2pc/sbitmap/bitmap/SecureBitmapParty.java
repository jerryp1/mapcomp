package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.roaringbitmap.RoaringBitmap;

/**
 * SecureBitMap party interface.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public interface SecureBitmapParty extends TwoPartyPto, BitmapOperations {

    /**
     * Init the protocol.
     *
     * @param maxBitNum      maximal number of bits stored in the SecureBitmap.
     * @param estimateAndNum estimated AND operation num. The protocol would do some computations if the actual AND
     *                       operation num is greater than estimated AND operation num.
     * @throws MpcAbortException if the protocol aborts.
     */
    void init(int maxBitNum, int estimateAndNum) throws MpcAbortException;

    /**
     * Returns the max number of bits stored in the secure bitmap.
     *
     * @return the max number of bits stored in the secure bitmap.
     * @throws IllegalStateException if the protocol is not initialized yet.
     */
    int maxBitNum();

    /**
     * Check the range [rangeStart, rangeEnd) is valid, that is,
     * <p>
     *     <li>rangeEnd should be in range (0, maxBitNum)</li>
     *     <li>rangeStart should be in range [0, rangeEnd)</li>
     * </p>
     *
     * @param rangeStart inclusive beginning of range.
     * @param rangeEnd exclusive ending of range.
     * @throws IllegalArgumentException if the range is invalid.
     */
    default void rangeSanityCheck(final int rangeStart, final int rangeEnd) {
        MathPreconditions.checkPositiveInRange("rangeEnd", rangeEnd, maxBitNum());
        MathPreconditions.checkNonNegativeInRange("rangeStart", rangeStart, rangeEnd);
    }

    /**
     * Share its own bitmap.
     *
     * @param plainBitmap the bitmap to be shared.
     * @return the shared secure bitmap.
     */
    SecureBitmap shareOwn(PlainBitmap plainBitmap);

    /**
     * Share other's bitmap.
     *
     * @return the shared secure bitmap.
     * @throws MpcAbortException if the protocol aborts.
     */
    SecureBitmap shareOther() throws MpcAbortException;

    /**
     * Reveal own bitmap.
     *
     * @param secureBitmap the bitmap to be revealed.
     * @return the shared secure bitmap.
     * @throws MpcAbortException if the protocol aborts.
     */
    SecureBitmap revealOwn(SecureBitmap secureBitmap) throws MpcAbortException;

    /**
     * Reveal other's bitmap.
     *
     * @param secureBitmap the bitmap to be revealed.
     * @return the shared secure bitmap.
     * @throws MpcAbortException if the protocol aborts.
     */
    SecureBitmap revealOther(SecureBitmap secureBitmap) throws MpcAbortException;

//    /**
//     * Bitwise AND (intersection) operation. The provided secure bitmaps are *not* modified.
//     *
//     * @param xi the first secure bitmap.
//     * @param yi the other secure bitmap.
//     * @return result of the operation.
//     * @throws MpcAbortException if the protocol aborts.
//     */
//    SecureBitmap and(final SecureBitmap xi, final SecureBitmap yi) throws MpcAbortException;
//
//    /**
//     * Bitwise XOR (symmetric difference) operation. The provided secure bitmaps are *not* modified.
//     *
//     * @param xi the first secure bitmap.
//     * @param yi the other secure bitmap.
//     * @return result of the operation.
//     * @throws MpcAbortException if the protocol aborts.
//     */
//    SecureBitmap xor(final SecureBitmap xi, final SecureBitmap yi) throws MpcAbortException;

    /**
     * Returns the number of distinct integers added to the secure bitmap (e.g., number of bits set).
     * The result is returned to its own.
     *
     * @param xi the secure bitmap.
     * @return the cardinality.
     * @throws MpcAbortException if the protocol aborts.
     */
    int getCardinalityOwn(SecureBitmap xi) throws MpcAbortException;

    /**
     * Return (to other) the number of distinct integers added to the secure bitmap (e.g., number of bits set).
     * The result is returned to the other.
     *
     * @param xi the secure bitmap.
     * @throws MpcAbortException if the protocol aborts.
     */
    void getCardinalityOther(SecureBitmap xi) throws MpcAbortException;

//    /**
//     * Generate a new secure bitmap with all integers in [rangeStart, rangeEnd) added.
//     * The added range is only known by its own.
//     *
//     * @param xi the initial secure bitmap (will not be modified).
//     * @param rangeStart inclusive beginning of range.
//     * @param rangeEnd exclusive ending of range.
//     * @return new secure bitmap.
//     * @throws MpcAbortException if the protocol aborts.
//     */
//    default SecureBitmap addOwn(final SecureBitmap xi, int rangeStart, int rangeEnd) throws MpcAbortException {
//        rangeSanityCheck(rangeStart, rangeEnd);
//        // generate a new bitmap and do or operation.
//        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(rangeStart, rangeEnd);
//        SecureBitmap yi = shareOwn(RoaringPlainBitmap.fromBitmap(roaringBitmap.getCardinality(), roaringBitmap));
//        return or(xi, yi);
//    }

//    /**
//     * Generate a new secure bitmap with all integers in [rangeStart, rangeEnd) added.
//     * The added range is only known by the other.
//     *
//     * @param xi the initial secure bitmap (will not be modified).
//     * @return new secure bitmap.
//     * @throws MpcAbortException if the protocol aborts.
//     */
//    default SecureBitmap addOther(final SecureBitmap xi) throws MpcAbortException {
//        // do or operation with a new (unknown) bitmap generated by the other.
//        SecureBitmap yi = shareOther();
//        return or(xi, yi);
//    }

//    /**
//     * Cardinality of Bitwise AND (intersection) operation. The provided secure bitmaps are *not* modified.
//     * The result is returned by its own.
//     *
//     * @param xi the first secure bitmap.
//     * @param yi the other secure bitmap.
//     * @return as if you did and(xi, yi).getCardinality().
//     * @throws MpcAbortException if the protocol aborts.
//     */
//    default int andCardinalityOwn(final SecureBitmap xi, final SecureBitmap yi) throws MpcAbortException {
//        return getCardinalityOwn(and(xi, yi));
//    }
//
//    /**
//     * Bitwise OR (union) operation. The provided bitmaps are *not* modified.
//     *
//     * @param xi the first secure bitmap.
//     * @param yi the other secure bitmap.
//     * @return result of the operation.
//     * @throws MpcAbortException if the protocol aborts.
//     */
//    default SecureBitmap or(final SecureBitmap xi, final SecureBitmap yi) throws MpcAbortException {
//        return xor(xor(xi, yi), and(xi, yi));
//    }

//    /**
//     * Bitwise NOT operation. The provided secure bitmaps are *not* modified.
//     *
//     * @param xi the secure bitmap.
//     * @return result of the operation.
//     * @throws MpcAbortException if the protocol aborts.
//     */
//    default SecureBitmap not(final SecureBitmap xi) throws MpcAbortException {
//        return xor(xi, SecureBitmapFactory.createOnes(maxBitNum()));
//    }

    @Override
    Bitmap and(Bitmap x, Bitmap y) throws MpcAbortException;

    @Override
    Bitmap or(Bitmap x, Bitmap y) throws MpcAbortException;

    @Override
    int bitCount(Bitmap x);

    /**
     * Plain bitmap to full secure bimap
     * @param plainBitmap plain bitmap
     * @return full secure bimap
     * @throws MpcAbortException if the protocol aborts.
     */
    default SecureBitmap plainToFullSecure(PlainBitmap plainBitmap) throws MpcAbortException {
        // share
        return shareOwn(plainBitmap);
    }

    /**
     * Plain bitmap to full secure bimap
     * @return full secure bimap
     * @throws MpcAbortException if the protocol aborts.
     */
    default SecureBitmap plainToFullSecure() throws MpcAbortException {
        return shareOther();
    }

    /**
     * Plain bitmap to dp secure bimap
     * @param plainBitmap plain bitmap
     * @return dp secure bimap
     * @throws MpcAbortException if the protocol aborts.
     */
    default SecureBitmap plainToDpSecure(RoaringPlainBitmap plainBitmap, double epsilon) throws MpcAbortException {
        // dp
        MutablePlainBitmap mutablePlainBitmap = plainBitmap.toDpRandom(epsilon);
        // share
        return shareOwn(mutablePlainBitmap);
    }

    /**
     * Plain bitmap to dp secure bimap
     * @return dp secure bimap
     * @throws MpcAbortException if the protocol aborts.
     */
    default SecureBitmap plainToDpSecure() throws MpcAbortException {
        return shareOther();
    }
}
