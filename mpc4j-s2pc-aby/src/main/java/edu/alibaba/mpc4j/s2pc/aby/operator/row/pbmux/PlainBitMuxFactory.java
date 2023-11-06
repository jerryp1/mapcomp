package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.Xxx23.Xxx23PlainBitMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.Xxx23.Xxx23PlainBitMuxReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.Xxx23.Xxx23PlainBitMuxSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Plain bit mux factory.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public class PlainBitMuxFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PlainBitMuxFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum PlainBitMuxType {
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
    public static PlainBitMuxParty createSender(Rpc senderRpc, Party receiverParty, PlainBitMuxConfig config) {
        PlainBitMuxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Xxx23:
                return new Xxx23PlainBitMuxSender(senderRpc, receiverParty, (Xxx23PlainBitMuxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainBitMuxType.class.getSimpleName() + ": " + type.name());
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
    public static PlainBitMuxParty createReceiver(Rpc receiverRpc, Party senderParty, PlainBitMuxConfig config) {
        PlainBitMuxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Xxx23:
                return new Xxx23PlainBitMuxReceiver(receiverRpc, senderParty, (Xxx23PlainBitMuxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainBitMuxType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static PlainBitMuxConfig createDefaultConfig(SecurityModel securityModel, Zl zl, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Xxx23PlainBitMuxConfig.Builder(zl)
                    .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
