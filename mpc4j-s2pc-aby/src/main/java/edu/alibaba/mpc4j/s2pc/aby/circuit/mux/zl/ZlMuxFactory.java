package edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.rrk20.Rrk20ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.rrk20.Rrk20ZlMuxReceiver;
import edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.rrk20.Rrk20ZlMuxSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Zl mux factory.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class ZlMuxFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlMuxFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlMuxType {
        /**
         * RRK+20
         */
        RRK20,
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
    public static ZlMuxParty createSender(Rpc senderRpc, Party receiverParty, ZlMuxConfig config) {
        ZlMuxType type = config.getPtoType();
        switch (type) {
            case RRK20:
                return new Rrk20ZlMuxSender(senderRpc, receiverParty, (Rrk20ZlMuxConfig) config);
            case RRG21:
            default:
                throw new IllegalArgumentException("Invalid " + ZlMuxType.class.getSimpleName() + ": " + type.name());
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
    public static ZlMuxParty createReceiver(Rpc receiverRpc, Party senderParty, ZlMuxConfig config) {
        ZlMuxType type = config.getPtoType();
        switch (type) {
            case RRK20:
                return new Rrk20ZlMuxReceiver(receiverRpc, senderParty, (Rrk20ZlMuxConfig) config);
            case RRG21:
            default:
                throw new IllegalArgumentException("Invalid " + ZlMuxType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates the default config.
     *
     * @param securityModel the security model.
     * @return the default config.
     */
    public static ZlMuxConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new Rrk20ZlMuxConfig.Builder()
                    .setCotConfig(CotFactory.createDirectConfig(SecurityModel.IDEAL))
                    .build();
            case SEMI_HONEST:
                return new Rrk20ZlMuxConfig.Builder()
                    .setCotConfig(CotFactory.createDirectConfig(SecurityModel.SEMI_HONEST))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
