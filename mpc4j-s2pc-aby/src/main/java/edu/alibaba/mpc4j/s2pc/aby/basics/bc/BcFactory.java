package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91.Bea91BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91.Bea91BcReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91.Bea91BcSender;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.rrg21.Rrg21BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.rrg21.Rrg21BcReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.rrg21.Rrg21BcSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * Boolean circuit factory.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public class BcFactory implements PtoFactory {
    /**
     * private constructor
     */
    private BcFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum BcType {
        /**
         * Bea91
         */
        Bea91,
        /**
         * RRG+21
         */
        RRG21,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static BcParty createSender(Rpc senderRpc, Party receiverParty, BcConfig config) {
        BcType type = config.getPtoType();
        switch (type) {
            case Bea91:
                return new Bea91BcSender(senderRpc, receiverParty, (Bea91BcConfig) config);
            case RRG21:
                return new Rrg21BcSender(senderRpc, receiverParty, (Rrg21BcConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BcType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static BcParty createReceiver(Rpc receiverRpc, Party senderParty, BcConfig config) {
        BcType type = config.getPtoType();
        switch (type) {
            case Bea91:
                return new Bea91BcReceiver(receiverRpc, senderParty, (Bea91BcConfig) config);
            case RRG21:
                return new Rrg21BcReceiver(receiverRpc, senderParty, (Rrg21BcConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BcType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static BcConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new Bea91BcConfig.Builder()
                    .setZ2MtgConfig(Z2MtgFactory.createDefaultConfig(SecurityModel.IDEAL))
                    .build();
            case SEMI_HONEST:
                return new Bea91BcConfig.Builder()
                    .setZ2MtgConfig(Z2MtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
