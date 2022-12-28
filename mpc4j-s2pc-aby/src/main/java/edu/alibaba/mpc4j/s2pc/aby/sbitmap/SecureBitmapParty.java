package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import org.roaringbitmap.RoaringBitmap;

/**
 * SecureBitMap party interface.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public interface SecureBitmapParty extends TwoPartyPto, SecurePto {
    /**
     * Returns the protocol type.
     *
     * @return the protocol type.
     */
    @Override
    SecureBitmapFactory.SecureBitMapPtoType getPtoType();

    /**
     * init the protocol.
     *
     * @param maxBitNum maximal number of bits stored in the SecureBitmap.
     * @param estimateAndNum estimated AND operation num. The protocol would do some computations if the actual AND
     *                       operation num is greater than estimated AND operation num.
     * @throws MpcAbortException if the protocol aborts.
     */
    void init(int maxBitNum, int estimateAndNum) throws MpcAbortException;

    /**
     * Share its own RoaringBitmap.
     *
     * @param roaringBitmap the RoaringBitmap to be shared.
     * @return the shared SecureBitmap.
     */
    SecureBitmap shareOwn(RoaringBitmap roaringBitmap);

    /**
     * Share other's RoaringBitmap.
     *
     * @return the shared SecureBitmap.
     * @throws MpcAbortException if the protocol aborts.
     */
    SecureBitmap shareOther() throws MpcAbortException;
}
