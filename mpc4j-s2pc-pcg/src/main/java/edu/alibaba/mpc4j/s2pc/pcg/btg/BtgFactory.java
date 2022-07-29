package edu.alibaba.mpc4j.s2pc.pcg.btg;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.cache.CacheBtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.cache.CacheBtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.cache.CacheBtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.file.FileBtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.file.FileBtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.file.FileBtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.offline.OfflineBtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.offline.OfflineBtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.offline.OfflineBtgSender;

/**
 * BTG协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/02/07
 */
public class BtgFactory {
    /**
     * 私有构造函数
     */
    private BtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum BtgType {
        /**
         * 文件
         */
        FILE,
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
    public static BtgParty createSender(Rpc senderRpc, Party receiverParty, BtgConfig config) {
        BtgType type = config.getPtoType();
        switch (type) {
            case FILE:
                return new FileBtgSender(senderRpc, receiverParty, (FileBtgConfig) config);
            case OFFLINE:
                return new OfflineBtgSender(senderRpc, receiverParty, (OfflineBtgConfig) config);
            case CACHE:
                return new CacheBtgSender(senderRpc, receiverParty, (CacheBtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid BtgType: " + type.name());
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
    public static BtgParty createReceiver(Rpc receiverRpc, Party senderParty, BtgConfig config) {
        BtgType type = config.getPtoType();
        switch (type) {
            case FILE:
                return new FileBtgReceiver(receiverRpc, senderParty, (FileBtgConfig) config);
            case OFFLINE:
                return new OfflineBtgReceiver(receiverRpc, senderParty, (OfflineBtgConfig) config);
            case CACHE:
                return new CacheBtgReceiver(receiverRpc, senderParty, (CacheBtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid BtgType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static BtgConfig createDefaultConfig(SecurityModel securityModel) {
        return new CacheBtgConfig.Builder(securityModel).build();
    }
}
