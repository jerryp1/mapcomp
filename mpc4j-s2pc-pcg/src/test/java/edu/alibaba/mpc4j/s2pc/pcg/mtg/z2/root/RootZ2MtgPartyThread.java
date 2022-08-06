package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;

/**
 * 根布尔三元组生成协议参与方线程。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class RootZ2MtgPartyThread extends Thread {
    /**
     * 参与方
     */
    private final RootZ2MtgParty party;
    /**
     * 布尔三元组数量
     */
    private final int num;
    /**
     * 输出
     */
    private Z2Triple output;

    RootZ2MtgPartyThread(RootZ2MtgParty party, int num) {
        this.party = party;
        this.num = num;
    }

    Z2Triple getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            party.getRpc().connect();
            party.init(num);
            output = party.generate(num);
            party.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
