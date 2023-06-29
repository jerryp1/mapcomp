package edu.alibaba.mpc4j.s2pc.pir.index.mulpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mul PIR server thread.
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class MulPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(MulPirServerThread.class);
    /**
     * Mul PIR server
     */
    private final Alpr21SingleIndexPirServer server;
    /**
     * Mul PIR params
     */
    private final Alpr21SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    MulPirServerThread(Alpr21SingleIndexPirServer server, Alpr21SingleIndexPirParams indexPirParams, NaiveDatabase database) {
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
