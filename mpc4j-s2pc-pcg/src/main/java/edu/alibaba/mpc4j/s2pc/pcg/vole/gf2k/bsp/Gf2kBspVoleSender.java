package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * Batched single-point GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public interface Gf2kBspVoleSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxBatchNum max batch num.
     * @param maxNum      max num for each GF2K-SSP-VOLE.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxBatchNum, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alphaArray α array.
     * @param num        num for each GF2K-SSP-VOLE.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVoleSenderOutput send(int[] alphaArray, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alphaArray      α array.
     * @param num             num for each GF2K-SSP-VOLE.
     * @param preSenderOutput pre-computed GF2K-VOLE sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVoleSenderOutput receive(int[] alphaArray, int num, Gf2kVoleSenderOutput preSenderOutput) throws MpcAbortException;
}