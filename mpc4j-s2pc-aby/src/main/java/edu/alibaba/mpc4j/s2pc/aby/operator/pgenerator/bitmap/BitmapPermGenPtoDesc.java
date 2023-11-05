package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class BitmapPermGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1579747698540881457L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "AHI22_P_SORTER";

    /**
     * singleton mode
     */
    private static final BitmapPermGenPtoDesc INSTANCE =
        new BitmapPermGenPtoDesc();

    /**
     * private constructor.
     */
    private BitmapPermGenPtoDesc() {
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
