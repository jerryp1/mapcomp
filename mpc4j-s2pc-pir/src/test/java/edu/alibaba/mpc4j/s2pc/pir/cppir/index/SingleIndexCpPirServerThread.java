package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single Index Client-specific Preprocessing PIR server thread.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
class SingleIndexCpPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleIndexCpPirServerThread.class);
    /**
     * server
     */
    private final SingleIndexCpPirServer server;
    /**
     * database
     */
    private final ZlDatabase database;

    SingleIndexCpPirServerThread(SingleIndexCpPirServer server, ZlDatabase database) {
        this.server = server;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            server.init(database);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            server.pir();
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
