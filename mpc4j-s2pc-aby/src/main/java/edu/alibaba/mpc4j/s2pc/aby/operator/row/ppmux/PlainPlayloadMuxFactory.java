package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.rrg21.Xxx23PlainPayloadMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.rrg21.Xxx23PlainPayloadMuxReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.rrg21.Xxx23PlainPayloadMuxSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Plain mux factory.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public class PlainPlayloadMuxFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PlainPlayloadMuxFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum PlainMuxType {
        /**
         * Xxx23
         */
        Xxx23,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PlainPayloadMuxParty createSender(Rpc senderRpc, Party receiverParty, PlainPayloadMuxConfig config) {
        PlainMuxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Xxx23:
                return new Xxx23PlainPayloadMuxSender(senderRpc, receiverParty, (Xxx23PlainPayloadMuxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainMuxType.class.getSimpleName() + ": " + type.name());
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
    public static PlainPayloadMuxParty createReceiver(Rpc receiverRpc, Party senderParty, PlainPayloadMuxConfig config) {
        PlainMuxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Xxx23:
                return new Xxx23PlainPayloadMuxReceiver(receiverRpc, senderParty, (Xxx23PlainPayloadMuxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainMuxType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static PlainPayloadMuxConfig createDefaultConfig(SecurityModel securityModel, Zl zl, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Xxx23PlainPayloadMuxConfig.Builder(zl)
                    .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
