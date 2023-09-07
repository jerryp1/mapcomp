package edu.alibaba.mpc4j.s2pc.pso.psi.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21-PSI协议信息。论文来源：
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Gmr21PsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -2423946578643819624L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "GMR21_PSI";

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
    private static final Gmr21PsiPtoDesc INSTANCE = new Gmr21PsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Gmr21PsiPtoDesc() {
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
