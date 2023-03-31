package edu.alibaba.mpc4j.s2pc.pso.opprf.bopprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

/**
 * Batch OPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
class BopprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final BopprfSender sender;
    /**
     * the input / output bit length
     */
    private final int l;
    /**
     * batch size
     */
    private final int batchSize;
    /**
     * point num
     */
    private final int pointNum;
    /**
     * sender input arrays
     */
    private final byte[][][] senderInputArrays;
    /**
     * sender target arrays
     */
    private final byte[][][] senderTargetArrays;

    BopprfSenderThread(BopprfSender sender, int l, byte[][][] senderInputArrays, byte[][][] senderTargetArrays) {
        this.sender = sender;
        this.l = l;
        batchSize = senderInputArrays.length;
        pointNum = Arrays.stream(senderInputArrays)
            .mapToInt(inputArray -> inputArray.length)
            .sum();
        this.senderInputArrays = senderInputArrays;
        this.senderTargetArrays = senderTargetArrays;
    }

    @Override
    public void run() {
        try {
            sender.init(batchSize, pointNum);
            sender.opprf(l, senderInputArrays, senderTargetArrays);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}