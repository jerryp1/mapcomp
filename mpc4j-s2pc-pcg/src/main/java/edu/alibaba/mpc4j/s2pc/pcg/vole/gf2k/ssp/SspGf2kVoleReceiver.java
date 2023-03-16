package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.Gf2eVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.ssp.SspGf2eVoleReceiverOutput;

/**
 * Single single-point GF2K VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface SspGf2kVoleReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta  Δ.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol
     *
     * @param num num.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SspGf2eVoleReceiverOutput send(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num               num.
     * @param preReceiverOutput pre-computed receiver output.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BspCotSenderOutput send(int num, Gf2eVoleReceiverOutput preReceiverOutput) throws MpcAbortException;
}
