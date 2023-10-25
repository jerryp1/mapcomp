package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.List;
import java.util.Vector;

/**
 * Shuffle receiver thread.
 *
 * @author Li Peng
 * @date 2023/10/22
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
     * the num
     */
    private final int num;
    /**
     * l
     */
    private final int l;
    /**
     * z1
     */
    private List<Vector<byte[]>> z1;

    ShuffleReceiverThread(ShuffleParty receiver, List<Vector<byte[]>> x1, int l) {
        this.receiver = receiver;
        this.x1 = x1;
        this.num = x1.get(0).size();
        this.l = l;
    }

    List<Vector<byte[]>> getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(l, num);
            z1 = receiver.shuffle(x1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
