package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.PrefixMaxConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.PrefixMaxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixsum.PrefixSumConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixsum.PrefixSumFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixsum.PrefixSumFactory.PrefixSumTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.PrefixXorConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.PrefixXorFactory;

public class PrefixAggFactory {

    /**
     * prefix agg type.
     */
    public enum PrefixAggTypes {
        /**
         * Summation.
         */
        SUM,
        /**
         * Maximum.
         */
        MAX,
        /**
         * XOR
         */
        XOR,
    }


    /**
     * Creates a prefix agg aggregator.
     *
     * @return a prefix agg aggregator.
     */
    public static PrefixAggParty createPrefixAggSender(Rpc senderRpc, Party receiverParty, PrefixAggConfig config) {
        PrefixAggTypes type = config.getPrefixType();
        switch (type) {
            case SUM:
                return PrefixSumFactory.createPrefixSumSender(senderRpc, receiverParty, (PrefixSumConfig) config);
            case MAX:
                return PrefixMaxFactory.createPrefixMaxSender(senderRpc, receiverParty, (PrefixMaxConfig) config);
            case XOR:
                return PrefixXorFactory.createPrefixXorSender(senderRpc, receiverParty, (PrefixXorConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixAggTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a prefix agg aggregator.
     *
     * @return a prefix agg aggregator.
     */
    public static PrefixAggParty createPrefixAggReceiver(Rpc receiverRpc, Party senderParty, PrefixAggConfig config) {
        PrefixAggTypes type = config.getPrefixType();
        switch (type) {
            case SUM:
                return PrefixSumFactory.createPrefixSumReceiver(receiverRpc, senderParty, (PrefixSumConfig) config);
            case MAX:
                return PrefixMaxFactory.createPrefixMaxReceiver(receiverRpc, senderParty, (PrefixMaxConfig) config);
            case XOR:
                return PrefixXorFactory.createPrefixXorReceiver(receiverRpc, senderParty, (PrefixXorConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixSumTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default prefix-agg config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static PrefixAggConfig createDefaultPrefixAggConfig(SecurityModel securityModel, Zl zl, boolean silent, PrefixAggTypes type, boolean plainOutput) {
        switch (type) {
            case MAX:
                return PrefixMaxFactory.createDefaultPrefixMaxConfig(securityModel, zl, silent, plainOutput);
            case SUM:
                return PrefixSumFactory.createDefaultPrefixSumConfig(securityModel, zl, silent, plainOutput);
            case XOR:
                return PrefixXorFactory.createDefaultPrefixXorConfig(securityModel, zl, silent, plainOutput);
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
