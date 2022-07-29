package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole.kos16.Kos16ShZ2VoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole.kos16.Kos16ShZ2VoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole.kos16.Kos16ShZ2VoleSender;

/**
 * Z_2-VOLE协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public class Z2VoleFactory {
    /**
     * 私有构造函数
     */
    private Z2VoleFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Z2VoleType {
        /**
         * KOS16半诚实协议
         */
        KOS16_SEMI_HONEST,
        /**
         * KOS16恶意安全协议
         */
        KOS16_MALICIOUS,
        /**
         * WYKW21协议
         */
        WYKW21,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static Z2VoleSender createSender(Rpc senderRpc, Party receiverParty, Z2VoleConfig config) {
        Z2VoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
                return new Kos16ShZ2VoleSender(senderRpc, receiverParty, (Kos16ShZ2VoleConfig) config);
            case KOS16_MALICIOUS:
            case WYKW21:
            default:
                throw new IllegalArgumentException("Invalid Z2VoleType: " + type.name());
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
    public static Z2VoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Z2VoleConfig config) {
        Z2VoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
                return new Kos16ShZ2VoleReceiver(receiverRpc, senderParty, (Kos16ShZ2VoleConfig) config);
            case KOS16_MALICIOUS:
            case WYKW21:
            default:
                throw new IllegalArgumentException("Invalid Z2VoleType: " + type.name());
        }
    }

    /**
     * 创建根协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 根COT协议配置项。
     */
    public static Z2VoleConfig createRootConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kos16ShZ2VoleConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }

    /**
     * 创建安静协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 安静协议配置项。
     */
    public static Z2VoleConfig createSilentConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kos16ShZ2VoleConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }
}
