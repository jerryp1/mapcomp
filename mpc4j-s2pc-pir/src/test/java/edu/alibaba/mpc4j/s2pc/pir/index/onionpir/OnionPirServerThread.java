package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OnionPIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class OnionPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnionPirServerThread.class);
    /**
     * OnionPIR server
     */
    private final Mcr21SingleIndexPirServer server;
    /**
     * OnionPIR params
     */
    private final Mcr21SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    OnionPirServerThread(Mcr21SingleIndexPirServer server, Mcr21SingleIndexPirParams indexPirParams,
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
