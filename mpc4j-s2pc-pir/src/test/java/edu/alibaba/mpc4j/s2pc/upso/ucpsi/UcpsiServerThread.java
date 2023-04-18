package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.util.Set;

/**
 * unbalanced circuit PSI receiver thread.
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
public class UcpsiServerThread<T> extends Thread {
    /**
     * the sender
     */
    private final UnbalancedCpsiServer<T> sender;
    /**
     * the sender set
     */
    private final Set<T> serverElementSet;
    /**
     * the PRF outputs
     */
    private UnbalancedCpsiServerOutput<T> targetArray;
    private int clientElementSize;

    UcpsiServerThread(UnbalancedCpsiServer<T> sender, Set<T> serverElementSet, int clientElementSize) {
        this.sender = sender;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    UnbalancedCpsiServerOutput<T> getTargetArray() {
        return targetArray;
    }

    @Override
    public void run() {
        try {
            sender.init(serverElementSet.size(), clientElementSize);
            targetArray = sender.psi(serverElementSet, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
