package edu.alibaba.mpc4j.s2pc.opf.prefixagg;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.PrefixMaxConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.PrefixMaxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.PrefixSumConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.PrefixSumFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.PrefixSumFactory.PrefixSumTypes;

/**
 * @author Li Peng
 * @date 2023/11/6
 */
public class PrefixAggFactory {

    /**
     * prefix agg type.
     */
    public enum PrefixAggTypes {
        SUM,
        MAX,
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
                return PrefixSumFactory.createPrefixSumReceiver(receiverRpc, senderParty, (PrefixSumConfig) config);
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
    public static PrefixAggConfig createDefaultPrefixAggConfig(SecurityModel securityModel, Zl zl, boolean silent, PrefixAggTypes type) {
        switch (type) {
            case MAX:
                return PrefixMaxFactory.createDefaultPrefixMaxConfig(securityModel, zl, silent);
            case SUM:
                return PrefixSumFactory.createDefaultPrefixSumConfig(securityModel, zl, silent);
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
