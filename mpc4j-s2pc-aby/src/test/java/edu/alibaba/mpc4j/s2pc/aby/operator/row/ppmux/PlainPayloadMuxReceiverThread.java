package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Plain mux thread.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class PlainPayloadMuxReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PlainPayloadMuxParty receiver;
    /**
     * x1
     */
    private final SquareZ2Vector x1;
    /**
     * the num
     */
    private final int num;
    /**
     * z1
     */
    private SquareZlVector z1;
    /**
     * z0
     */
    private SquareZ2Vector[] z1Binary;

    private final boolean runBinary;
    private final int validBitLen;

    PlainPayloadMuxReceiverThread(PlainPayloadMuxParty receiver, SquareZ2Vector x1, int validBitLen, boolean runBinary) {
        this.receiver = receiver;
        this.x1 = x1;
        num = x1.getNum();
        this.runBinary = runBinary;
        this.validBitLen = validBitLen;
    }

    SquareZlVector getZ1() {
        return z1;
    }
    SquareZ2Vector[] getZ1Binary(){
        return z1Binary;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            if(!runBinary){
                z1 = receiver.mux(x1, null, validBitLen);
            }else{
                z1Binary = receiver.muxB(x1, null, validBitLen);
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
