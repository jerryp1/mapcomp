package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole.kos16.Kos16ShZ2RvoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole.kos16.Kos16ShZ2RvoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole.kos16.Kos16ShZ2RvoleSender;

/**
 * Z_2-RVOLE协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public class Z2RvoleFactory {
    /**
     * 私有构造函数
     */
    private Z2RvoleFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Z2RvoleType {
        /**
         * KOS16半诚实协议
         */
        KOS16_SEMI_HONEST,
        /**
         * KOS16恶意安全协议
         */
        KOS16_MALICIOUS,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static Z2RvoleSender createSender(Rpc senderRpc, Party receiverParty, Z2RvoleConfig config) {
        Z2RvoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
                return new Kos16ShZ2RvoleSender(senderRpc, receiverParty, (Kos16ShZ2RvoleConfig) config);
            case KOS16_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + Z2RvoleType.class.getSimpleName() + ": " + type.name());
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
    public static Z2RvoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Z2RvoleConfig config) {
        Z2RvoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
                return new Kos16ShZ2RvoleReceiver(receiverRpc, senderParty, (Kos16ShZ2RvoleConfig) config);
            case KOS16_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + Z2RvoleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 根COT协议配置项。
     */
    public static Z2RvoleConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kos16ShZ2RvoleConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }
}
