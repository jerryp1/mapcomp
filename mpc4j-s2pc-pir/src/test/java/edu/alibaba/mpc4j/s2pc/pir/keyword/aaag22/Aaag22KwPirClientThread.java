package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirParams;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AAAG22 keyword PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
public class Aaag22KwPirClientThread<T> extends Thread {
    /**
     * AAAG22 keyword PIR client
     */
    private final Aaag22KwPirClient<T> client;
    /**
     * AAAG22 keyword PIR params
     */
    private final Aaag22KwPirParams kwPirParams;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * retrieval sets
     */
    private final List<Set<T>> retrievalSets;
    /**
     * repeat time
     */
    private final int repeatTime;
    /**
     * retrieval result
     */
    private final List<Map<T, ByteBuffer>> retrievalResults;

    Aaag22KwPirClientThread(Aaag22KwPirClient<T> client, Aaag22KwPirParams kwPirParams, List<Set<T>> retrievalSets,
                            int labelByteLength) {
        this.client = client;
        this.kwPirParams = kwPirParams;
        this.retrievalSets = retrievalSets;
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
            client.init(kwPirParams, labelByteLength);
            client.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                retrievalResults.add(client.pir(retrievalSets.get(i)));
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}