package edu.alibaba.mpc4j.s2pc.pir.index.constantweightpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirParams;


import java.nio.ByteBuffer;

/**
 * Constant-Weight Pir Client Thread
 *
 * @author Qixian Zhou
 * @date 2023/6/20
 */
public class ConstantWeightPirClientThread extends Thread {
    /**
     * Mul PIR client
     */
    private final Mk22SingleIndexPirClient client;
    /**
     * Mul PIR params
     */
    private final Mk22SingleIndexPirParams indexPirParams;
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

    ConstantWeightPirClientThread(Mk22SingleIndexPirClient client, Mk22SingleIndexPirParams indexPirParams, int retrievalSingleIndex,
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
