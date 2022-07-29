package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.alsz13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ALSZ13-半诚实安全RCOT协议信息。论文来源：
 * <p>
 * Asharov G, Lindell Y, Schneider T, et al. More efficient oblivious transfer and extensions for faster secure
 * computation. CCS 2013, ACM, 2013, pp. 535-548.
 * </p>
 *
 * @author Weiran Liu
 * @date 2020/08/13
 */
class Alsz13RcotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)4297344541051710603L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ALSZ13_RCOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送矩阵
         */
        RECEIVER_SEND_MATRIX,
    }

    /**
     * 单例模式
     */
    private static final Alsz13RcotPtoDesc INSTANCE = new Alsz13RcotPtoDesc();

    /**
     * 私有构造函数
     */
    private Alsz13RcotPtoDesc() {
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
