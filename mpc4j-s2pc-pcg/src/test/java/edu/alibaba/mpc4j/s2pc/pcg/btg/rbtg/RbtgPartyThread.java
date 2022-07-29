package edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BooleanTriple;

/**
 * RBTG协议参与方线程。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class RbtgPartyThread extends Thread {
    /**
     * 参与方
     */
    private final RbtgParty party;
    /**
     * 布尔三元组数量
     */
    private final int num;
    /**
     * 输出
     */
    private BooleanTriple output;

    RbtgPartyThread(RbtgParty party, int num) {
        this.party = party;
        this.num = num;
    }

    BooleanTriple getOutput() {
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
