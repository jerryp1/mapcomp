package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FastPIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class FastPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(FastPirServerThread.class);
    /**
     * FastPIR server
     */
    private final Ayaa21SingleIndexPirServer server;
    /**
     * FastPIR params
     */
    private final Ayaa21SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    FastPirServerThread(Ayaa21SingleIndexPirServer server, Ayaa21SingleIndexPirParams indexPirParams,
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
