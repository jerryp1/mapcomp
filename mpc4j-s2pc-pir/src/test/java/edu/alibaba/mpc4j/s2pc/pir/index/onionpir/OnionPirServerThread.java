package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * OnionPIR协议服务端线程。
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class OnionPirServerThread extends Thread {
    /**
     * OnionPIR协议服务端
     */
    private final Mcr21IndexPirServer server;
    /**
     * OnionPIR参数
     */
    private final Mcr21IndexPirParams indexPirParams;
    /**
     * 服务端元素数组
     */
    private final ArrayList<ByteBuffer> elementArrayList;
    /**
     * 元素字节长度
     */
    private final int elementByteLength;
    /**
     * 重复次数
     */
    private final int repeatTime;

    OnionPirServerThread(Mcr21IndexPirServer server, Mcr21IndexPirParams indexPirParams,
                         ArrayList<ByteBuffer> elementArrayList, int elementByteLength, int repeatTime) {
        this.server = server;
        this.indexPirParams = indexPirParams;
        this.elementArrayList = elementArrayList;
        this.elementByteLength = elementByteLength;
        this.repeatTime = repeatTime;
    }

    @Override
    public void run() {
        try {
            server.init(indexPirParams, elementArrayList, elementByteLength);
            server.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                server.pir();
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
