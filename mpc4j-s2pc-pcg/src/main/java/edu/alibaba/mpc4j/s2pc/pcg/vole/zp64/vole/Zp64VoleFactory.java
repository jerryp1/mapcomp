package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.vole;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.vole.kos16.Kos16ShZp64VoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.vole.kos16.Kos16ShZp64VoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.vole.kos16.Kos16ShZp64VoleSender;

/**
 * Zp64域的VOLE协议工厂类。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public class Zp64VoleFactory {
    /**
     * 私有构造函数。
     */
    private Zp64VoleFactory() {
        // empty
    }

    /**
     * 协议类型。
     */
    public enum Zp64VoleType {
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
    public static Zp64VoleSender createSender(Rpc senderRpc, Party receiverParty, Zp64VoleConfig config) {
        Zp64VoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
                return new Kos16ShZp64VoleSender(senderRpc, receiverParty, (Kos16ShZp64VoleConfig) config);
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
    public static Zp64VoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Zp64VoleConfig config) {
        Zp64VoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
                return new Kos16ShZp64VoleReceiver(receiverRpc, senderParty, (Kos16ShZp64VoleConfig) config);
            case KOS16_MALICIOUS:
            case WYWL21:
            default:
                throw new IllegalArgumentException("Invalid ZpVoleType: " + type.name());
        }
    }
}
