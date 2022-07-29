package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.math.BigInteger;

/**
 * ZP-VOLE协议接收方线程。
 *
 * @author Hanwen Feng
 * @date 2022/06/10
 */
class ZpVoleSenderThread extends Thread {
    /**
     * 接收方
     */
    private final ZpVoleSender sender;
    /**
     * 素数p
     */
    private final BigInteger prime;
    /**
     * x
     */
    private final BigInteger[] x;
    /**
     * 接收方输出
     */
    private ZpVoleSenderOutput senderOutput;

    ZpVoleSenderThread(ZpVoleSender sender, BigInteger prime, BigInteger[] x) {
        this.sender = sender;
        this.prime = prime;
        this.x = x;
    }

    ZpVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(prime, x.length);
            senderOutput = sender.send(x);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
