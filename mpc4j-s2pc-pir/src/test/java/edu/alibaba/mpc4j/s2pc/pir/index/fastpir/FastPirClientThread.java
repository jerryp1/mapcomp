package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirParams;

import java.nio.ByteBuffer;

/**
 * FastPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class FastPirClientThread extends Thread {
    /**
     * FastPIR client
     */
    private final Ayaa21SingleIndexPirClient client;
    /**
     * FastPIR params
     */
    private final Ayaa21SingleIndexPirParams indexPirParams;
    /**
     * element byte length
     */
    private final int elementByteLength;
    /**
     * retrieval index value
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

    FastPirClientThread(Ayaa21SingleIndexPirClient client, Ayaa21SingleIndexPirParams indexPirParams, int retrievalIndex,
                        int serverElementSize, int elementByteLength) {
        this.client = client;
        this.indexPirParams = indexPirParams;
        this.retrievalIndex = retrievalIndex;
        this.serverElementSize = serverElementSize;
        this.elementByteLength = elementByteLength;
    }

    public ByteBuffer getRetrievalResult() {
        return indexPirResult;
    }

    @Override
    public void run() {
        try {
            client.init(indexPirParams, serverElementSize, elementByteLength);
            client.getRpc().synchronize();
            indexPirResult = ByteBuffer.wrap(client.pir(retrievalIndex));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
