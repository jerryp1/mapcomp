package edu.alibaba.mpc4j.s2pc.pir.batchpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 批量索引PIR协议服务端线程。
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class BatchPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPirServerThread.class);
    /**
     * 服务端
     */
    private final BatchIndexPirServer server;
    /**
     * 服务端元素列表
     */
    private final byte[][] serverElementArray;
    /**
     * 元素比特长度
     */
    private final int elementBitLength;
    /**
     * 支持的最大查询数目
     */
    private final int maxRetrievalSize;

    BatchPirServerThread(BatchIndexPirServer server, byte[][] serverElementArray, int elementBitLength,
                         int maxRetrievalSize) {
        this.server = server;
        this.serverElementArray = serverElementArray;
        this.elementBitLength = elementBitLength;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementArray, elementBitLength, maxRetrievalSize);
            LOGGER.info("Server: Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
            server.getRpc().synchronize();
            server.getRpc().reset();
            server.pir();
            LOGGER.info("Server: Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}