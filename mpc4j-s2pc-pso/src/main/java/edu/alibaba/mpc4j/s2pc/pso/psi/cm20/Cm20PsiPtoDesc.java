package edu.alibaba.mpc4j.s2pc.pso.psi.cm20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CM20-PSI协议信息。论文来源：
 * <p>
 * Chase M, Miao P. Private Set Intersection in the Internet Setting from Lightweight Oblivious PRF. CRYPTO 2020.
 * pp. 34-63.
 * <p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2022/03/03
 */
public class Cm20PsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 8849499987257147091L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CM20_PSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送PRF
         */
        SERVER_SEND_PRFS,
    }
    /**
     * 单例模式
     */
    private static final Cm20PsiPtoDesc INSTANCE = new Cm20PsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Cm20PsiPtoDesc() {
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
