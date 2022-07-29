package edu.alibaba.mpc4j.s2pc.pcg.btg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * BTG协议参与方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
class BtgPartyThread extends Thread {
    /**
     * 参与方
     */
    private final BtgParty party;
    /**
     * 布尔三元组数量
     */
    private final int num;
    /**
     * 输出
     */
    private BooleanTriple output;

    BtgPartyThread(BtgParty party, int num) {
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
            party.init(num, num);
            output = party.generate(num);
            party.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
