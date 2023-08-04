package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Sbitmap protocol description.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class SbitmapPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8994173660078441772L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SBITMAP";

    /**
     * protocol step
     */
    public enum PtoStep {
        /**
         * PID
         */
        PID,
        /**
         * AND
         */
        AND
    }

    /**
     * singleton mode
     */
    private static final SbitmapPtoDesc INSTANCE = new SbitmapPtoDesc();

    /**
     * private constructor
     */
    private SbitmapPtoDesc() {
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
