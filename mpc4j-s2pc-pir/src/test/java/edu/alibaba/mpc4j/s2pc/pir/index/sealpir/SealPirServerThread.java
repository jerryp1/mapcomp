package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SEAL PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class SealPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SealPirServerThread.class);
    /**
     * SEAL PIR server
     */
    private final Acls18SingleIndexPirServer server;
    /**
     * SEAL PIR params
     */
    private final Acls18SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    SealPirServerThread(Acls18SingleIndexPirServer server, Acls18SingleIndexPirParams indexPirParams,
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
