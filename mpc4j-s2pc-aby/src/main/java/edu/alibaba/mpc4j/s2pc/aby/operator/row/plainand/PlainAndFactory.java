package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.php24.Php24PlainAndConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.php24.Php24PlainAndReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.php24.Php24PlainAndSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Plain and factory.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public class PlainAndFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PlainAndFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum PlainAndType {
        /**
         * Php+24
         */
        PHP24,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PlainAndParty createSender(Rpc senderRpc, Party receiverParty, PlainAndConfig config) {
        PlainAndType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PHP24:
                return new Php24PlainAndSender(senderRpc, receiverParty, (Php24PlainAndConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainAndType.class.getSimpleName() + ": " + type.name());
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
    public static PlainAndParty createReceiver(Rpc receiverRpc, Party senderParty, PlainAndConfig config) {
        PlainAndType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PHP24:
                return new Php24PlainAndReceiver(receiverRpc, senderParty, (Php24PlainAndConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PlainAndType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static PlainAndConfig createDefaultConfig(SecurityModel securityModel, Zl zl, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24PlainAndConfig.Builder(zl, silent)
                    .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
