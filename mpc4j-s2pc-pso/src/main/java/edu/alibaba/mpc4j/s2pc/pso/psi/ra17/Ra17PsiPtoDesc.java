package edu.alibaba.mpc4j.s2pc.pso.psi.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class Ra17PsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 4737662127936250651L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "RA17_ECC_PSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送PRF
         */
        SERVER_SEND_BLIND_PRFS,
    }
    /**
     * 单例模式
     */
    private static final Ra17PsiPtoDesc INSTANCE = new Ra17PsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Ra17PsiPtoDesc() {
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
