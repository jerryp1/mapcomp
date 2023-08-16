package edu.alibaba.mpc4j.s2pc.pso.psi.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class Psz14GbfPsiPtoDesc  implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -8635439476687893783L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PSZ14GBF_PSI";

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
    private static final Psz14GbfPsiPtoDesc INSTANCE = new Psz14GbfPsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Psz14GbfPsiPtoDesc() {
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