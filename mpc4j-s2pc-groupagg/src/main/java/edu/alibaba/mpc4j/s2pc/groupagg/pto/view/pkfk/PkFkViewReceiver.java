package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.ViewParty;

/**
 * view receiver, the party who holds the foreign key
 *
 * @author Feng Han
 * @date 2024/7/19
 */
public interface PkFkViewReceiver extends ViewParty {
    /**
     * init the protocol
     */
    void init(int payloadBitLen, int senderSize, int receiverSize) throws MpcAbortException;

    /**
     * generate the view
     *
     * @param key        the join key of database
     * @param payload    the payload of database
     * @param senderSize the data size of sender
     */
    PkFkViewReceiverOutput generate(byte[][] key, BitVector[] payload, int senderSize, int senderPayloadBitLen) throws MpcAbortException;

    /**
     * generate the view
     *
     * @param preView the pre-generated view
     * @param payload new payload
     */
    PkFkViewReceiverOutput refresh(PkFkViewReceiverOutput preView, BitVector[] payload) throws MpcAbortException;
}