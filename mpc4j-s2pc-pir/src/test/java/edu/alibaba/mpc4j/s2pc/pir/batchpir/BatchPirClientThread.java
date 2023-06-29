package edu.alibaba.mpc4j.s2pc.pir.batchpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * batch PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class BatchPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPirClientThread.class);
    /**
     * batch index PIR client
     */
    private final BatchIndexPirClient client;
    /**
     * retrieval index list
     */
    private final List<Integer> retrievalIndexList;
    /**
     * retrieval result
     */
    private Map<Integer, byte[]> retrievalResult;
    /**
     * element bit length
     */
    private final int elementBitLength;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * max retrieval size
     */
    private final int maxRetrievalSize;

    BatchPirClientThread(BatchIndexPirClient client, List<Integer> retrievalIndexList, int elementBitLength,
                         int serverElementSize, int maxRetrievalSize) {
        this.client = client;
        this.retrievalIndexList = retrievalIndexList;
        this.retrievalResult = new HashMap<>(retrievalIndexList.size());
        this.elementBitLength = elementBitLength;
        this.serverElementSize = serverElementSize;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    public Map<Integer, byte[]> getRetrievalResult() {
        return retrievalResult;
    }

    @Override
    public void run() {
        try {
            client.init(serverElementSize, elementBitLength, maxRetrievalSize);
            LOGGER.info(
                "Client: Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();
            retrievalResult = client.pir(retrievalIndexList);
            LOGGER.info(
                "Client: Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}