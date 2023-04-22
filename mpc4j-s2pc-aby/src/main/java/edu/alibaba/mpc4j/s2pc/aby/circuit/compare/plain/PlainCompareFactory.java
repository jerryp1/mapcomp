package edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain.fa.FullAdderPlainCompareConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain.fa.FullAdderPlainCompareSender;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * 比较协议工厂类
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/11
 */
public class PlainCompareFactory {
    /**
     * 私有构造函数
     */
    private PlainCompareFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum PlainCompareType {
        /**
         * 基于全加器的比较协议
         */
        FULL_ADDER
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static PlainCompareParty createSender(Rpc senderRpc, Party receiverParty, PlainCompareConfig config) {
        PlainCompareType type = config.getPtoType();
        switch (type) {
            case FULL_ADDER:
                return new FullAdderPlainCompareSender(senderRpc, receiverParty, (FullAdderPlainCompareConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainCompareType.class.getSimpleName() + ": " + type.name());
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
    public static PlainCompareParty createReceiver(Rpc receiverRpc, Party senderParty, PlainCompareConfig config) {
        PlainCompareType type = config.getPtoType();
        switch (type) {
            case FULL_ADDER:
                return new FullAdderPlainCompareSender(receiverRpc, senderParty, (FullAdderPlainCompareConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + HammingFactory.HammingType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static PlainCompareConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new FullAdderPlainCompareConfig.Builder()
                        .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.IDEAL, false))
                        .build();
            case SEMI_HONEST:
                return new FullAdderPlainCompareConfig.Builder()
                        .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false))
                        .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
