package edu.alibaba.mpc4j.s2pc.groupagg.view;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewSenderOutput;

public class PkFkViewSenderThread extends Thread {
    /**
     * join key
     */
    private final PkFkViewSender sender;
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
    private final int receiverSize;
    /**
     * output1
     */
    public PkFkViewSenderOutput senderOut1;
    /**
     * output2
     */
    public PkFkViewSenderOutput senderOut2;

    PkFkViewSenderThread(PkFkViewSender sender, byte[][] inputKey, BitVector[] inputPayload1, BitVector[] inputPayload2, int receiverSize) {
        assert inputKey.length == inputPayload1.length;
        assert inputKey.length == inputPayload2.length;
        this.sender = sender;
        this.inputKey = inputKey;
        this.inputPayload1 = inputPayload1;
        this.inputPayload2 = inputPayload2;
        this.receiverSize = receiverSize;
    }

    @Override
    public void run() {
        try {
            sender.init(inputPayload1[0].bitNum(), inputKey.length, receiverSize);
            senderOut1 = sender.generate(inputKey, inputPayload1, receiverSize);
            senderOut2 = sender.refresh(senderOut1, inputPayload2);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
