package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aSender;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aSender;

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
}
