package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RR17 Dual Execution PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Peter Rindal, Mike Rosulek. Malicious-Secure Private Set Intersection via Dual Execution.
 * ACM CCS 2017
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17DePsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -947977268059832451L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "RR17_DE_PSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送哈希桶PRF
         */
        SERVER_SEND_PRFS,
    }

    /**
     * 单例模式
     */
    private static final Rr17DePsiPtoDesc INSTANCE = new Rr17DePsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Rr17DePsiPtoDesc() {
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