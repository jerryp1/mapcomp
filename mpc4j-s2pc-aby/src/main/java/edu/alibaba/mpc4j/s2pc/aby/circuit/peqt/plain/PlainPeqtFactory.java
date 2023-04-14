package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22.Cgs22PlainPeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22.Cgs22PlainPeqtReceiver;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22.Cgs22PlainPeqtSender;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.naive.NaivePlainPeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.naive.NaivePlainPeqtReceiver;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.naive.NaivePlainPeqtSender;

/**
 * plain private equality test factory.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class PlainPeqtFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PlainPeqtFactory() {
        // empty
    }

    /**
     * type
     */
    public enum PlainPeqtType {
        /**
         * naive implementation, bit-wise operations.
         */
        NAIVE,
        /**
         * CGS22
         */
        CGS22,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PlainPeqtParty createSender(Rpc senderRpc, Party receiverParty, PlainPeqtConfig config) {
        PlainPeqtType type = config.getPtoType();
        switch (type) {
            case NAIVE:
                return new NaivePlainPeqtSender(senderRpc, receiverParty, (NaivePlainPeqtConfig) config);
            case CGS22:
                return new Cgs22PlainPeqtSender(senderRpc, receiverParty, (Cgs22PlainPeqtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainPeqtType.class.getSimpleName() + ": " + type.name());
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
    public static PlainPeqtParty createReceiver(Rpc receiverRpc, Party senderParty, PlainPeqtConfig config) {
        PlainPeqtType type = config.getPtoType();
        switch (type) {
            case NAIVE:
                return new NaivePlainPeqtReceiver(receiverRpc, senderParty, (NaivePlainPeqtConfig) config);
            case CGS22:
                return new Cgs22PlainPeqtReceiver(receiverRpc, senderParty, (Cgs22PlainPeqtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainPeqtType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates the default config.
     *
     * @param securityModel the security model.
     * @return the default config.
     */
    public static PlainPeqtConfig createDefaultConfig(SecurityModel securityModel) {
        return new NaivePlainPeqtConfig.Builder(securityModel).build();
    }
}
