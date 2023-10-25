package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.List;
import java.util.Vector;

/**
 * Shuffle sender thread.
 *
 * @author Li Peng
 * @date 2023/10/22
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
     * the num
     */
    private final int num;
    /**
     * l
     */
    private final int l;
    /**
     * z0
     */
    private List<Vector<byte[]>> z0;

    ShuffleSenderThread(ShuffleParty sender, List<Vector<byte[]>> x0, int l) {
        this.sender = sender;
        this.x0 = x0;
        this.num = x0.get(0).size();
        this.l = l;
    }

    List<Vector<byte[]>> getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(l, num);
            z0 = sender.shuffle(x0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
