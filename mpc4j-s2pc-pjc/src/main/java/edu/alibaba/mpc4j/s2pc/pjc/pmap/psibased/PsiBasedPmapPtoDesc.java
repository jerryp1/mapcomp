package edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

import java.io.Serializable;

/**
 * @author Feng Han
 * @date 2024/7/22
 */
public class PsiBasedPmapPtoDesc implements PtoDesc, Serializable {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6850253681126374703L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PSI_PMAP";

    /**
     * singleton mode
     */
    private static final PsiBasedPmapPtoDesc INSTANCE = new PsiBasedPmapPtoDesc();

    public enum PtoStep {
        /**
         * the client sends hash keys
         */
        CLIENT_SEND_SPI_RES,
    }

    /**
     * private constructor
     */
    private PsiBasedPmapPtoDesc() {
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
