package edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.alsz13.Alsz13RbtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.alsz13.Alsz13RbtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.alsz13.Alsz13RbtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.ideal.IdealRbtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.ideal.IdealRbtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.ideal.IdealRbtgSender;

/**
 * RBTG协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class RbtgFactory {
    /**
     * 私有构造函数
     */
    private RbtgFactory() {
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
    public static RbtgParty createSender(Rpc senderRpc, Party receiverParty, RbtgConfig config) {
        RbtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealRbtgSender(senderRpc, receiverParty, (IdealRbtgConfig) config);
            case ALSZ13:
                return new Alsz13RbtgSender(senderRpc, receiverParty, (Alsz13RbtgConfig) config);
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
    public static RbtgParty createReceiver(Rpc receiverRpc, Party senderParty, RbtgConfig config) {
        RbtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealRbtgReceiver(receiverRpc, senderParty, (IdealRbtgConfig) config);
            case ALSZ13:
                return new Alsz13RbtgReceiver(receiverRpc, senderParty, (Alsz13RbtgConfig) config);
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
    public static RbtgConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new IdealRbtgConfig.Builder().build();
            case SEMI_HONEST:
                return new Alsz13RbtgConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
