package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * Batched single-point GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public interface Gf2kBspVoleReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta       Δ.
     * @param maxBatchNum max batch num.
     * @param maxNum      max num for each GF2K-SSP-VOLE.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int maxBatchNum, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum batch num.
     * @param num      num for each GF2K-SSP-VOLE.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVoleReceiverOutput send(int batchNum, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum          batch num.
     * @param num               num for each GF2K-SSP-VOLE.
     * @param preReceiverOutput pre-computed receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVoleReceiverOutput send(int batchNum, int num, Gf2kVoleReceiverOutput preReceiverOutput)
        throws MpcAbortException;
}
