package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirServer;

/**
 * SEAL PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class SealPirServerThread extends Thread {
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

    SealPirServerThread(Acls18SingleIndexPirServer server, Acls18SingleIndexPirParams indexPirParams, NaiveDatabase database) {
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
