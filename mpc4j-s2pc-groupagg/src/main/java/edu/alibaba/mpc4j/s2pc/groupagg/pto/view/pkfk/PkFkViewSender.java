package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.ViewParty;

/**
 * view receiver, the party who holds the primary key
 *
 * @author Feng Han
 * @date 2024/7/19
 */
public interface PkFkViewSender extends ViewParty {
    /**
     * init the protocol
     */
    void init(int bitNum, int senderSize, int receiverSize) throws MpcAbortException;
    /**
     * generate the view
     * @param key the join key of database
     * @param payload the payload of database
     * @param receiverSize the table size of the receiver
     */
    PkFkViewSenderOutput generate(byte[][] key, BitVector[] payload, int receiverSize) throws MpcAbortException;
    /**
     * generate the view
     * @param preView the pre-generated view
     * @param receiverSize the table size of the receiver
     */
    PkFkViewSenderOutput refresh(PkFkViewSenderOutput preView, int receiverSize) throws MpcAbortException;
}