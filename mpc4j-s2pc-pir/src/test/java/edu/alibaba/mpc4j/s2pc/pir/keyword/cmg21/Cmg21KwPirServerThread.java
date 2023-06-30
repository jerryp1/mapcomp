package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * CMG21 keyword PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class Cmg21KwPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cmg21KwPirServerThread.class);
    /**
     * CMG21 keyword PIR server
     */
    private final Cmg21KwPirServer server;
    /**
     * CMG21 keyword PIR params
     */
    private final Cmg21KwPirParams kwPirParams;
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

    Cmg21KwPirServerThread(Cmg21KwPirServer server, Cmg21KwPirParams kwPirParams,
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