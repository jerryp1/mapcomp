package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * unbalanced batched OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public interface UbopprfReceiver extends TwoPartyPto {
    /**
     * init the protocol.
     *
     * @param batchSize batch size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int batchSize) throws MpcAbortException;

    /**
     * Gets OPPRF.
     *
     * @param l          the output bit length.
     * @param inputArray the batched input array.
     * @param targetNum  the number of programmed points.
     * @return the receiver outputs.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] opprf(int l, byte[][] inputArray, int targetNum) throws MpcAbortException;
}
