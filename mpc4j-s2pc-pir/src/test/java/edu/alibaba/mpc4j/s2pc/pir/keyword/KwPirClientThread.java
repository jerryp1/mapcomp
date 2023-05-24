package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * keyword PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirClientThread<T> extends Thread {
    /**
     * keyword PIR client
     */
    private final KwPirClient<T> client;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * retrieval sets
     */
    private final List<Set<T>> retrievalSets;
    /**
     * retrieval size
     */
    private final int retrievalSize;
    /**
     * repeat time
     */
    private final int repeatTime;
    /**
     * retrieval result
     */
    private final List<Map<T, ByteBuffer>> retrievalResults;

    KwPirClientThread(KwPirClient<T> client, List<Set<T>> retrievalSets, int retrievalSize, int labelByteLength) {
        this.client = client;
        this.retrievalSets = retrievalSets;
        this.retrievalSize = retrievalSize;
        this.labelByteLength = labelByteLength;
        repeatTime = retrievalSets.size();
        retrievalResults = new ArrayList<>(repeatTime);
    }

    public Map<T, ByteBuffer> getRetrievalResult(int index) {
        return retrievalResults.get(index);
    }

    @Override
    public void run() {
        try {
            client.init(retrievalSize, labelByteLength);
            client.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                retrievalResults.add(client.pir(retrievalSets.get(i)));
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}