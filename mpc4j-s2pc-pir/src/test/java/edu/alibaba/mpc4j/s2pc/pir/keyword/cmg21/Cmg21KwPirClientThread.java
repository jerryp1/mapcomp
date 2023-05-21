package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

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
public class Cmg21KwPirClientThread<T> extends Thread {
    /**
     * CMG21 keyword PIR client
     */
    private final Cmg21KwPirClient<T> client;
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
    private final List<Set<T>> retrievalSets;
    /**
     * repeat time
     */
    private final int repeatTime;
    /**
     * retrieval result
     */
    private final List<Map<T, ByteBuffer>> retrievalResults;

    Cmg21KwPirClientThread(Cmg21KwPirClient<T> client, Cmg21KwPirParams kwPirParams, List<Set<T>> retrievalSets,
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