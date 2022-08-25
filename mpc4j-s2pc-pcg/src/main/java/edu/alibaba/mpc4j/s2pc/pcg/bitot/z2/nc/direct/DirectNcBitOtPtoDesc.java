package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * 直接通过截取COT输出的第一个比特构造BitOT。
 *
 * @author Hanwen Feng
 * @date 2022/08/11
 */
class DirectNcBitOtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 1726912431942336863L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "DIRECT_NC_BitOT";

    /**
     * 单例模式
     */
    private static final DirectNcBitOtPtoDesc INSTANCE = new DirectNcBitOtPtoDesc();


    /**
     * 私有构造函数
     */
    private DirectNcBitOtPtoDesc() {
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
