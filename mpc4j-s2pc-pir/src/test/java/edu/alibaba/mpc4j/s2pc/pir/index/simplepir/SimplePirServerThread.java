package edu.alibaba.mpc4j.s2pc.pir.index.simplepir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/5/31
 */
public class SimplePirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePirServerThread.class);
    /**
     * Simple PIR server
     */
    private final Hhcm23SimpleSingleIndexPirServer server;
    /**
     * Double PIR params
     */
    private final Hhcm23SimpleSingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    SimplePirServerThread(Hhcm23SimpleSingleIndexPirServer server, Hhcm23SimpleSingleIndexPirParams indexPirParams,
                          NaiveDatabase database) {
        this.server = server;
        this.indexPirParams = indexPirParams;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            server.init(indexPirParams, database);
            LOGGER.info("Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
            server.getRpc().reset();
            server.getRpc().synchronize();
            server.pir();
            LOGGER.info("Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1024 * 1024));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
