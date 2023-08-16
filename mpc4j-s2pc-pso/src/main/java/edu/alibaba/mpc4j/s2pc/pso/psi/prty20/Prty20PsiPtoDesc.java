package edu.alibaba.mpc4j.s2pc.pso.psi.prty20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PRTY20 PSI协议描述信息，论文为：
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/14
 */
public class Prty20PsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -752220453444251882L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PRTY20_PSI";

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
    private static final Prty20PsiPtoDesc INSTANCE = new Prty20PsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Prty20PsiPtoDesc() {
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