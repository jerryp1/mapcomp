package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.direct.DirectNcBitOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.direct.DirectNcBitOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.direct.DirectNcBitOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.kk13.Kk13NcBitOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.kk13.Kk13NcBitOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.kk13.Kk13NcBitOtSender;

/**
 * NC-BitOT协议工厂。
 *
 * @author Hanwen Feng
 * @date 2022/08/10
 */
public class NcBitOtFactory {
    /**
     * 私有构造函数。
     */
    private NcBitOtFactory() {
        // empty
    }

    /**
     * 协议类型。
     */
    public enum NcBitOtType {
        /**
         * 直接协议
         */
        DIRECT,
        /**
         * KK13协议
         */
        KK13,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc 发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config 配置项。
     * @return 发送方。
     */
    public static NcBitOtSender createSender(Rpc senderRpc, Party receiverParty, NcBitOtConfig config) {
        NcBitOtType type = config.getPtoType();
        switch (type) {
            case KK13:
                return new Kk13NcBitOtSender(senderRpc, receiverParty, (Kk13NcBitOtConfig) config);
            case DIRECT:
                return new DirectNcBitOtSender(senderRpc, receiverParty, (DirectNcBitOtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + NcBitOtType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建接收方。
     *
     * @param receiverRpc 接收方通信接口。
     * @param senderParty 发送方信息。
     * @param config 配置项。
     * @return 发送方。
     */
    public static NcBitOtReceiver createReceiver(Rpc receiverRpc, Party senderParty, NcBitOtConfig config) {
        NcBitOtType type = config.getPtoType();
        switch (type) {
            case KK13:
                return new Kk13NcBitOtReceiver(receiverRpc, senderParty, (Kk13NcBitOtConfig) config);
            case DIRECT:
                return new DirectNcBitOtReceiver(receiverRpc, senderParty, (DirectNcBitOtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + NcBitOtType.class.getSimpleName() + ": " + type.name());
        }
    }
}
