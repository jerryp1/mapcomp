package edu.alibaba.mpc4j.s2pc.pcg.b2a;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.hardcode.HardcodeB2aTupleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.hardcode.HardcodeB2aTupleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.hardcode.HardcodeB2aTupleSender;

/**
 * Zl multiplication triple generator factory.
 *
 * @author Weiran Liu
 * @date 2023/11/21
 */
public class B2aTupleFactory implements PtoFactory {
    /**
     * private constructor
     */
    private B2aTupleFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum B2aTupleType {
        /**
         * hardcode
         */
        HARDCODE,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static B2aTupleParty createSender(Rpc senderRpc, Party receiverParty, B2aTupleConfig config) {
        B2aTupleType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case HARDCODE:
                return new HardcodeB2aTupleSender(senderRpc, receiverParty, (HardcodeB2aTupleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + B2aTupleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param aiderParty    aider party.
     * @param config        config.
     * @return a sender.
     */
    public static B2aTupleParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, B2aTupleConfig config) {
        B2aTupleType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case HARDCODE:
                return new HardcodeB2aTupleSender(senderRpc, receiverParty, aiderParty, (HardcodeB2aTupleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + B2aTupleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static B2aTupleParty createReceiver(Rpc receiverRpc, Party senderParty, B2aTupleConfig config) {
        B2aTupleType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case HARDCODE:
                return new HardcodeB2aTupleReceiver(receiverRpc, senderParty, (HardcodeB2aTupleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + B2aTupleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static B2aTupleParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, B2aTupleConfig config) {
        B2aTupleType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case HARDCODE:
                return new HardcodeB2aTupleReceiver(receiverRpc, senderParty, aiderParty, (HardcodeB2aTupleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + B2aTupleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @return a default config.
     */
    public static B2aTupleConfig createDefaultConfig(Zl zl) {
        return new HardcodeB2aTupleConfig.Builder(zl).build();
    }
}
