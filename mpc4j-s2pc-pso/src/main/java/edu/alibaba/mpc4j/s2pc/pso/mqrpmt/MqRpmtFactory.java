package edu.alibaba.mpc4j.s2pc.pso.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.gmr21.Gmr21MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.gmr21.Gmr21MqRpmtServer;

/**
 * Multi-Query RPMT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public class MqRpmtFactory {
    /**
     * 私有构造函数
     */
    private MqRpmtFactory() {
        // empty
    }

    /**
     * PSU协议类型。
     */
    public enum MqRpmtType {
        /**
         * GMR21方案
         */
        GMR21,
        /**
         * JSZ22置乱客户端方案
         */
        JSZ22_SFC,
        /**
         * ZCL22_PKE方案
         */
        ZCL22_PKE,
        /**
         * ZCL22_SKE方案
         */
        ZCL22_SKE,
        /**
         * CZZ22_CW_PRF方案
         */
        CZZ22_CW_PRF,
        /**
         * CZZ22_PO_PRF方案
         */
        CZZ22_PO_PRF,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static MqRpmtServer createServer(Rpc serverRpc, Party clientParty, MqRpmtConfig config) {
        MqRpmtType type = config.getPtoType();
        switch (type) {
            case GMR21:
                return new Gmr21MqRpmtServer(serverRpc, clientParty, (Gmr21MqRpmtConfig) config);
            case ZCL22_SKE:
            case ZCL22_PKE:
            case JSZ22_SFC:
            case CZZ22_CW_PRF:
            case CZZ22_PO_PRF:
            default:
                throw new IllegalArgumentException("Invalid " + MqRpmtType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建客户端。
     *
     * @param clientRpc 客户端通信接口。
     * @param serverParty 服务端信息。
     * @param config      配置项。
     * @return 客户端。
     */
    public static MqRpmtClient createClient(Rpc clientRpc, Party serverParty, MqRpmtConfig config) {
        MqRpmtType type = config.getPtoType();
        switch (type) {
            case GMR21:
                return new Gmr21MqRpmtClient(clientRpc, serverParty, (Gmr21MqRpmtConfig)config);
            case ZCL22_SKE:
            case ZCL22_PKE:
            case JSZ22_SFC:
            case CZZ22_CW_PRF:
            case CZZ22_PO_PRF:
            default:
                throw new IllegalArgumentException("Invalid " + MqRpmtType.class.getSimpleName() + ": " + type.name());
        }
    }
}
