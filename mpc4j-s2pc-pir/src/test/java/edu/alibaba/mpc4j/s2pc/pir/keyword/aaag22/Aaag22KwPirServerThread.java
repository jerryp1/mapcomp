package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * AAAG22 keyword PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
public class Aaag22KwPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aaag22KwPirServerThread.class);
    /**
     * AAAG22 keyword PIR server
     */
    private final Aaag22KwPirServer server;
    /**
     * AAAG22 keyword PIR params
     */
    private final Aaag22KwPirParams kwPirParams;
    /**
     * keyword label map
     */
    private final Map<ByteBuffer, ByteBuffer> keywordLabelMap;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * repeat time
     */
    private final int repeatTime;

    Aaag22KwPirServerThread(Aaag22KwPirServer server, Aaag22KwPirParams kwPirParams,
                            Map<ByteBuffer, ByteBuffer> keywordLabelMap, int labelByteLength, int repeatTime) {
        this.server = server;
        this.kwPirParams = kwPirParams;
        this.keywordLabelMap = keywordLabelMap;
        this.labelByteLength = labelByteLength;
        this.repeatTime = repeatTime;
    }

    @Override
    public void run() {
        try {
            server.init(kwPirParams, keywordLabelMap, labelByteLength);
            LOGGER.info("Server: The Offline Communication costs {}MB",
                server.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
            server.getRpc().reset();
            server.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                server.pir();
            }
            LOGGER.info("Server: The Online Communication costs {}MB",
                server.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}