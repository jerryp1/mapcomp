package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * RR16-PSI协议信息。此方案的核心是使用cut-of-choose来限制OT+GBF方案中receiver在OT中可设置1的数量，论文来源：
 * Peter Rindal and Mike Rosulek. Improved Private Set Intersection against Malicious Adversaries.
 * Advances in Cryptology 2017.
 * 为了与RR17做区分，在此和libPsi中命名一致，采用ePrint上的发表时间
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/06
 */
public class Rr16PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2034064559515953124L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RR16_PSI";
    /**
     * singleton mode
     */
    private static final Rr16PsiPtoDesc INSTANCE = new Rr16PsiPtoDesc();

    /**
     * private constructor.
     */
    private Rr16PsiPtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends challenge of OT choice bits
         */
        SERVER_SEND_CHANLLEGE,
        /**
         * client sends response
         */
        CLIENT_SEND_RESPONSE,
        /**
         * client sends permutation pi
         */
        CLIENT_SEND_PERMUTATION,
        /**
         * server sends PRFs
         */
        SERVER_SEND_PRFS,
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