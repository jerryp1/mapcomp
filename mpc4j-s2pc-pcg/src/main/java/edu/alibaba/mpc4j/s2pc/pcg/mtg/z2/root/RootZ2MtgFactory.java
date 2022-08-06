package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.alsz13.Alsz13RootZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.alsz13.Alsz13RootZ2MtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.alsz13.Alsz13RootZ2MtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.ideal.IdealRootZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.ideal.IdealRootZ2MtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.ideal.IdealRootZ2MtgSender;

/**
 * RBTG协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class RootZ2MtgFactory {
    /**
     * 私有构造函数
     */
    private RootZ2MtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum RbtgType {
        /**
         * 理想协议
         */
        IDEAL,
        /**
         * ALSZ13协议
         */
        ALSZ13,
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
    public static RootZ2MtgParty createSender(Rpc senderRpc, Party receiverParty, RootZ2MtgConfig config) {
        RbtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealRootZ2MtgSender(senderRpc, receiverParty, (IdealRootZ2MtgConfig) config);
            case ALSZ13:
                return new Alsz13RootZ2MtgSender(senderRpc, receiverParty, (Alsz13RootZ2MtgConfig) config);
            case WYKW21:
            default:
                throw new IllegalArgumentException("Invalid " + RbtgType.class.getSimpleName() + ": " + type.name());
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
    public static RootZ2MtgParty createReceiver(Rpc receiverRpc, Party senderParty, RootZ2MtgConfig config) {
        RbtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealRootZ2MtgReceiver(receiverRpc, senderParty, (IdealRootZ2MtgConfig) config);
            case ALSZ13:
                return new Alsz13RootZ2MtgReceiver(receiverRpc, senderParty, (Alsz13RootZ2MtgConfig) config);
            case WYKW21:
            default:
                throw new IllegalArgumentException("Invalid " + RbtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static RootZ2MtgConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new IdealRootZ2MtgConfig.Builder().build();
            case SEMI_HONEST:
                return new Alsz13RootZ2MtgConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
