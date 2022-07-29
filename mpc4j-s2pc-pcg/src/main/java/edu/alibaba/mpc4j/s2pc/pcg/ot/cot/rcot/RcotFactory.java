package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.alsz13.Alsz13RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.alsz13.Alsz13RcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.alsz13.Alsz13RcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.iknp03.Iknp03RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.iknp03.Iknp03RcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.iknp03.Iknp03RcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.kos15.Kos15RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.kos15.Kos15RcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.kos15.Kos15RcotSender;

/**
 * RCOT协议工厂。
 *
 * @author Weiran Liu
 * @date 2021/01/29
 */
public class RcotFactory {
    /**
     * 私有构造函数
     */
    private RcotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum RcotType {
        /**
         * IKNP03协议
         */
        IKNP03,
        /**
         * ALSZ13协议
         */
        ALSZ13,
        /**
         * KOS15协议
         */
        KOS15,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static RcotSender createSender(Rpc senderRpc, Party receiverParty, RcotConfig config) {
        RcotType type = config.getPtoType();
        switch (type) {
            case IKNP03:
                return new Iknp03RcotSender(senderRpc, receiverParty, (Iknp03RcotConfig) config);
            case ALSZ13:
                return new Alsz13RcotSender(senderRpc, receiverParty, (Alsz13RcotConfig) config);
            case KOS15:
                return new Kos15RcotSender(senderRpc, receiverParty, (Kos15RcotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + RcotType.class.getSimpleName() + ": " + type.name());
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
    public static RcotReceiver createReceiver(Rpc receiverRpc, Party senderParty, RcotConfig config) {
        RcotType type = config.getPtoType();
        switch (type) {
            case IKNP03:
                return new Iknp03RcotReceiver(receiverRpc, senderParty, (Iknp03RcotConfig) config);
            case ALSZ13:
                return new Alsz13RcotReceiver(receiverRpc, senderParty, (Alsz13RcotConfig) config);
            case KOS15:
                return new Kos15RcotReceiver(receiverRpc, senderParty, (Kos15RcotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + RcotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static RcotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Alsz13RcotConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Kos15RcotConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
