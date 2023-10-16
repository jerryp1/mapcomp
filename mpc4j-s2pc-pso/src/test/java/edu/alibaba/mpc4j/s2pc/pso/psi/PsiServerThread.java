package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17ByteEccPsiServer;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSI server thread.
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
class PsiServerThread extends Thread {
    /**
     * PSI server
     */
    private final PsiServer<ByteBuffer> server;
    /**
     * server element set
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * client element size
     */
    private final int clientElementSize;

    PsiServerThread(PsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            if(server instanceof Ra17ByteEccPsiServer){
                ((Ra17ByteEccPsiServer<ByteBuffer>) server).setup(serverElementSet.size(), clientElementSize, serverElementSet);
                ((Ra17ByteEccPsiServer<ByteBuffer>) server).psiOnline(clientElementSize);
            }else{
                server.init(serverElementSet.size(), clientElementSize);
                server.psi(serverElementSet, clientElementSize);
            }

        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
