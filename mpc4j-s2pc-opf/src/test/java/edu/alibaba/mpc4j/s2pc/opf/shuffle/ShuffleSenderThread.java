package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.List;
import java.util.Vector;

/**
 * Shuffle sender thread.
 */
class ShuffleSenderThread extends Thread {
    /**
     * the sender
     */
    private final ShuffleParty sender;
    /**
     * x0
     */
    private final List<Vector<byte[]>> x0;
    /**
     * random local permutation
     */
    private final int[] randomPerms;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private List<Vector<byte[]>> z0;

    ShuffleSenderThread(ShuffleParty sender, List<Vector<byte[]>> x0, int[] randomPerms) {
        this.sender = sender;
        this.x0 = x0;
        this.num = x0.get(0).size();
        this.randomPerms = randomPerms;
    }

    List<Vector<byte[]>> getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            z0 = sender.shuffle(x0, randomPerms);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
