package edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain.fa;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 基于全加器的明文比较协议信息。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/11
 */
class FullAdderPlainComparePtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6159294510188420043L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "BCP13_HAMMING";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送OT数据
         */
        SENDER_SEND_PAYLOAD,
        /**
         * 接收方发送T
         */
        RECEIVER_SEND_T,
        /**
         * 发送方发送R
         */
        SENDER_SEND_R,
    }

    /**
     * 单例模式
     */
    private static final FullAdderPlainComparePtoDesc INSTANCE = new FullAdderPlainComparePtoDesc();

    /**
     * 私有构造函数
     */
    private FullAdderPlainComparePtoDesc() {
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
