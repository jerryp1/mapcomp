package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.nco.NcoCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.nco.NcoCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.nco.NcoCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.rto.RtoCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.rto.RtoCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.rto.RtoCotSender;

/**
 * COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CotFactory {
    /**
     * 私有构造函数
     */
    private CotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum CotType {
        /**
         * 根协议单次调用
         */
        ROOT_ONCE,
        /**
         * 无选择协议单次调用
         */
        NO_CHOICE_ONCE,
        /**
         * 离线
         */
        OFFLINE,
        /**
         * 缓存
         */
        CACHE,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static CotSender createSender(Rpc senderRpc, Party receiverParty, CotConfig config) {
        CotType type = config.getPtoType();
        switch (type) {
            case ROOT_ONCE:
                return new RtoCotSender(senderRpc, receiverParty, (RtoCotConfig) config);
            case NO_CHOICE_ONCE:
                return new NcoCotSender(senderRpc, receiverParty, (NcoCotConfig) config);
            case CACHE:
                return new CacheCotSender(senderRpc, receiverParty, (CacheCotConfig) config);
            case OFFLINE:
            default:
                throw new IllegalArgumentException("Invalid " + CotType.class.getSimpleName() + ": " + type.name());
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
    public static CotReceiver createReceiver(Rpc receiverRpc, Party senderParty, CotConfig config) {
        CotType type = config.getPtoType();
        switch (type) {
            case ROOT_ONCE:
                return new RtoCotReceiver(receiverRpc, senderParty, (RtoCotConfig) config);
            case NO_CHOICE_ONCE:
                return new NcoCotReceiver(receiverRpc, senderParty, (NcoCotConfig) config);
            case CACHE:
                return new CacheCotReceiver(receiverRpc, senderParty, (CacheCotConfig) config);
            case OFFLINE:
            default:
                throw new IllegalArgumentException("Invalid " + CotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static CotConfig createDefaultConfig(SecurityModel securityModel) {
        return new CacheCotConfig.Builder(securityModel).build();
    }
}
