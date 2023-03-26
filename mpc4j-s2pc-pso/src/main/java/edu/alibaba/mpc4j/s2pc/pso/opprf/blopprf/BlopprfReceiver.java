package edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Batched l-bit-input OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public interface BlopprfReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxBatchSize the max batch size.
     * @param maxPointNum  the max point num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxBatchSize, int maxPointNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param l          the input / output bit length.
     * @param inputArray the batched input array.
     * @param targetNum  the number of programmed points.
     * @return the receiver outputs.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] opprf(int l, byte[][] inputArray, int targetNum) throws MpcAbortException;
}
