package edu.alibaba.mpc4j.s2pc.groupagg.main.view;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * @author Feng Han
 * @date 2024/7/24
 */
public class MainPkFkViewReceiverThread extends Thread {
    /**
     * receiver RPC
     */
    private final Rpc receiverRpc;
    /**
     * sender party
     */
    private final Party senderParty;
    /**
     * main pkFkView
     */
    private final PkFkViewMain pkFkViewMain;

    MainPkFkViewReceiverThread(Rpc receiverRpc, Party senderParty, PkFkViewMain pkFkViewMain) {
        this.receiverRpc = receiverRpc;
        this.senderParty = senderParty;
        this.pkFkViewMain = pkFkViewMain;
    }

    @Override
    public void run() {
        try {
            pkFkViewMain.runReceiver(receiverRpc, senderParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
