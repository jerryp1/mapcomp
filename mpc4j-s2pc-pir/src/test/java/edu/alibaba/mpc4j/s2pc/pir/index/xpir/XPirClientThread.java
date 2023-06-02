package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirParams;

import java.nio.ByteBuffer;

/**
 * XPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class XPirClientThread extends Thread {
    /**
     * XPIR client
     */
    private final Mbfk16SingleIndexPirClient client;
    /**
     * XPIR params
     */
    private final Mbfk16SingleIndexPirParams indexPirParams;
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

    XPirClientThread(Mbfk16SingleIndexPirClient client, Mbfk16SingleIndexPirParams indexPirParams, int retrievalIndex,
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
