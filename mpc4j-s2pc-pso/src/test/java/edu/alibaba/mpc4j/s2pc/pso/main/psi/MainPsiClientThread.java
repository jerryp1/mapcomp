package edu.alibaba.mpc4j.s2pc.pso.main.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuMain;

public class MainPsiClientThread extends Thread {
    /**
     * client RPC
     */
    private final Rpc clientRpc;
    /**
     * server party
     */
    private final Party serverParty;
    /**
     * main PSU
     */
    private final PsiMain psiMain;

    MainPsiClientThread(Rpc clientRpc, Party serverParty, PsiMain psiMain) {
        this.clientRpc = clientRpc;
        this.serverParty = serverParty;
        this.psiMain = psiMain;
    }

    @Override
    public void run() {
        try {
            psiMain.runClient(clientRpc, serverParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
