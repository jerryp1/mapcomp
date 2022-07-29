package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole.kos16.Kos16ShZpVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole.kos16.Kos16ShZpVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole.kos16.Kos16ShZpVoleReceiver;

/**
 * Zp域的VOLE协议工厂类。
 *
 * @author Hanwen Feng
 * @date 2022/06/07
 */
public class ZpVoleFactory {
    /**
     * 私有构造函数。
     */
    private ZpVoleFactory() {
        // empty
    }

    /**
     * 协议类型。
     */
    public enum ZpVoleType {
        /**
         * KOS16半诚实安全协议
         */
        KOS16_SEMI_HONEST,
        /**
         * KOS16恶意安全协议
         */
        KOS16_MALICIOUS,
        /**
         * WYWL21协议
         */
        WYWL21,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static ZpVoleSender createSender(Rpc senderRpc, Party receiverParty, ZpVoleConfig config) {
        ZpVoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
                return new Kos16ShZpVoleSender(senderRpc, receiverParty, (Kos16ShZpVoleConfig) config);
            case KOS16_MALICIOUS:
            case WYWL21:
            default:
                throw new IllegalArgumentException("Invalid ZpVoleType: " + type.name());
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
    public static ZpVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, ZpVoleConfig config) {
        ZpVoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
                return new Kos16ShZpVoleReceiver(receiverRpc, senderParty, (Kos16ShZpVoleConfig) config);
            case KOS16_MALICIOUS:
            case WYWL21:
            default:
                throw new IllegalArgumentException("Invalid ZpVoleType: " + type.name());
        }
    }
}
