package edu.alibaba.mpc4j.s2pc.pir.batchpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirServer;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 批量索引PIR协议服务端线程。
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class BatchPirServerThread extends Thread {
    /**
     * 服务端
     */
    private final BatchIndexPirServer server;
    /**
     * 服务端元素列表
     */
    private final ArrayList<ByteBuffer> serverElementList;
    /**
     * 元素比特长度
     */
    private final int elementBitLength;

    private final int maxRetrievalSize;

    BatchPirServerThread(BatchIndexPirServer server, ArrayList<ByteBuffer> serverElementList, int elementBitLength,
                         int maxRetrievalSize) {
        this.server = server;
        this.serverElementList = serverElementList;
        this.elementBitLength = elementBitLength;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementList, elementBitLength, maxRetrievalSize);
            server.getRpc().synchronize();
            server.pir();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}