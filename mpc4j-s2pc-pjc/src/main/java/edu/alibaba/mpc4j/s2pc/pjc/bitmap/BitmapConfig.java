package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingConfig;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapPtoDesc.BitmapType;

/**
 * Bitmap配置类
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public interface BitmapConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BitmapType getPtoType();

    /**
     * 返回底层协议最大数量。
     *
     * @return 底层协议最大数量。
     */
    int maxBaseNum();

    /**
     * 返回Bc协议配置项
     *
     * @return Bc协议配置项
     */
    BcConfig getBcConfig();

    /**
     * 返回汉明距离协议配置项
     *
     * @return 汉明距离协议配置项
     */
    HammingConfig getHammingConfig();
}
