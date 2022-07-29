package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * 非平衡PSI协议发送方线程。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiServerThread extends Thread {
    /**
     * 非平衡PSI协议发送方
     */
    private final UpsiServer upsiServer;
    /**
     * 发送方集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 接收方元素数量
     */
    private final int clientElementSize;
    /**
     * 元素字节长度
     */
    private final int elementByteLength;

    UpsiServerThread(UpsiServer upsiServer, Set<ByteBuffer> serverElementSet, int clientElementSize,
                    int elementByteLength) {
        this.upsiServer = upsiServer;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
        this.elementByteLength = elementByteLength;
    }

    @Override
    public void run() {
        try {
            upsiServer.getRpc().connect();
            upsiServer.init();
            upsiServer.psi(serverElementSet, clientElementSize, elementByteLength);
            upsiServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
