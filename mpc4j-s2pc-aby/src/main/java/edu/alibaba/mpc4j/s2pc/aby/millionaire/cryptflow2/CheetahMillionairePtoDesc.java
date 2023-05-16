package edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Cheetah Millionaire Protocol Description.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class CheetahMillionairePtoDesc implements PtoDesc {
    /**
     * Protocol id.
     */
    private static final int PTO_ID = Math.abs((int) 6159294510188420044L);
    /**
     * Protocol name.
     */
    private static final String PTO_NAME = "CHEETAH_MILLIONAIRE";

    /**
     * Protocol steps.
     */
    enum PtoStep {
        /**
         * the sender sends s.
         */
        SENDER_SENDS_S,
        /**
         * the sender sends t.
         */
        SENDER_SENDS_T,
    }

    /**
     * Singleton pattern.
     */
    private static final CheetahMillionairePtoDesc INSTANCE = new CheetahMillionairePtoDesc();

    /**
     * Private constructor.
     */
    private CheetahMillionairePtoDesc() {
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
