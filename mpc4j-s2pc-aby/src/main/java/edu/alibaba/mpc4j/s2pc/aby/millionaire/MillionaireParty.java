package edu.alibaba.mpc4j.s2pc.aby.millionaire;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

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
     * @param maxBitNum max bit num.
     */
    void init(int maxBitNum) throws MpcAbortException;
}
