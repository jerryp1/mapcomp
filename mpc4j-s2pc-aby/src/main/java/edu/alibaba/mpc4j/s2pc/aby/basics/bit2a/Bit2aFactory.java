package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aSender;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aSender;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * Bit2a Factory.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public class Bit2aFactory {
    /**
     * Private constructor.
     */
    private Bit2aFactory() {
        // empty
    }

    /**
     * Bit2a type enums.
     */
    public enum Bit2aTypes {
        /**
         * KVH+21.
         */
        KVH21,
        /**
         * Using tuple.
         */
        TUPLE,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static Bit2aParty createSender(Rpc senderRpc, Party receiverParty, Bit2aConfig config) {
        Bit2aFactory.Bit2aTypes type = config.getPtoType();
        switch (type) {
            case KVH21:
                return new Kvh21Bit2aSender(senderRpc, receiverParty, (Kvh21Bit2aConfig) config);
            case TUPLE:
                return new TupleBit2aSender(senderRpc, receiverParty, (TupleBit2aConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Bit2aTypes.class.getSimpleName() + ": " + type.name());
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
    public static Bit2aParty createReceiver(Rpc receiverRpc, Party senderParty, Bit2aConfig config) {
        Bit2aFactory.Bit2aTypes type = config.getPtoType();
        switch (type) {
            case KVH21:
                return new Kvh21Bit2aReceiver(receiverRpc, senderParty, (Kvh21Bit2aConfig) config);
            case TUPLE:
                return new TupleBit2aReceiver(receiverRpc, senderParty, (TupleBit2aConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Bit2aTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @return a default config.
     */
    public static Bit2aConfig createDefaultConfig(SecurityModel securityModel, Zl zl,boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new TupleBit2aConfig.Builder(zl, silent).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
