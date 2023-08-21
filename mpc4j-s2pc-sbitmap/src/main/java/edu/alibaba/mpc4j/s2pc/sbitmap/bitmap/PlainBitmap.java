package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.Container;

/**
 * @author Li Peng
 * @date 2023/8/15
 */
public interface PlainBitmap extends Bitmap {

    PlainBitmap and(PlainBitmap other) throws MpcAbortException;

    PlainBitmap or(PlainBitmap other) throws MpcAbortException;

    int bitCount() throws MpcAbortException;

    PlainBitmap resizeBlock(int blockSize);

    /**
     * Return the containers of bitmap.
     * @return the containers of bitmap.
     */
    BitVector[] getContainers();

    /**
     * Return the containers of bitmap.
     * @return the containers of bitmap.
     */
    Container[] getContainer();

}
