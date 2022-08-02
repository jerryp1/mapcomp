package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * 非平衡PSI协议服务端（发送方）线程。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiServerThread extends Thread {
    /**
     * 非平衡PSI协议服务端
     */
    private final UpsiServer upsiServer;
    /**
     * 服务端集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 客户端（接收方）元素数量
     */
    private final int clientElementSize;

    UpsiServerThread(UpsiServer upsiServer, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.upsiServer = upsiServer;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            upsiServer.getRpc().connect();
            upsiServer.init();
            upsiServer.psi(serverElementSet, clientElementSize);
            upsiServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
