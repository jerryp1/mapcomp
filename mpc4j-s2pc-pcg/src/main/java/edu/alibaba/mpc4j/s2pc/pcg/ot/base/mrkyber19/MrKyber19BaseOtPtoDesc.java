package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mrkyber19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MRKYBER19-基础OT协议信息。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * @author Sheng Hu
 * @date 2022/08/05
 */

public class MrKyber19BaseOtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)7413097849730455724L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "MRKYBER19_BASE_OT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送公钥PK0、PK1
         */
        RECEIVER_SEND_PK,
        /**
         * 发送方发送参数B
         */
        SENDER_SEND_B,
    }
    /**
     * 单例模式
     */
    private static final MrKyber19BaseOtPtoDesc INSTANCE = new MrKyber19BaseOtPtoDesc();

    /**
     * 私有构造函数
     */
    private MrKyber19BaseOtPtoDesc() {
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
