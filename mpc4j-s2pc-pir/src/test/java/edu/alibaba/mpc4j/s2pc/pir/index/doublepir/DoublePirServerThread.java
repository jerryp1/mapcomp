package edu.alibaba.mpc4j.s2pc.pir.index.doublepir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Double PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/5/31
 */
public class DoublePirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoublePirServerThread.class);
    /**
     * Double PIR server
     */
    private final Hhcm23DoubleSingleIndexPirServer server;
    /**
     * Double PIR params
     */
    private final Hhcm23DoubleSingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    DoublePirServerThread(Hhcm23DoubleSingleIndexPirServer server, Hhcm23DoubleSingleIndexPirParams indexPirParams,
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
