package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 缓存BTG协议信息。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class CacheBtgPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 2352412063583480191L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CACHE_BTG";

    /**
     * 单例模式
     */
    private static final CacheBtgPtoDesc INSTANCE = new CacheBtgPtoDesc();

    /**
     * 私有构造函数
     */
    private CacheBtgPtoDesc() {
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
