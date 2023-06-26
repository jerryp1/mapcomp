package edu.alibaba.mpc4j.s2pc.pir.index.constantweightpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirServer;

/**
 * Constant-Weight Pir Server Thread
 *
 * @author Qixian Zhou
 * @date 2023/6/20
 */
public class ConstantWeightPirServerThread extends Thread {

    /**
     * Mul PIR server
     */
    private final Mk22SingleIndexPirServer server;
    /**
     * Mul PIR params
     */
    private final Mk22SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    ConstantWeightPirServerThread(Mk22SingleIndexPirServer server, Mk22SingleIndexPirParams indexPirParams, NaiveDatabase database) {
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
