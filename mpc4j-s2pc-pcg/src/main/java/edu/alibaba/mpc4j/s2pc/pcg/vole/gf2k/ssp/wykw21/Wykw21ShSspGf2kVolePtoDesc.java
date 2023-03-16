package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * WYKW21-SSP-GF2K-VOLE (semi-honest) description. The protocol comes from:
 * <p>
 * Weng, Chenkai, Kang Yang, Jonathan Katz, and Xiao Wang. Wolverine: fast, scalable, and communication-efficient
 * zero-knowledge proofs for boolean and arithmetic circuits. S&P 2021, pp. 1074-1091. IEEE, 2021.
 * </p>
 * The semi-honest version does not require Consistency check shown in Figure 7.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
class Wykw21ShSspGf2kVolePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 9135789737278442424L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "WYKW21_SH_SSP_GF2K_VOLE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends x' = β - x
         */
        SENDER_SEND_X_PRIME,
        /**
         * receiver sends d = γ - Σ_{i ∈ [0, n)} v[i]
         */
        RECEIVER_SEND_D,
    }

    /**
     * singleton mode
     */
    private static final Wykw21ShSspGf2kVolePtoDesc INSTANCE = new Wykw21ShSspGf2kVolePtoDesc();

    /**
     * private constructor
     */
    private Wykw21ShSspGf2kVolePtoDesc() {
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
