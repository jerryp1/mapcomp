package edu.alibaba.mpc4j.s2pc.pjc.bitmap.liu22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Liu22-Bitmap协议信息。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public class Liu22BitmapPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 139609527980746823L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "LIU22_BITMAP";

    /**
     * 协议步骤
     */
    enum PtoStep {
    }

    /**
     * 单例模式
     */
    private static final Liu22BitmapPtoDesc INSTANCE = new Liu22BitmapPtoDesc();

    /**
     * 私有构造函数
     */
    private Liu22BitmapPtoDesc() {
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
