package edu.alibaba.mpc4j.s2pc.groupagg.view;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiverOutput;

public class PkFkViewReceiverThread extends Thread {
    /**
     * join key
     */
    private final PkFkViewReceiver receiver;
    /**
     * join key
     */
    private final byte[][] inputKey;
    /**
     * payload1 in rows
     */
    private final BitVector[] inputPayload1;
    /**
     * payload1 in rows
     */
    private final BitVector[] inputPayload2;
    /**
     * sender's input size
     */
    private final int senderSize;
    /**
     * bit length of the sender's payload
     */
    private final int senderPayloadBitLen;
    /**
     * output1
     */
    public PkFkViewReceiverOutput receiverOut1;
    /**
     * output2
     */
    public PkFkViewReceiverOutput receiverOut2;

    PkFkViewReceiverThread(PkFkViewReceiver receiver, byte[][] inputKey, BitVector[] inputPayload1, BitVector[] inputPayload2, int senderSize, int senderPayloadBitLen) {
        assert inputKey.length == inputPayload1.length;
        assert inputKey.length == inputPayload2.length;
        this.receiver = receiver;
        this.inputKey = inputKey;
        this.inputPayload1 = inputPayload1;
        this.inputPayload2 = inputPayload2;
        this.senderSize = senderSize;
        this.senderPayloadBitLen = senderPayloadBitLen;
    }

    @Override
    public void run() {
        try {
            receiver.init(senderPayloadBitLen, senderSize, inputKey.length);
            receiverOut1 = receiver.generate(inputKey, inputPayload1, senderSize, senderPayloadBitLen);
            receiverOut2 = receiver.refresh(receiverOut1, inputPayload2);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
