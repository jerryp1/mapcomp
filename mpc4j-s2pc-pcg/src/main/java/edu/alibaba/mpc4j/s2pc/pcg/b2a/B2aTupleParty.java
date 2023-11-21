package edu.alibaba.mpc4j.s2pc.pcg.b2a;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * B2a tuple generation party.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public interface B2aTupleParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param updateNum update num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int updateNum) throws MpcAbortException;

    /**
     * executes the protocol.
     *
     * @param num num.
     * @return party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    B2aTuple generate(int num) throws MpcAbortException;
}