package edu.alibaba.mpc4j.s2pc.opf.shuffle.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24 shuffle protocol description.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Php24ShufflePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4005670455894029821L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24_SHUFFLE";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Php24ShufflePtoDesc INSTANCE = new Php24ShufflePtoDesc();

    /**
     * private constructor.
     */
    private Php24ShufflePtoDesc() {
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
