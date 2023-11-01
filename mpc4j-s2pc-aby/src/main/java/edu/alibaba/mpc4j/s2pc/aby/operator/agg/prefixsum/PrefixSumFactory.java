package edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum.xxx23.Xxx23PrefixSumConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum.xxx23.Xxx23PrefixSumReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum.xxx23.Xxx23PrefixSumSender;

/**
 * Prefix sum factory.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class PrefixSumFactory {
    /**
     * Private constructor.
     */
    private PrefixSumFactory() {
        // empty
    }

    /**
     * prefix sum type.
     */
    public enum PrefixSumTypes {
        /**
         * Xxx+23
         */
        Xxx23,
    }

    /**
     * Creates a prefix sum aggregator.
     *
     * @return a prefix sum aggregator.
     */
    public static PrefixSumAggregator createPrefixSumSender(Rpc senderRpc, Party receiverParty, PrefixSumConfig config) {
        PrefixSumTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Xxx23:
                return new Xxx23PrefixSumSender(senderRpc, receiverParty, (Xxx23PrefixSumConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixSumTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a prefix sum aggregator.
     *
     * @return a prefix sum aggregator.
     */
    public static PrefixSumAggregator createPrefixSumReceiver(Rpc receiverRpc, Party senderParty, PrefixSumConfig config) {
        PrefixSumTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Xxx23:
                return new Xxx23PrefixSumReceiver(receiverRpc, senderParty, (Xxx23PrefixSumConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixSumTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
