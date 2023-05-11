package edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Cryptflow2 Millionaire Protocol Description.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class Cryptflow2MillionairePtoDesc implements PtoDesc {
    /**
     * Protocol id.
     */
    private static final int PTO_ID = Math.abs((int) 6159294510188420043L);
    /**
     * Protocol name.
     */
    private static final String PTO_NAME = "CRYPTFLOW2_MILLIONAIRE";

    /**
     * Protocol steps.
     */
    enum PtoStep {
        /**
         * OT
         */
        OT,
    }

    /**
     * Singleton pattern.
     */
    private static final Cryptflow2MillionairePtoDesc INSTANCE = new Cryptflow2MillionairePtoDesc();

    /**
     * Private constructor.
     */
    private Cryptflow2MillionairePtoDesc() {
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
