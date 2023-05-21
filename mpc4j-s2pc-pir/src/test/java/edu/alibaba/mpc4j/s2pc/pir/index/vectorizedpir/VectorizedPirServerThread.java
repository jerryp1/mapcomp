package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirServer;

/**
 * Vectorized PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/3/24
 */
public class VectorizedPirServerThread extends Thread {
    /**
     * Vectorized PIR server
     */
    private final Mr23SingleIndexPirServer server;
    /**
     * Vectorized PIR params
     */
    private final Mr23SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    VectorizedPirServerThread(Mr23SingleIndexPirServer server, Mr23SingleIndexPirParams indexPirParams,
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
