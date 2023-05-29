package edu.alibaba.mpc4j.s2pc.pir.index.mulpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirParams;

import java.nio.ByteBuffer;

/**
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class MulPirClientThread extends Thread {
    /**
     * Mul PIR client
     */
    private final Alpr21SingleIndexPirClient client;
    /**
     * Mul PIR params
     */
    private final Alpr21SingleIndexPirParams indexPirParams;
    /**
     * element byte length
     */
    private final int elementByteLength;
    /**
     * retrieval index
     */
    private final int retrievalSingleIndex;
    /**
     * database size
     */
    private final int serverElementSize;
    /**
     * retrieval result
     */
    private ByteBuffer indexPirResult;

    MulPirClientThread(Alpr21SingleIndexPirClient client, Alpr21SingleIndexPirParams indexPirParams, int retrievalSingleIndex,
                       int serverElementSize, int elementByteLength) {
        this.client = client;
        this.indexPirParams = indexPirParams;
        this.retrievalSingleIndex = retrievalSingleIndex;
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
            indexPirResult = ByteBuffer.wrap(client.pir(retrievalSingleIndex));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
