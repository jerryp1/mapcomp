package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.bea95;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Bea95-PCOT协议信息。协议来自下述论文的图3：
 * <p>
 * Beaver, Donald. Precomputing oblivious transfer. CRYPTO 1995, pp. 97-109. Springer, Berlin, Heidelberg, 1995.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
class Bea95PcotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 3370327442911780279L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "BEA95_PCOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送纠正比特
         */
        RECEIVER_SEND_XOR,
    }

    /**
     * 单例模式
     */
    private static final Bea95PcotPtoDesc INSTANCE = new Bea95PcotPtoDesc();

    /**
     * 私有构造函数
     */
    private Bea95PcotPtoDesc() {
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
