package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Vector;

/**
 * Permutation receiver thread.
 *
 */
class SharedPermutationReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final SharedPermutationParty receiver;
    /**
     * perms
     */
    private final Vector<byte[]> perms1;
    /**
     * x
     */
    private final Vector<byte[]> x1;
    /**
     * the num
     */
    private final int num;
    /**
     * z1
     */
    private Vector<byte[]> z1;

    SharedPermutationReceiverThread(SharedPermutationParty receiver, Vector<byte[]> perms1, Vector<byte[]> x1) {
        this.receiver = receiver;
        this.perms1 = perms1;
        this.num = perms1.size();
        this.x1 = x1;
    }

    Vector<byte[]> getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            z1 = receiver.permute(perms1, x1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
