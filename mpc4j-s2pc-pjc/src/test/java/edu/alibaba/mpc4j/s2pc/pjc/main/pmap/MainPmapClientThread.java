package edu.alibaba.mpc4j.s2pc.pjc.main.pmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

public class MainPmapClientThread extends Thread {
    /**
     * client RPC
     */
    private final Rpc clientRpc;
    /**
     * server party
     */
    private final Party serverParty;
    /**
     * main PID
     */
    private final PmapMain pmapMain;

    MainPmapClientThread(Rpc clientRpc, Party serverParty, PmapMain pidMain) {
        this.clientRpc = clientRpc;
        this.serverParty = serverParty;
        this.pmapMain = pidMain;
    }

    @Override
    public void run() {
        try {
            pmapMain.runClient(clientRpc, serverParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
