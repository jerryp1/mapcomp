package edu.alibaba.mpc4j.s2pc.aby.basics.a2b.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * DSZ15 A2b protocol description. The protocol comes from the following paper:
 * <p>
 * Demmler, Daniel, Thomas Schneider, and Michael Zohner. "ABY-A framework for efficient mixed-protocol
 * secure two-party computation." NDSS. 2015.
 * </p>
 *
 * @author Li Peng
 * @date 2023/10/20
 */
public class Dsz15A2bPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -3704115308334790390L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DSZ15_A2B";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Dsz15A2bPtoDesc INSTANCE =
        new Dsz15A2bPtoDesc();

    /**
     * private constructor.
     */
    private Dsz15A2bPtoDesc() {
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
