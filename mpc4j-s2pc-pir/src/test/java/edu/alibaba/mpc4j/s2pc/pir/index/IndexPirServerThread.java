package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Index PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class IndexPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexPirServerThread.class);
    /**
     * index PIR server
     */
    private final SingleIndexPirServer server;
    /**
     * database
     */
    private final NaiveDatabase database;

    IndexPirServerThread(SingleIndexPirServer server, NaiveDatabase database) {
        this.server = server;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            server.init(database);
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
