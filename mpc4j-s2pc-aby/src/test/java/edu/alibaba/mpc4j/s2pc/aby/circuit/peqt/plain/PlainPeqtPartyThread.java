package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

/**
 * plain private equality test sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
class PlainPeqtPartyThread extends Thread {
    /**
     * the party
     */
    private final PlainPeqtParty party;
    /**
     * l
     */
    private final int l;
    /**
     * xs
     */
    private final byte[][] inputs;
    /**
     * num
     */
    private final int num;
    /**
     * zi
     */
    private SquareShareZ2Vector zi;

    PlainPeqtPartyThread(PlainPeqtParty party, int l, byte[][] inputs) {
        this.party = party;
        this.l = l;
        this.inputs = inputs;
        num = inputs.length;
    }

    SquareShareZ2Vector getZi() {
        return zi;
    }

    @Override
    public void run() {
        try {
            party.init(l, num);
            party.getRpc().reset();
            zi = party.peqt(l, inputs);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
