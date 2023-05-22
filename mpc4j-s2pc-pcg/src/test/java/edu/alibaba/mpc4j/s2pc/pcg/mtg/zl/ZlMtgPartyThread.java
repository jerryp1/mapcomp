package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Zl multiplication triple generator party thread.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
class ZlMtgPartyThread extends Thread {
    /**
     * party
     */
    private final ZlMtgParty party;
    /**
     * num
     */
    private final int num;
    /**
     * output
     */
    private ZlTriple output;

    ZlMtgPartyThread(ZlMtgParty party, int num) {
        this.party = party;
        this.num = num;
    }

    ZlTriple getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            party.init(num, num);
            output = party.generate(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
