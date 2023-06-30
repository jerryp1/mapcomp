package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CMG21 keyword PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class Cmg21KwPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cmg21KwPirClientThread.class);
    /**
     * CMG21 keyword PIR client
     */
    private final Cmg21KwPirClient client;
    /**
     * CMG21 keyword PIR params
     */
    private final Cmg21KwPirParams kwPirParams;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * retrieval sets
     */
    private final List<Set<ByteBuffer>> retrievalSets;
    /**
     * repeat time
     */
    private final int repeatTime;
    /**
     * retrieval result
     */
    private final List<Map<ByteBuffer, ByteBuffer>> retrievalResults;
    /**
     * server element size
     */
    private final int serverElementSize;

    Cmg21KwPirClientThread(Cmg21KwPirClient client, Cmg21KwPirParams kwPirParams, List<Set<ByteBuffer>> retrievalSets,
                           int serverElementSize, int labelByteLength) {
        this.client = client;
        this.kwPirParams = kwPirParams;
        this.retrievalSets = retrievalSets;
        this.serverElementSize = serverElementSize;
        this.labelByteLength = labelByteLength;
        repeatTime = retrievalSets.size();
        retrievalResults = new ArrayList<>(repeatTime);
    }

    public Map<ByteBuffer, ByteBuffer> getRetrievalResult(int index) {
        return retrievalResults.get(index);
    }

    @Override
    public void run() {
        try {
            client.init(kwPirParams, serverElementSize, labelByteLength);
            LOGGER.info("Client: The Offline Communication costs {}MB",
                client.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
            client.getRpc().reset();
            client.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                retrievalResults.add(client.pir(retrievalSets.get(i)));
            }
            LOGGER.info("Client: The Online Communication costs {}MB",
                client.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}