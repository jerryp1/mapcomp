package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.liu22.Liu22BitmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.liu22.Liu22BitmapReceiver;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.liu22.Liu22BitmapSender;

/**
 * Bitmap工厂类
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public class BitmapFactory {
    /**
     * 私有构造函数
     */
    private BitmapFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum BitmapType {
        /**
         * Beaver91协议
         */
        LIU22,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static BitmapParty createSender(Rpc senderRpc, Party receiverParty, BitmapConfig config) {
        BitmapType type = config.getPtoType();
        switch (type) {
            case LIU22:
                return new Liu22BitmapSender(senderRpc, receiverParty, (Liu22BitmapConfig) config);
            default:
                throw new IllegalArgumentException("Invalid BitmapType: " + type.name());
        }
    }

    /**
     * 构建接收方。
     *
     * @param receiverRpc 接收方通信接口。
     * @param senderParty 发送方信息。
     * @param config      配置项。
     * @return 接收方。
     */
    public static BitmapParty createReceiver(Rpc receiverRpc, Party senderParty, BitmapConfig config) {
        BitmapType type = config.getPtoType();
        switch (type) {
            case LIU22:
                return new Liu22BitmapReceiver(receiverRpc, senderParty, (Liu22BitmapConfig) config);
            default:
                throw new IllegalArgumentException("Invalid BitmapType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static BitmapConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new Liu22BitmapConfig.Builder()
                        .setBcConfig(BcFactory.createDefaultConfig(SecurityModel.IDEAL))
                        .build();
            case SEMI_HONEST:
                return new Liu22BitmapConfig.Builder()
                        .setBcConfig(BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                        .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }
}
