package edu.alibaba.mpc4j.s2pc.pso.main.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

public class MainPlpsiClientThread extends Thread {
    /**
     * client RPC
     */
    private final Rpc clientRpc;
    /**
     * server party
     */
    private final Party serverParty;
    /**
     * main PSI
     */
    private final PlpsiMain plpsiMain;
    /**
     * success
     */
    private boolean success;

    MainPlpsiClientThread(Rpc clientRpc, Party serverParty, PlpsiMain plpsiMain) {
        this.clientRpc = clientRpc;
        this.serverParty = serverParty;
        this.plpsiMain = plpsiMain;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            plpsiMain.runClient(clientRpc, serverParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}
