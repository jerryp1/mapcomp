package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirServer;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Vectorized PIR协议服务端线程。
 *
 * @author Liqiang Peng
 * @date 2023/3/24
 */
public class VectorizedPirServerThread extends Thread {
    /**
     * Vectorized PIR协议服务端
     */
    private final Mr23IndexPirServer server;
    /**
     * Vectorized PIR参数
     */
    private final Mr23IndexPirParams indexPirParams;
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

    VectorizedPirServerThread(Mr23IndexPirServer server, Mr23IndexPirParams indexPirParams,
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
