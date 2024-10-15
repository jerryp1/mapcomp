package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class BaselinePkFkViewPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1831211969270444701L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PK_FK_VIEW_BASELINE";

    /**
     * singleton mode
     */
    private static final BaselinePkFkViewPtoDesc INSTANCE = new BaselinePkFkViewPtoDesc();

    public enum PtoStep {
        /**
         * the client sends hash keys
         */
        SERVER_SEND_HASH_KEYS,
        DEBUG,
    }

    /**
     * private constructor
     */
    private BaselinePkFkViewPtoDesc() {
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
