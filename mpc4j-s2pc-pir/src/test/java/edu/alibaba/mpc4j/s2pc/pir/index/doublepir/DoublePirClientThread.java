package edu.alibaba.mpc4j.s2pc.pir.index.doublepir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Double PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/5/31
 */
public class DoublePirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoublePirClientThread.class);
    /**
     * Double PIR client
     */
    private final Hhcm23DoubleSingleIndexPirClient client;
    /**
     * Double PIR params
     */
    private final Hhcm23DoubleSingleIndexPirParams indexPirParams;
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

    DoublePirClientThread(Hhcm23DoubleSingleIndexPirClient client, Hhcm23DoubleSingleIndexPirParams indexPirParams,
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
            LOGGER.info("Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
            client.getRpc().reset();
            client.getRpc().synchronize();
            indexPirResult = ByteBuffer.wrap(client.pir(retrievalIndex));
            LOGGER.info("Client: The Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
