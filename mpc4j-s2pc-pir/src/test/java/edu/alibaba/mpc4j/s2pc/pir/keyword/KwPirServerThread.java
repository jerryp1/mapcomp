package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * keyword PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirServerThread<T> extends Thread {
    /**
     * keyword PIR server
     */
    private final KwPirServer<T> server;
    /**
     * keyword label map
     */
    private final Map<T, ByteBuffer> keywordLabelMap;
    /**
     * retrieval size
     */
    private final int retrievalSize;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * repeat time
     */
    private final int repeatTime;

    KwPirServerThread(KwPirServer<T> server, Map<T, ByteBuffer> keywordLabelMap, int retrievalSize, int labelByteLength,
                      int repeatTime) {
        this.server = server;
        this.keywordLabelMap = keywordLabelMap;
        this.retrievalSize = retrievalSize;
        this.labelByteLength = labelByteLength;
        this.repeatTime = repeatTime;
    }

    @Override
    public void run() {
        try {
            server.init(keywordLabelMap, retrievalSize, labelByteLength);
            server.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                server.pir();
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}