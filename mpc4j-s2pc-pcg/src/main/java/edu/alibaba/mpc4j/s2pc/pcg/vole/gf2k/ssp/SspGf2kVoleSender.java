package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.ssp.SspGf2eVoleSenderOutput;

/**
 * Single single-point GF2K VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface SspGf2kVoleSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxNum num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha α.
     * @param num   num.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SspGf2eVoleSenderOutput receive(int alpha, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha           α.
     * @param num             num.
     * @param preSenderOutput pre-computed sender output.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SspGf2eVoleSenderOutput receive(int alpha, int num, SspGf2eVoleSenderOutput preSenderOutput) throws MpcAbortException;
}
