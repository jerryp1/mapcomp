package edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23b;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Xxx+23b un-shuffle protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/5/26
 */
public class Xxx23bShufflePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 506301946942776593L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23B_UN_SHUFFLE";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Xxx23bShufflePtoDesc INSTANCE = new Xxx23bShufflePtoDesc();

    /**
     * private constructor.
     */
    private Xxx23bShufflePtoDesc() {
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
