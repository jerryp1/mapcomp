package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * CMG21 keyword PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class Cmg21KwPirServerThread<T> extends Thread {
    /**
     * CMG21 keyword PIR server
     */
    private final Cmg21KwPirServer<T> server;
    /**
     * CMG21 keyword PIR params
     */
    private final Cmg21KwPirParams kwPirParams;
    /**
     * keyword label map
     */
    private final Map<T, ByteBuffer> keywordLabelMap;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * repeat time
     */
    private final int repeatTime;

    Cmg21KwPirServerThread(Cmg21KwPirServer<T> server, Cmg21KwPirParams kwPirParams, Map<T, ByteBuffer> keywordLabelMap,
                           int labelByteLength, int repeatTime) {
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
            server.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                server.pir();
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}