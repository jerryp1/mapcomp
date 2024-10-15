package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.List;
import java.util.Vector;

/**
 * Shuffle receiver thread.
 *
 */
class ShuffleReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final ShuffleParty receiver;
    /**
     * x1
     */
    private final List<Vector<byte[]>> x1;
    /**
     * random local permutation
     */
    private final int[] randomPerms;
    /**
     * the num
     */
    private final int num;
    /**
     * z1
     */
    private List<Vector<byte[]>> z1;

    ShuffleReceiverThread(ShuffleParty receiver, List<Vector<byte[]>> x1, int[] randomPerms) {
        this.receiver = receiver;
        this.x1 = x1;
        this.num = x1.get(0).size();
        this.randomPerms = randomPerms;
    }

    List<Vector<byte[]>> getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            z1 = receiver.shuffle(x1, randomPerms);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
