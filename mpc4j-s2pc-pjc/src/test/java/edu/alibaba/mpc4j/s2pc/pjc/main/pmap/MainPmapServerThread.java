package edu.alibaba.mpc4j.s2pc.pjc.main.pmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

public class MainPmapServerThread extends Thread {
    /**
     * server RPC
     */
    private final Rpc serverRpc;
    /**
     * client party
     */
    private final Party clientParty;
    /**
     * main PID
     */
    private final PmapMain pmapMain;

    MainPmapServerThread(Rpc serverRpc, Party clientParty, PmapMain pidMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.pmapMain = pidMain;
    }

    @Override
    public void run() {
        try {
            pmapMain.runServer(serverRpc, clientParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
