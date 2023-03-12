package edu.alibaba.mpc4j.s2pc.pir.batchpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirClient;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 批量索引PIR协议客户端线程。
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class BatchPirClientThread extends Thread {
    /**
     * 批量索引PIR协议客户端
     */
    private final BatchIndexPirClient client;
    /**
     * 客户端集合
     */
    private final ArrayList<Integer> retrievalIndicesArray;
    /**
     * PIR结果
     */
    private Map<Integer, ByteBuffer> retrievalResults;
    /**
     * 元素比特长度
     */
    private final int elementBitLength;
    /**
     * 服务端元素数量
     */
    private final int serverElementSize;

    private final int maxRetrievalSize;

    BatchPirClientThread(BatchIndexPirClient client, ArrayList<Integer> retrievalIndicesArray, int elementBitLength,
                         int serverElementSize, int maxRetrievalSize) {
        this.client = client;
        this.retrievalIndicesArray = retrievalIndicesArray;
        retrievalResults = new HashMap<>(retrievalIndicesArray.size());
        this.elementBitLength = elementBitLength;
        this.serverElementSize = serverElementSize;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    public Map<Integer, ByteBuffer> getRetrievalResult() {
        return retrievalResults;
    }

    @Override
    public void run() {
        try {
            client.init(serverElementSize, elementBitLength, maxRetrievalSize);
            client.getRpc().synchronize();
            retrievalResults = client.pir(retrievalIndicesArray);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}