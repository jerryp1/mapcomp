package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

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
    private final int[] serverPayloadBitLs;

    private final boolean[] isBinaryShares;
    /**
     * client output
     */
    private PlpsiClientOutput<ByteBuffer> clientOutput;
    /**
     * whether deal all payload with flag together
     */
    private final boolean dealTogether;

    PlpsiClientThread(PlpsiClient<ByteBuffer> client, List<ByteBuffer> clientElementList, int serverElementSize,
                      int[] serverPayloadBitLs, boolean[] isBinaryShares, boolean dealTogether) {
        this.client = client;
        this.clientElementList = clientElementList;
        this.serverElementSize = serverElementSize;
        if(serverPayloadBitLs != null){
            MathPreconditions.checkEqual("serverPayloadBitLs.length", "isBinaryShares.length", serverPayloadBitLs.length, isBinaryShares.length);
        }
        this.serverPayloadBitLs = serverPayloadBitLs;
        this.isBinaryShares = isBinaryShares;
        this.dealTogether = dealTogether;
    }

    PlpsiClientOutput<ByteBuffer> getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementList.size(), serverElementSize);
            if(dealTogether){
                clientOutput = client.psiWithPayload(clientElementList, serverElementSize, serverPayloadBitLs, isBinaryShares);
            }else{
                clientOutput = client.psi(clientElementList, serverElementSize);
                if(serverPayloadBitLs != null){
                    for(int i = 0; i < serverPayloadBitLs.length; i++){
                        client.intersectPayload(serverPayloadBitLs[i], isBinaryShares[i]);
                    }
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
