package edu.alibaba.mpc4j.s2pc.opf.shuffle.php24b;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24b un-shuffle protocol description.
 *
 */
public class Php24bShufflePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 506301946942776593L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24B_UN_SHUFFLE";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Php24bShufflePtoDesc INSTANCE = new Php24bShufflePtoDesc();

    /**
     * private constructor.
     */
    private Php24bShufflePtoDesc() {
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
