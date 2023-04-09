package edu.alibaba.mpc4j.s2pc.pcg.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class ZlEdabitPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)6265841553375232333L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ZlEdabit";

    /**
     * 单例模式
     */
    private static final ZlEdabitPtoDesc INSTANCE = new ZlEdabitPtoDesc();

    /**
     * 私有构造函数
     */
    private ZlEdabitPtoDesc() {
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
