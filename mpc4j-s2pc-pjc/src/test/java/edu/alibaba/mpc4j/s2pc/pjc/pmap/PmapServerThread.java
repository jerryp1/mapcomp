package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.List;

public class PmapServerThread extends Thread {
    /**
     * party
     */
    private final PmapServer<ByteBuffer> pmapServer;
    /**
     * own element set
     */
    private final List<ByteBuffer> ownElementList;
    /**
     * client element size
     */
    private final int clientElementSize;
    /**
     * server output
     */
    private PmapPartyOutput<ByteBuffer> serverOutput;

    PmapServerThread(PmapServer<ByteBuffer> pmapServer, List<ByteBuffer> ownElementList, int clientElementSize) {
        this.pmapServer = pmapServer;
        this.ownElementList = ownElementList;
        this.clientElementSize = clientElementSize;
    }

    PmapPartyOutput<ByteBuffer> getPmapOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            pmapServer.init(ownElementList.size(), clientElementSize);
            serverOutput = pmapServer.map(ownElementList, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
