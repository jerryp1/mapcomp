package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Plain mux thread.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class PlainPayloadMuxSenderThread extends Thread {
    /**
     * the sender
     */
    private final PlainPayloadMuxParty sender;
    /**
     * x0
     */
    private final SquareZ2Vector x0;
    /**
     * y
     */
    private long[] y;
    /**
     * y
     */
    private BitVector[] yBinary;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private SquareZlVector z0;
    /**
     * z0
     */
    private SquareZ2Vector[] z0Binary;

    PlainPayloadMuxSenderThread(PlainPayloadMuxParty sender, SquareZ2Vector x0, long[] y) {
        this.sender = sender;
        this.x0 = x0;
        num = x0.getNum();
        this.y = y;
    }

    PlainPayloadMuxSenderThread(PlainPayloadMuxParty sender, SquareZ2Vector x0, BitVector[] y) {
        this.sender = sender;
        this.x0 = x0;
        num = x0.getNum();
        this.yBinary = y;
    }

    SquareZlVector getZ0() {
        return z0;
    }
    SquareZ2Vector[] getZ0Binary(){
        return z0Binary;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            if(y != null){
                z0 = sender.mux(x0, y, 64);
            }else{
                z0Binary = sender.muxB(x0, yBinary, yBinary.length);
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
