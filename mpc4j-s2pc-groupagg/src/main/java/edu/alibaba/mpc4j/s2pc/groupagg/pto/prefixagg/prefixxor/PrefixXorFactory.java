package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.php24.Php24PrefixXorConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.php24.Php24PrefixXorReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.php24.Php24PrefixXorSender;

/**
 * Prefix xor factory.
 *
 * @author Li Peng
 * @date 2024/7/19
 */
public class PrefixXorFactory {
    /**
     * Private constructor.
     */
    private PrefixXorFactory() {
        // empty
    }

    /**
     * prefix sum type.
     */
    public enum PrefixXorTypes {
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
    public static AbstractPrefixXorAggregator createPrefixXorSender(Rpc senderRpc, Party receiverParty, PrefixXorConfig config) {
        PrefixXorTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PHP24:
                return new Php24PrefixXorSender(senderRpc, receiverParty, (Php24PrefixXorConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixXorTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a prefix sum aggregator.
     *
     * @return a prefix sum aggregator.
     */
    public static AbstractPrefixXorAggregator createPrefixXorReceiver(Rpc receiverRpc, Party senderParty, PrefixXorConfig config) {
        PrefixXorTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PHP24:
                return new Php24PrefixXorReceiver(receiverRpc, senderParty, (Php24PrefixXorConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixXorTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default prefix-sum config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static PrefixXorConfig createDefaultPrefixSumConfig(SecurityModel securityModel, Zl zl, boolean silent, boolean plainOutput) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Php24PrefixXorConfig.Builder(zl, silent).setPlainOutput(plainOutput).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
