package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * payload-circuit PSI server thread.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class PlpsiServerThread extends Thread {
    /**
     * server
     */
    private final PlpsiServer<ByteBuffer> server;
    /**
     * server element list
     */
    private final List<ByteBuffer> serverElementList;
    /**
     * server payload list
     */
    private final List<ByteBuffer> serverPayloadList;
    /**
     * client element size
     */
    private final int clientElementSize;
    /**
     * server payload bit length
     */
    private final int serverPayloadBitL;
    /**
     * server output
     */
    private PlpsiServerOutput serverOutput;

    PlpsiServerThread(PlpsiServer<ByteBuffer> server, List<ByteBuffer> serverElementList, List<ByteBuffer> serverPayloadList, int clientElementSize, int serverPayloadBitL) {
        this.server = server;
        this.serverElementList = serverElementList;
        this.serverPayloadList = serverPayloadList;
        this.clientElementSize = clientElementSize;
        this.serverPayloadBitL = serverPayloadBitL;
    }

    PlpsiServerOutput getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementList.size(), clientElementSize, serverPayloadBitL);
            serverOutput = server.psi(serverElementList, serverPayloadList, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
