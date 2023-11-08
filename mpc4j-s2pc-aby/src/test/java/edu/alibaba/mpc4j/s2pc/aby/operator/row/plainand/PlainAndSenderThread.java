package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Plain and thread.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
class PlainAndSenderThread extends Thread {
    /**
     * the sender
     */
    private final PlainAndParty sender;
    /**
     * x0
     */
    private final BitVector x0;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private SquareZ2Vector z0;

    PlainAndSenderThread(PlainAndParty sender, BitVector x0) {
        this.sender = sender;
        this.x0 = x0;
        num = x0.bitNum();
    }

    SquareZ2Vector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            z0 = sender.and(x0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
