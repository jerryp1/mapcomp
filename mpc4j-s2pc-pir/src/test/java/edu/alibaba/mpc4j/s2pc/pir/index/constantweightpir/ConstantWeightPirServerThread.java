package edu.alibaba.mpc4j.s2pc.pir.index.constantweightpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constant-Weight Pir Server Thread
 *
 * @author Qixian Zhou
 * @date 2023/6/20
 */
public class ConstantWeightPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantWeightPirServerThread.class);
    /**
     * Constant-Weight PIR server
     */
    private final Mk22SingleIndexPirServer server;
    /**
     * Constant-Weight PIR params
     */
    private final Mk22SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    ConstantWeightPirServerThread(Mk22SingleIndexPirServer server, Mk22SingleIndexPirParams indexPirParams,
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
