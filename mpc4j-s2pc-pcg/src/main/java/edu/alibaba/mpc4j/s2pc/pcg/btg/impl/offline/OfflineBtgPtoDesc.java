package edu.alibaba.mpc4j.s2pc.pcg.btg.impl.offline;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 离线BTG协议信息。
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
class OfflineBtgPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)4637156677746717932L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "OFFLINE_BTG";

    /**
     * 单例模式
     */
    private static final OfflineBtgPtoDesc INSTANCE = new OfflineBtgPtoDesc();

    /**
     * 私有构造函数
     */
    private OfflineBtgPtoDesc() {
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
