package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * @author Liqiang Peng
 * @date 2023/4/18
 */
public class UcpsiClientThread extends Thread {
    /**
     * the receiver
     */
    private final UcpsiClient receiver;
    /**
     * the receiver set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * the receiver outputs
     */
    private UcpsiClientOutput outputs;
    /**
     * server element size
     */
    private final int serverElementSize;

    UcpsiClientThread(UcpsiClient receiver, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.receiver = receiver;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    UcpsiClientOutput getOutputs() {
        return outputs;
    }

    @Override
    public void run() {
        try {
            receiver.init(clientElementSet.size(), serverElementSize);
            outputs = receiver.psi(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
