package edu.alibaba.mpc4j.s2pc.aby.millionaire;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;

/**
 * Millionaire Protocol Party.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public interface MillionaireParty extends TwoPartyPto {
    /**
     * init protocol.
     *
     * @param l         input value bit length.
     * @param maxBitNum max bit num.
     */
    void init(int l, int maxBitNum) throws MpcAbortException;

    /**
     * less than.
     *
     * @param inputs input value.
     * @return result.
     */
    BitVector lt(ZlVector inputs) throws MpcAbortException;
}
