package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * payload-circuit PSI client thread.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class PlpsiClientThread extends Thread {
    /**
     * client
     */
    private final PlpsiClient<ByteBuffer> client;
    /**
     * client element set
     */
    private final List<ByteBuffer> clientElementList;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * server payload bit length
     */
    private final int serverPayloadBitL;

    /**
     * client output
     */
    private PlpsiClientOutput<ByteBuffer> clientOutput;

    PlpsiClientThread(PlpsiClient<ByteBuffer> client, List<ByteBuffer> clientElementList, int serverElementSize, int serverPayloadBitL) {
        this.client = client;
        this.clientElementList = clientElementList;
        this.serverElementSize = serverElementSize;
        this.serverPayloadBitL = serverPayloadBitL;
    }

    PlpsiClientOutput<ByteBuffer> getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementList.size(), serverElementSize, serverPayloadBitL);
            clientOutput = client.psi(clientElementList, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
