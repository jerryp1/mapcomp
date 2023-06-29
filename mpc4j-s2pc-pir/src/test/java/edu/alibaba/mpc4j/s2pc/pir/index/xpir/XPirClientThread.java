package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * XPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class XPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(XPirClientThread.class);
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
