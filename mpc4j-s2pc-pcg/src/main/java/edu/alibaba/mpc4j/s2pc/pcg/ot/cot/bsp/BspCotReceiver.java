package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Batched single-point COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface BspCotReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxBatchNum max batch num.
     * @param maxNum      max num for each SSP-COT.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxBatchNum, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alphaArray α array.
     * @param num        num for each SSP-COT.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BspCotReceiverOutput receive(int[] alphaArray, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray        α array.
     * @param num               num for each SSP-COT.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BspCotReceiverOutput receive(int[] alphaArray, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}
