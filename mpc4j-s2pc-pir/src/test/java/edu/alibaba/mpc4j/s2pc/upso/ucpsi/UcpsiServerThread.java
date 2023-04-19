package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * unbalanced circuit PSI receiver thread.
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
public class UcpsiServerThread extends Thread {
    /**
     * the sender
     */
    private final UcpsiServer sender;
    /**
     * the sender set
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * the sender outputs
     */
    private SquareShareZ2Vector z2Vector;
    /**
     * client element size
     */
    private final int clientElementSize;

    UcpsiServerThread(UcpsiServer sender, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.sender = sender;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    SquareShareZ2Vector getOutputs() {
        return z2Vector;
    }

    @Override
    public void run() {
        try {
            sender.init(serverElementSet, clientElementSize);
            z2Vector = sender.psi();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
