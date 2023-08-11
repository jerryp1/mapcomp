package edu.alibaba.mpc4j.s2pc.pso.psi.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CZZ22-PSI协议信息。
 * <p>
 * Chen, Yu, Min Zhang, Cong Zhang, and Minglang Dong. Private Set Operations from Multi-Query Reverse Private
 * Membership Test. Cryptology ePrint Archive (2022).
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Czz22PsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 5022494113030939769L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CZZ22_PSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送Cipher
         */
        SERVER_SEND_CIPHER,
    }

    /**
     * 单例模式
     */
    private static final Czz22PsiPtoDesc INSTANCE = new Czz22PsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Czz22PsiPtoDesc() {
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
