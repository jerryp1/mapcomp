package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.xxx23.Xxx23PrefixMaxConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.xxx23.Xxx23PrefixMaxReceiver;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.xxx23.Xxx23PrefixMaxSender;

/**
 * Prefix max factory.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class PrefixMaxFactory {
    /**
     * Private constructor.
     */
    private PrefixMaxFactory() {
        // empty
    }

    /**
     * prefix max type.
     */
    public enum PrefixMaxTypes {
        /**
         * Xxx+23
         */
        Xxx23,
    }

    /**
     * Creates a prefix max aggregator.
     *
     * @return a prefix max aggregator.
     */
    public static AbstractPrefixMaxAggregator createPrefixMaxSender(Rpc senderRpc, Party receiverParty, PrefixMaxConfig config) {
        PrefixMaxTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Xxx23:
                return new Xxx23PrefixMaxSender(senderRpc, receiverParty, (Xxx23PrefixMaxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixMaxTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a prefix max aggregator.
     *
     * @return a prefix max aggregator.
     */
    public static AbstractPrefixMaxAggregator createPrefixMaxReceiver(Rpc receiverRpc, Party senderParty, PrefixMaxConfig config) {
        PrefixMaxTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Xxx23:
                return new Xxx23PrefixMaxReceiver(receiverRpc, senderParty, (Xxx23PrefixMaxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixMaxTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
