package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.*;
import edu.alibaba.mpc4j.s2pc.opf.oprf.ra17.Ra17MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.ra17.Ra17MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.ra17.Ra17MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh.EcdhEccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh.EcdhEccSqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh.EcdhEccSqOprfSender;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class SqOprfFactory {


    /**
     * 私有构造函数
     */
    private SqOprfFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum SqOprfType {
        /**
         * 基于 ECC接口 实现 ECDH OPRF
         */
        ECDH_ECC,
    }


    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static SqOprfSender createSqOprfSender(Rpc senderRpc, Party receiverParty, SqOprfConfig config) {
        SqOprfFactory.SqOprfType type = config.getPtoType();
        switch (type) {
            case ECDH_ECC:
                return new EcdhEccSqOprfSender(senderRpc, receiverParty, (EcdhEccSqOprfConfig)config);

            default:
                throw new IllegalArgumentException("Invalid " + OprfFactory.OprfType.class.getSimpleName() + ": " + type.name());
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
    public static SqOprfReceiver createSqOprfReceiver(Rpc receiverRpc, Party senderParty, SqOprfConfig config) {
        SqOprfFactory.SqOprfType type = config.getPtoType();
        switch (type) {
            case ECDH_ECC:
                return new EcdhEccSqOprfReceiver(receiverRpc, senderParty, (EcdhEccSqOprfConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfFactory.OprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static OprfConfig createOprfDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kkrt16OptOprfConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }





}
