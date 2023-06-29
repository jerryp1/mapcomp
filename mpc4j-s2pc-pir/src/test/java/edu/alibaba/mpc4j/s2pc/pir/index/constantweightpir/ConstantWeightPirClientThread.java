package edu.alibaba.mpc4j.s2pc.pir.index.constantweightpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.ByteBuffer;

/**
 * Constant-Weight Pir Client Thread
 *
 * @author Qixian Zhou
 * @date 2023/6/20
 */
public class ConstantWeightPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantWeightPirClientThread.class);
    /**
     * Constant-Weight PIR client
     */
    private final Mk22SingleIndexPirClient client;
    /**
     * Constant-Weight PIR params
     */
    private final Mk22SingleIndexPirParams indexPirParams;
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

    ConstantWeightPirClientThread(Mk22SingleIndexPirClient client, Mk22SingleIndexPirParams indexPirParams,
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
