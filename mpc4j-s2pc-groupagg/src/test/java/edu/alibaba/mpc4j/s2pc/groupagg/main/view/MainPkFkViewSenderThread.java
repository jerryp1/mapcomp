package edu.alibaba.mpc4j.s2pc.groupagg.main.view;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

public class MainPkFkViewSenderThread extends Thread {
    /**
     * receiver RPC
     */
    private final Rpc senderRpc;
    /**
     * sender party
     */
    private final Party receiverParty;
    /**
     * main pkFkView
     */
    private final PkFkViewMain pkFkViewMain;

    MainPkFkViewSenderThread(Rpc senderRpc, Party receiverParty, PkFkViewMain pkFkViewMain) {
        this.senderRpc = senderRpc;
        this.receiverParty = receiverParty;
        this.pkFkViewMain = pkFkViewMain;
    }

    @Override
    public void run() {
        try {
            pkFkViewMain.runSender(senderRpc, receiverParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
