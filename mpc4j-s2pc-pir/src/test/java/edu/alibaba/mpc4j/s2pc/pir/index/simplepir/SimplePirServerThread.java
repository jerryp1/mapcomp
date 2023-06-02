package edu.alibaba.mpc4j.s2pc.pir.index.simplepir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SingleIndexPirServer;

/**
 * Simple PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/5/31
 */
public class SimplePirServerThread extends Thread {
    /**
     * Simple PIR server
     */
    private final Hhcm23SingleIndexPirServer server;
    /**
     * SEAL PIR params
     */
    private final Hhcm23SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    SimplePirServerThread(Hhcm23SingleIndexPirServer server, Hhcm23SingleIndexPirParams indexPirParams,
                          NaiveDatabase database) {
        this.server = server;
        this.indexPirParams = indexPirParams;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            server.init(indexPirParams, database);
            server.getRpc().synchronize();
            server.pir();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
