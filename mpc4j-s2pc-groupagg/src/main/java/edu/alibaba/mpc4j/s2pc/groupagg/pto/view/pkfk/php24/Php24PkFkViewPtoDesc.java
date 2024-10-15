package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class Php24PkFkViewPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3725325741451236135L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PK_FK_VIEW_BASELINE";

    /**
     * singleton mode
     */
    private static final Php24PkFkViewPtoDesc INSTANCE = new Php24PkFkViewPtoDesc();

    public enum PtoStep {
        /**
         * the client sends hash keys
         */
        SERVER_SEND_HASH_KEYS,
    }

    /**
     * private constructor
     */
    private Php24PkFkViewPtoDesc() {
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
