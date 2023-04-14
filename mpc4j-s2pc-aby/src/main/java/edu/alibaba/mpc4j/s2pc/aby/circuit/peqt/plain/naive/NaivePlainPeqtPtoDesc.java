package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.naive;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * naive plain private equality test protocol description. This protocol does bit-wise AND / OR for PEQT.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
class NaivePlainPeqtPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 792694580069278595L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "NAIVE_PLAIN_PEQT";
    /**
     * singleton mode
     */
    private static final NaivePlainPeqtPtoDesc INSTANCE = new NaivePlainPeqtPtoDesc();

    /**
     * private constructor.
     */
    private NaivePlainPeqtPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
