package edu.alibaba.mpc4j.s2pc.pir.index.simplepir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SingleIndexPirParams;

import java.nio.ByteBuffer;

/**
 * Simple PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/5/31
 */
public class SimplePirClientThread extends Thread {
    /**
     * Simple PIR client
     */
    private final Hhcm23SingleIndexPirClient client;
    /**
     * Simple PIR params
     */
    private final Hhcm23SingleIndexPirParams indexPirParams;
    /**
     * element bit length
     */
    private final int elementBitLength;
    /**
     * retrieval index
     */
    private final int retrievalIndex;
    /**
     * database size
     */
    private final int serverElementSize;
    /**
     * retrieval result
     */
    private ByteBuffer indexPirResult;

    SimplePirClientThread(Hhcm23SingleIndexPirClient client, Hhcm23SingleIndexPirParams indexPirParams,
                          int retrievalIndex, int serverElementSize, int elementBitLength) {
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
