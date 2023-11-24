package edu.alibaba.mpc4j.s2pc.pso.main.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

public class MainPlpsiServerThread extends Thread {
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
    private final PlpsiMain plpsiMain;
    /**
     * success
     */
    private boolean success;

    MainPlpsiServerThread(Rpc serverRpc, Party clientParty, PlpsiMain plpsiMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.plpsiMain = plpsiMain;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            plpsiMain.runServer(serverRpc, clientParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}
