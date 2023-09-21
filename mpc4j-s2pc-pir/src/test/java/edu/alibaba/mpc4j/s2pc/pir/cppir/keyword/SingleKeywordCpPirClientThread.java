package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single Keyword Client-specific Preprocessing PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
class SingleKeywordCpPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleKeywordCpPirClientThread.class);
    /**
     * client
     */
    private final SingleKeywordCpPirClient client;
    /**
     * database size
     */
    private final int n;
    /**
     * value bit length
     */
    private final int l;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * retrieval result
     */
    private final Map<ByteBuffer, ByteBuffer> retrievalResult;
    /**
     * retrieval list
     */
    private final List<ByteBuffer> retrievalList;

    SingleKeywordCpPirClientThread(SingleKeywordCpPirClient client, int n, int l, List<ByteBuffer> retrievalList) {
        this.client = client;
        this.n = n;
        this.l = l;
        this.retrievalList = retrievalList;
        this.queryNum = retrievalList.size();
        retrievalResult = new HashMap<>(queryNum);
    }

    public Map<ByteBuffer, ByteBuffer> getRetrievalResult() {
        return retrievalResult;
    }

    @Override
    public void run() {
        try {
            client.init(n, l);
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            for (int i = 0; i < queryNum; i++) {
                ByteBuffer value = client.pir(retrievalList.get(i));
                if (!(value == null)) {
                    retrievalResult.put(retrievalList.get(i), value);
                }
            }
            LOGGER.info(
                "Client: The Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
