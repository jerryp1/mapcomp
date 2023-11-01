package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Vector;

/**
 * Shared permutation sender thread.
 *
 * @author Li Peng
 * @date 2023/10/25
 */
class SharedPermutationSenderThread extends Thread {
    /**
     * the sender
     */
    private final SharedPermutationParty sender;
    /**
     * perms
     */
    private final Vector<byte[]> perms0;
    /**
     * x
     */
    private final Vector<byte[]> x0;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private Vector<byte[]> z0;

    SharedPermutationSenderThread(SharedPermutationParty sender, Vector<byte[]> perms0, Vector<byte[]> x0) {
        this.sender = sender;
        this.perms0 = perms0;
        this.num = perms0.size();
        this.x0 = x0;
    }

    Vector<byte[]> getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            z0 = sender.permute(perms0, x0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
