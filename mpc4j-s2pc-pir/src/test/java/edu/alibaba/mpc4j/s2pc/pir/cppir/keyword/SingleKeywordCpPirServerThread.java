package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Single Keyword Client-specific Preprocessing PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
class SingleKeywordCpPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleKeywordCpPirServerThread.class);
    /**
     * server
     */
    private final SingleKeywordCpPirServer server;
    /**
     * keyword label map
     */
    private final Map<ByteBuffer, ByteBuffer> keywordLabelMap;
    /**
     * label bit length
     */
    private final int labelBitLength;
    /**
     * query num
     */
    private final int queryNum;

    SingleKeywordCpPirServerThread(SingleKeywordCpPirServer server, Map<ByteBuffer, ByteBuffer> keywordLabelMap,
                                   int labelBitLength, int queryNum) {
        this.server = server;
        this.keywordLabelMap = keywordLabelMap;
        this.labelBitLength = labelBitLength;
        this.queryNum = queryNum;
    }

    @Override
    public void run() {
        try {
            server.init(keywordLabelMap, labelBitLength);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            for (int i = 0; i < queryNum; i++) {
                server.pir();
            }
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
