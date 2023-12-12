package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Pmap protocol test.
 *
 * @author Feng Han
 * @date 2023/11/03
 */
public class PmapClientThread extends Thread {
    /**
     * client
     */
    private final PmapClient<ByteBuffer> client;
    /**
     * client element set
     */
    private final List<ByteBuffer> clientElementList;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * client output
     */
    private PmapPartyOutput<ByteBuffer> clientOutput;

    PmapClientThread(PmapClient<ByteBuffer> client, List<ByteBuffer> clientElementList, int serverElementSize) {
        this.client = client;
        this.clientElementList = clientElementList;
        this.serverElementSize = serverElementSize;
    }

    PmapPartyOutput<ByteBuffer> getPmapOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementList.size(), serverElementSize);
            clientOutput = client.map(clientElementList, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
