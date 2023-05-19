package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingConfig;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapPtoDesc.BitmapType;

/**
 * Bitmap配置类
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public interface BitmapConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BitmapType getPtoType();

    /**
     * 返回Bc协议配置项
     *
     * @return Bc协议配置项
     */
    Z2cConfig getZ2cConfig();

    /**
     * 返回汉明距离协议配置项
     *
     * @return 汉明距离协议配置项
     */
    HammingConfig getHammingConfig();
}
