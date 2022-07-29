package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.nco;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 无选择单次调用COT（No-Choice Once COT, NCO-COT）协议信息。
 *
 * @author Weiran Liu
 * @date 2022/07/13
 */
class NcoCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 5660276397626647391L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "NO_CHOICE_ONCE_COT";

    /**
     * 单例模式
     */
    private static final NcoCotPtoDesc INSTANCE = new NcoCotPtoDesc();

    /**
     * 私有构造函数
     */
    private NcoCotPtoDesc() {
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
