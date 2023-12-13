package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.php24.Php24PrefixSumConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.php24.Php24PrefixSumReceiver;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.php24.Php24PrefixSumSender;

/**
 * Prefix sum factory.
 *
 * @author Li Peng
 * @date 2023/10/30
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
         * Php+24
         */
        PHP24,
    }

    /**
     * Creates a prefix sum aggregator.
     *
     * @return a prefix sum aggregator.
     */
    public static AbstractPrefixSumAggregator createPrefixSumSender(Rpc senderRpc, Party receiverParty, PrefixSumConfig config) {
        PrefixSumTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PHP24:
                return new Php24PrefixSumSender(senderRpc, receiverParty, (Php24PrefixSumConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixSumTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a prefix sum aggregator.
     *
     * @return a prefix sum aggregator.
     */
    public static AbstractPrefixSumAggregator createPrefixSumReceiver(Rpc receiverRpc, Party senderParty, PrefixSumConfig config) {
        PrefixSumTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PHP24:
                return new Php24PrefixSumReceiver(receiverRpc, senderParty, (Php24PrefixSumConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixSumTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default prefix-sum config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static PrefixSumConfig createDefaultPrefixSumConfig(SecurityModel securityModel, Zl zl, boolean silent,boolean plainOutput) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24PrefixSumConfig.Builder(zl, silent).setPlainOutput(plainOutput).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
