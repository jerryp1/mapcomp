package edu.alibaba.mpc4j.s2pc.aby.basics.b2a;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15.Dsz15B2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15.Dsz15B2aReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15.Dsz15B2aSender;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.tuple.TupleB2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.tuple.TupleB2aReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.tuple.TupleB2aSender;

/**
 * B2a Factory.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public class B2aFactory {
    /**
     * Private constructor.
     */
    private B2aFactory() {
        // empty
    }

    /**
     * B2a type enums.
     */
    public enum B2aTypes {
        /**
         * DSZ15.
         */
        DSZ15,
        /**
         * Using b2a tuple
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
    public static B2aParty createSender(Rpc senderRpc, Party receiverParty, B2aConfig config) {
        B2aTypes type = config.getPtoType();
        switch (type) {
            case DSZ15:
                return new Dsz15B2aSender(senderRpc, receiverParty, (Dsz15B2aConfig) config);
            case TUPLE:
                return new TupleB2aSender(senderRpc,receiverParty,(TupleB2aConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + B2aTypes.class.getSimpleName() + ": " + type.name());
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
    public static B2aParty createReceiver(Rpc receiverRpc, Party senderParty, B2aConfig config) {
        B2aTypes type = config.getPtoType();
        switch (type) {
            case DSZ15:
                return new Dsz15B2aReceiver(receiverRpc, senderParty, (Dsz15B2aConfig) config);
            case TUPLE:
                return new TupleB2aReceiver(receiverRpc,senderParty,(TupleB2aConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + B2aTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param zl zl.
     * @return a default config.
     */
    public static B2aConfig createDefaultConfig(SecurityModel securityModel, Zl zl, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new Dsz15B2aConfig.Builder(zl, silent).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
