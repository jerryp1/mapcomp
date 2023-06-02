package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirParams;

import java.nio.ByteBuffer;

/**
 * OnionPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class OnionPirClientThread extends Thread {
    /**
     * OnionPIR client
     */
    private final Mcr21SingleIndexPirClient client;
    /**
     * OnionPIR params
     */
    private final Mcr21SingleIndexPirParams indexPirParams;
    /**
     * element bit length
     */
    private final int elementBitLength;
    /**
     * retrieval index
     */
    private final int retrievalIndex;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * retrieval result
     */
    private ByteBuffer indexPirResult;

    OnionPirClientThread(Mcr21SingleIndexPirClient client, Mcr21SingleIndexPirParams indexPirParams, int retrievalIndex,
                         int serverElementSize, int elementBitLength) {
        this.client = client;
        this.indexPirParams = indexPirParams;
        this.retrievalIndex = retrievalIndex;
        this.serverElementSize = serverElementSize;
        this.elementBitLength = elementBitLength;
    }

    public ByteBuffer getRetrievalResult() {
        return indexPirResult;
    }

    @Override
    public void run() {
        try {
            client.init(indexPirParams, serverElementSize, elementBitLength);
            client.getRpc().synchronize();
            indexPirResult = ByteBuffer.wrap(client.pir(retrievalIndex));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
