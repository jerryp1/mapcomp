package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k.ywl20.Ywl20Gf2kDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k.ywl20.Ywl20Gf2kDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k.ywl20.Ywl20Gf2kDpprfSender;

/**
 * GF2K-DPPRF工厂。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Gf2kDpprfFactory {
    /**
     * 私有构造函数
     */
    private Gf2kDpprfFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Gf2kDpprfType {
        /**
         * BCG19协议
         */
        BCG19,
        /**
         * YWL20协议
         */
        YWL20,
    }

    /**
     * 返回执行协议所需的预计算数量。
     *
     * @param config     配置项。
     * @param batchNum   批处理数量。
     * @param alphaBound α上界。
     * @return 预计算数量。
     */
    public static int getPrecomputeNum(Gf2kDpprfConfig config, int batchNum, int alphaBound) {
        assert batchNum > 0 : "BatchNum must be greater than 0: " + batchNum;
        assert alphaBound > 0 : "alphaBound must be greater than 0: " + alphaBound;
        Gf2kDpprfType type = config.getPtoType();
        switch (type) {
            case YWL20:
                return LongUtils.ceilLog2(alphaBound) * batchNum;
            case BCG19:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kDpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static Gf2kDpprfSender createSender(Rpc senderRpc, Party receiverParty, Gf2kDpprfConfig config) {
        Gf2kDpprfType type = config.getPtoType();
        switch (type) {
            case YWL20:
                return new Ywl20Gf2kDpprfSender(senderRpc, receiverParty, (Ywl20Gf2kDpprfConfig) config);
            case BCG19:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kDpprfType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kDpprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kDpprfConfig config) {
        Gf2kDpprfType type = config.getPtoType();
        switch (type) {
            case YWL20:
                return new Ywl20Gf2kDpprfReceiver(receiverRpc, senderParty, (Ywl20Gf2kDpprfConfig) config);
            case BCG19:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kDpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static Gf2kDpprfConfig createDefaultConfig(SecurityModel securityModel) {
        return new Ywl20Gf2kDpprfConfig.Builder(securityModel).build();
    }
}
