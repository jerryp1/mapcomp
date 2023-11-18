package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * DSZ15 Zl DReLU protocol description. The protocol comes from the following paper:
 * <p>
 *
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/18
 */
public class Dsz15ZlDreluPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8661344656114422625L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DSZ15_ZL_DRELU";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Dsz15ZlDreluPtoDesc INSTANCE = new Dsz15ZlDreluPtoDesc();

    /**
     * private constructor.
     */
    private Dsz15ZlDreluPtoDesc() {
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
