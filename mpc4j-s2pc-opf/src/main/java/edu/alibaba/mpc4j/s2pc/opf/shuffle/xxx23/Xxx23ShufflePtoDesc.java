package edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Xxx+23 shuffle protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Xxx23ShufflePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4005670455894029821L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23_SHUFFLE";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Xxx23ShufflePtoDesc INSTANCE = new Xxx23ShufflePtoDesc();

    /**
     * private constructor.
     */
    private Xxx23ShufflePtoDesc() {
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
