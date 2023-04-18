package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.util.Set;

/**
 * @author Liqiang Peng
 * @date 2023/4/18
 */
public class UcpsiClientThread<T> extends Thread {
    /**
     * the receiver
     */
    private final UnbalancedCpsiClient<T> receiver;
    /**
     * the receiver set
     */
    private final Set<T> clientElementSet;
    /**
     * the PRF outputs
     */
    private SquareShareZ2Vector targetArray;
    private int serverElementSize;

    UcpsiClientThread(UnbalancedCpsiClient<T> receiver, Set<T> clientElementSet, int serverElementSize) {
        this.receiver = receiver;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    SquareShareZ2Vector getTargetArray() {
        return targetArray;
    }

    @Override
    public void run() {
        try {
            receiver.init(clientElementSet.size(), serverElementSize);
            targetArray = receiver.psi(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
