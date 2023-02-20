package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.*;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgSender;

/**
 * 核l比特三元组生成协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlCoreMtgFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private ZlCoreMtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum ZlCoreMtgType {
        /**
         * 理想协议
         */
        IDEAL,
        /**
         * OT-based DSZ15
         */
        DSZ15_OT,
        /**
         * HE-based DSZ15
         */
        DSZ15_HE,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static ZlCoreMtgParty createSender(Rpc senderRpc, Party receiverParty, ZlCoreMtgConfig config) {
        ZlCoreMtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealZlCoreMtgSender(senderRpc, receiverParty, (IdealZlCoreMtgConfig) config);
            case DSZ15_OT:
                return new Dsz15OtZlCoreMtgSender(senderRpc, receiverParty, (Dsz15OtZlCoreMtgConfig) config);
            case DSZ15_HE:
                return new Dsz15HeZlCoreMtgSender(senderRpc, receiverParty, (Dsz15HeZlCoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCoreMtgType.class.getSimpleName() + ": " + type.name());
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
    public static ZlCoreMtgParty createReceiver(Rpc receiverRpc, Party senderParty, ZlCoreMtgConfig config) {
        ZlCoreMtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealZlCoreMtgReceiver(receiverRpc, senderParty, (IdealZlCoreMtgConfig) config);
            case DSZ15_OT:
                return new Dsz15OtZlCoreMtgReceiver(receiverRpc, senderParty, (Dsz15OtZlCoreMtgConfig) config);
            case DSZ15_HE:
                return new Dsz15HeZlCoreMtgReceiver(receiverRpc, senderParty, (Dsz15HeZlCoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCoreMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认配置项。
     *
     * @param l 乘法三元组比特长度。
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static ZlCoreMtgConfig createDefaultConfig(SecurityModel securityModel, int l) {
        switch (securityModel) {
            case IDEAL:
                return new IdealZlCoreMtgConfig.Builder(l).build();
            case SEMI_HONEST:
                return new Dsz15OtZlCoreMtgConfig.Builder(l).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
