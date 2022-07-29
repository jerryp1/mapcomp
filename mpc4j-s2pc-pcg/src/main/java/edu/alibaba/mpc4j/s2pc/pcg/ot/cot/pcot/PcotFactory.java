package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.bea95.Bea95PcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.bea95.Bea95PcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.bea95.Bea95PcotSender;

/**
 * PCOT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class PcotFactory {
    /**
     * 私有构造函数
     */
    private PcotFactory() {
        // empty
    }

    /**
     * PCOT协议类型
     */
    public enum PcotType {
        /**
         * Bea95协议
         */
        Bea95,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static PcotSender createSender(Rpc senderRpc, Party receiverParty, PcotConfig config) {
        PcotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea95:
                return new Bea95PcotSender(senderRpc, receiverParty, (Bea95PcotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + PcotType.class.getSimpleName() + ": " + type.name());
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
    public static PcotReceiver createReceiver(Rpc receiverRpc, Party senderParty, PcotConfig config) {
        PcotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea95:
                return new Bea95PcotReceiver(receiverRpc, senderParty, (Bea95PcotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + PcotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static PcotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Bea95PcotConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
