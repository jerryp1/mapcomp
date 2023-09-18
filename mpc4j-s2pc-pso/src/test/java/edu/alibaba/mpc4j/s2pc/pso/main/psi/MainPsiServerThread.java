package edu.alibaba.mpc4j.s2pc.pso.main.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

/**
 * PSI main server thread.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
class MainPsiServerThread extends Thread {
    /**
     * server RPC
     */
    private final Rpc serverRpc;
    /**
     * client party
     */
    private final Party clientParty;
    /**
     * main PSU
     */
    private final PsiMain psiMain;

    MainPsiServerThread(Rpc serverRpc, Party clientParty, PsiMain psiMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.psiMain = psiMain;
    }

    @Override
    public void run() {
        try {
            psiMain.runServer(serverRpc, clientParty);
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}
