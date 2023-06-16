package edu.alibaba.mpc4j.s2pc.pir.index.doublepir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirServer;

/**
 * Double PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/5/31
 */
public class DoublePirServerThread extends Thread {
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
            server.getRpc().synchronize();
            server.pir();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
