package edu.alibaba.mpc4j.s2pc.pjc.pmap.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * level 2 secure map protocol description, the protocols comes from "MapComp"
 *
 * @author Feng Han
 * @date 2023/10/24
 */
public class Php24PmapPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -3452608700023484963L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24_PMAP";

    /**
     * singleton mode
     */
    private static final Php24PmapPtoDesc INSTANCE = new Php24PmapPtoDesc();

    /**
     * private constructor
     */
    private Php24PmapPtoDesc() {
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
