package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.ViewParty;

/**
 * view receiver, the party who holds the primary key
 *
 */
public interface PkFkViewSender extends ViewParty {
    /**
     * init the protocol
     */
    void init(int payloadBitLen, int senderSize, int receiverSize) throws MpcAbortException;

    /**
     * generate the view
     *
     * @param key          the join key of database
     * @param payload      the payload of database
     * @param receiverSize the table size of the receiver
     */
    PkFkViewSenderOutput generate(byte[][] key, BitVector[] payload, int receiverSize) throws MpcAbortException;

    /**
     * generate the view
     *
     * @param preView the pre-generated view
     * @param payload new payload
     */
    PkFkViewSenderOutput refresh(PkFkViewSenderOutput preView, BitVector[] payload) throws MpcAbortException;
}