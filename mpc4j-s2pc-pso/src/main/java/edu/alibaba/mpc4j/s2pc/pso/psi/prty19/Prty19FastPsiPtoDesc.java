package edu.alibaba.mpc4j.s2pc.pso.psi.prty19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PRTY19_FAST_PSI协议信息。论文来源：
 * <p>
 * Benny Pinkas, Mike Rosulek, et al. SpOT-Light- Lightweight Private Set Intersection from Sparse OT Extension.
 * CRYPTO 2019, pp. 401–431.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
public class Prty19FastPsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -5838309125365213426L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PRTY19_FAST_PSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送PRF
         */
        SERVER_SEND_PRFS_0,

        SERVER_SEND_PRFS_1,
    }
    /**
     * 单例模式
     */
    private static final Prty19FastPsiPtoDesc INSTANCE = new Prty19FastPsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Prty19FastPsiPtoDesc() {
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
