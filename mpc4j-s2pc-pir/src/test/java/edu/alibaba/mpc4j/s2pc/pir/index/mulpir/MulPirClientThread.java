package edu.alibaba.mpc4j.s2pc.pir.index.mulpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Mul PIR client thread.
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class MulPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(MulPirClientThread.class);
    /**
     * Mul PIR client
     */
    private final Alpr21SingleIndexPirClient client;
    /**
     * Mul PIR params
     */
    private final Alpr21SingleIndexPirParams indexPirParams;
    /**
     * element bit length
     */
    private final int elementBitLength;
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

    MulPirClientThread(Alpr21SingleIndexPirClient client, Alpr21SingleIndexPirParams indexPirParams,
                       int retrievalSingleIndex, int serverElementSize, int elementBitLength) {
        this.client = client;
        this.indexPirParams = indexPirParams;
        this.retrievalSingleIndex = retrievalSingleIndex;
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
            LOGGER.info("Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
            client.getRpc().reset();
            client.getRpc().synchronize();
            indexPirResult = ByteBuffer.wrap(client.pir(retrievalSingleIndex));
            LOGGER.info("Client: The Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
