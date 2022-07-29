package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.math.BigInteger;

/**
 * ZP_VOLE协议发送方线程。
 *
 * @author Hanwen Feng
 * @date 2022/06/10
 */
class ZpVoleReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final ZpVoleReceiver receiver;
    /**
     * 素数域
     */
    private final BigInteger prime;
    /**
     * 关联值Δ
     */
    private final BigInteger delta;
    /**
     * 数量
     */
    private final int num;
    /**
     * 接收方输出
     */
    private ZpVoleReceiverOutput receiverOutput;

    ZpVoleReceiverThread(ZpVoleReceiver receiver, BigInteger prime, BigInteger delta, int num) {
        this.receiver = receiver;
        this.prime = prime;
        this.delta = delta;
        this.num = num;
    }

    ZpVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(prime, delta, num);
            receiverOutput = receiver.receive(num);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
