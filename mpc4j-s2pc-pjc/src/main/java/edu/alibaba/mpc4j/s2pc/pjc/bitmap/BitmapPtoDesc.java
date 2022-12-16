package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Bitmap协议信息。
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public class BitmapPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 139609527980746823L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "BITMAP";

    /**
     * 协议步骤
     */
    enum PtoStep {
    }

    /**
     * 单例模式
     */
    private static final BitmapPtoDesc INSTANCE = new BitmapPtoDesc();

    /**
     * 私有构造函数
     */
    private BitmapPtoDesc() {
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

    /**
     * Bitmap任务类型。
     *
     * @author Li Peng  
     * @date 2022/12/1
     */
    public enum BitmapTaskType {
        /**
         * 执行AND运算。
         */
        AND,
        /**
         * 执行OR运算。
         */
        OR,
        /**
         * 执行NOT运算。
         */
        NOT,
        /**
         * 执行COUNT运算。
         */
        COUNT,
        /**
         * 执行COUNT(DISTINCT ...)运算
         */
        COUNT_DISTINCT
    }


    /**
     * 协议类型
     */
    public enum BitmapType {
        /**
         * BITMAP
         */
        BITMAP,
    }
}
