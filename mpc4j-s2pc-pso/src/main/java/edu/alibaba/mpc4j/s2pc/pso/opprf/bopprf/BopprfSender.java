package edu.alibaba.mpc4j.s2pc.pso.opprf.bopprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public interface BopprfSender extends TwoPartyPto {
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
     * @param l            the output bit length.
     * @param inputArrays  the batched input arrays.
     * @param targetArrays the batched target programmed arrays.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void opprf(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException;
}
