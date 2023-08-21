package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Bitmap Operations.
 *
 * @author Li Peng
 * @date 2023/8/11
 */
public interface BitmapOperations {
    /**
     * And operation.
     *
     * @param x bitmap input.
     * @param y bitmap input.
     * @return result.
     * @throws MpcAbortException if the protocol aborts.
     */
    Bitmap and(Bitmap x, Bitmap y) throws MpcAbortException;
    /**
     * Or operation.
     * @param x bitmap input.
     * @param y bitmap input.
     * @return result.
     * @throws MpcAbortException if the protocol aborts.
     */
    Bitmap or(Bitmap x, Bitmap y) throws MpcAbortException;
    /**
     * Bitcount operation.
     * @param x bitmap input.
     * @return result.
     * @throws MpcAbortException if the protocol aborts.
     */
    int bitCount(Bitmap x) throws MpcAbortException;
}
