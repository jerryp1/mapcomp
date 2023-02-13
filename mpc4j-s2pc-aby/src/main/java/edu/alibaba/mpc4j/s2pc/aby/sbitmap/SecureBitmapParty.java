package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

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
public interface SecureBitmapParty extends TwoPartyPto {
    /**
     * Returns the supported secure bitmap type.
     *
     * @return the supported secure bitmap type.
     */
    SecureBitmapFactory.SecureBitmapType supportSecureBitmapType();

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
     * @param roaringBitmap the bitmap to be shared.
     * @return the shared secure bitmap.
     */
    SecureBitmap shareOwn(RoaringBitmap roaringBitmap);

    /**
     * Share other's bitmap.
     *
     * @return the shared secure bitmap.
     * @throws MpcAbortException if the protocol aborts.
     */
    SecureBitmap shareOther() throws MpcAbortException;

    /**
     * Bitwise AND (intersection) operation. The provided secure bitmaps are *not* modified.
     *
     * @param xi the first secure bitmap.
     * @param yi the other secure bitmap.
     * @return result of the operation.
     * @throws MpcAbortException if the protocol aborts.
     */
    SecureBitmap and(final SecureBitmap xi, final SecureBitmap yi) throws MpcAbortException;

    /**
     * Bitwise XOR (symmetric difference) operation. The provided secure bitmaps are *not* modified.
     *
     * @param xi the first secure bitmap.
     * @param yi the other secure bitmap.
     * @return result of the operation.
     * @throws MpcAbortException if the protocol aborts.
     */
    SecureBitmap xor(final SecureBitmap xi, final SecureBitmap yi) throws MpcAbortException;

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

    /**
     * Generate a new secure bitmap with all integers in [rangeStart, rangeEnd) added.
     * The added range is only known by its own.
     *
     * @param xi the initial secure bitmap (will not be modified).
     * @param rangeStart inclusive beginning of range.
     * @param rangeEnd exclusive ending of range.
     * @return new secure bitmap.
     * @throws MpcAbortException if the protocol aborts.
     */
    default SecureBitmap addOwn(final SecureBitmap xi, int rangeStart, int rangeEnd) throws MpcAbortException {
        rangeSanityCheck(rangeStart, rangeEnd);
        // generate a new bitmap and do or operation.
        RoaringBitmap roaringBitmap = RoaringBitmap.bitmapOfRange(rangeStart, rangeEnd);
        SecureBitmap yi = shareOwn(roaringBitmap);
        return or(xi, yi);
    }

    /**
     * Generate a new secure bitmap with all integers in [rangeStart, rangeEnd) added.
     * The added range is only known by the other.
     *
     * @param xi the initial secure bitmap (will not be modified).
     * @return new secure bitmap.
     * @throws MpcAbortException if the protocol aborts.
     */
    default SecureBitmap addOther(final SecureBitmap xi) throws MpcAbortException {
        // do or operation with a new (unknown) bitmap generated by the other.
        SecureBitmap yi = shareOther();
        return or(xi, yi);
    }

    /**
     * Cardinality of Bitwise AND (intersection) operation. The provided secure bitmaps are *not* modified.
     * The result is returned by its own.
     *
     * @param xi the first secure bitmap.
     * @param yi the other secure bitmap.
     * @return as if you did and(xi, yi).getCardinality().
     * @throws MpcAbortException if the protocol aborts.
     */
    default int andCardinalityOwn(final SecureBitmap xi, final SecureBitmap yi) throws MpcAbortException {
        return getCardinalityOwn(and(xi, yi));
    }

    /**
     * Bitwise OR (union) operation. The provided bitmaps are *not* modified.
     *
     * @param xi the first secure bitmap.
     * @param yi the other secure bitmap.
     * @return result of the operation.
     * @throws MpcAbortException if the protocol aborts.
     */
    default SecureBitmap or(final SecureBitmap xi, final SecureBitmap yi) throws MpcAbortException {
        return xor(xor(xi, yi), and(xi, yi));
    }

    /**
     * Bitwise NOT operation. The provided secure bitmaps are *not* modified.
     *
     * @param xi the secure bitmap.
     * @return result of the operation.
     * @throws MpcAbortException if the protocol aborts.
     */
    default SecureBitmap not(final SecureBitmap xi) throws MpcAbortException {
        return xor(xi, SecureBitmapFactory.createOnes(supportSecureBitmapType(), maxBitNum()));
    }
}
