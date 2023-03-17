package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * YWL20-BP-DPPRF description. The scheme comes from:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class Ywl20BpDpprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2106752700961581956L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWL20_BP_DPPRF";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * i ∈ {1,...,h}, R sends a bit b_i = r_i ⊕ α_i ⊕ 1.
         */
        RECEIVER_SEND_BINARY_ARRAY,
        /**
         * i ∈ {1,...,h}, S sends M_0^i = K_0^i ⊕ H(q_i ⊕ b_i ∆, i || l), M_1^i = K_1^i ⊕ H(q_i ⊕ \not b_i ∆, i || l)
         */
        SENDER_SEND_MESSAGE_ARRAY,
    }

    /**
     * singleton mode
     */
    private static final Ywl20BpDpprfPtoDesc INSTANCE = new Ywl20BpDpprfPtoDesc();

    /**
     * private constructor
     */
    private Ywl20BpDpprfPtoDesc() {
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
