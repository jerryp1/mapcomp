package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

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
    private final PlpsiServer<ByteBuffer, ByteBuffer> server;
    /**
     * server element list
     */
    private final List<ByteBuffer> serverElementList;
    /**
     * server payload list
     */
    private final List<List<ByteBuffer>> serverPayloadLists;
    /**
     * client element size
     */
    private final int clientElementSize;
    /**
     * server payload bit length
     */
    private final int[] serverPayloadBitLs;
    /**
     * server payload share type
     */
    private final boolean[] isBinaryShares;
    /**
     * server output
     */
    private PlpsiShareOutput serverOutput;

    PlpsiServerThread(PlpsiServer<ByteBuffer, ByteBuffer> server, List<ByteBuffer> serverElementList, int clientElementSize,
                      List<List<ByteBuffer>> serverPayloadLists, int[] serverPayloadBitLs, boolean[] isBinaryShares) {
        this.server = server;
        this.serverElementList = serverElementList;
        this.clientElementSize = clientElementSize;
        if(serverPayloadLists != null){
            MathPreconditions.checkEqual("serverPayloadLists.length", "serverPayloadBitLs.length", serverPayloadLists.size(), serverPayloadBitLs.length);
            MathPreconditions.checkEqual("serverPayloadBitLs.length", "isBinaryShares.length", serverPayloadBitLs.length, isBinaryShares.length);
        }
        this.serverPayloadLists = serverPayloadLists;
        this.serverPayloadBitLs = serverPayloadBitLs;
        this.isBinaryShares = isBinaryShares;
    }

    PlpsiShareOutput getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementList.size(), clientElementSize);
            serverOutput = server.psi(serverElementList, clientElementSize);
            if(serverPayloadLists != null){
                for(int i = 0; i < serverPayloadBitLs.length; i++){
                    server.intersectPayload(serverPayloadLists.get(i), serverPayloadBitLs[i], isBinaryShares[i]);
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
