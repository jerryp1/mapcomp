package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.rrk20.Rrk20Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.rrk20.Rrk20Z2MuxReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.rrk20.Rrk20Z2MuxSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

public class Z2MuxFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Z2MuxFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum Z2MuxType {
        /**
         * RRK+20
         */
        RRK20,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static Z2MuxParty createSender(Rpc senderRpc, Party receiverParty, Z2MuxConfig config) {
        Z2MuxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20Z2MuxSender(senderRpc, receiverParty, (Rrk20Z2MuxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2MuxType.class.getSimpleName() + ": " + type.name());
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
    public static Z2MuxParty createReceiver(Rpc receiverRpc, Party senderParty, Z2MuxConfig config) {
        Z2MuxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20Z2MuxReceiver(receiverRpc, senderParty, (Rrk20Z2MuxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2MuxType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static Z2MuxConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Rrk20Z2MuxConfig.Builder(silent)
                    .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
