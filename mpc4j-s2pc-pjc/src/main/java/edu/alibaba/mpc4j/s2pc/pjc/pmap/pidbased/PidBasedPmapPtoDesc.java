package edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class PidBasedPmapPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -2607344576398744169L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PID_PMAP";

    /**
     * 单例模式
     */
    private static final PidBasedPmapPtoDesc INSTANCE = new PidBasedPmapPtoDesc();

    public enum PtoStep {
        /**
         * the client sends hash keys
         */
        SERVER_SEND_HASH_KEYS,
    }

    /**
     * 私有构造函数
     */
    private PidBasedPmapPtoDesc() {
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
