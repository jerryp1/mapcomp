package edu.alibaba.mpc4j.s2pc.pso.cpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.nio.ByteBuffer;
import java.util.Set;


/**
 * Circuit PSI sender thread.
 *
 * @author Liqiang Peng
 * @date 2023/2/1
 */
class CpsiClientThread extends Thread {
    /**
     * client
     */
    private final CpsiClient client;
    /**
     * client element set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * client output
     */
    private SquareShareZ2Vector clientOutput;

    CpsiClientThread(CpsiClient client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    SquareShareZ2Vector getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            clientOutput = client.psi(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
