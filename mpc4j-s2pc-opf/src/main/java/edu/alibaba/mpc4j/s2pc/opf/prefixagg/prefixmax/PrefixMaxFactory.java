package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.amos22.Amos22PrefixMaxConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.amos22.Amos22PrefixMaxReceiver;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.amos22.Amos22PrefixMaxSender;
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
        AMOS22,
    }

    /**
     * Creates a prefix max aggregator.
     *
     * @return a prefix max aggregator.
     */
    public static AbstractPrefixMaxAggregator createPrefixMaxSender(Rpc senderRpc, Party receiverParty, PrefixMaxConfig config) {
        PrefixMaxTypes type = config.getPtoType();
        switch (type) {
            case Xxx23:
                return new Xxx23PrefixMaxSender(senderRpc, receiverParty, (Xxx23PrefixMaxConfig) config);
            case AMOS22:
                return new Amos22PrefixMaxSender(senderRpc, receiverParty, (Amos22PrefixMaxConfig) config);
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
        switch (type) {
            case Xxx23:
                return new Xxx23PrefixMaxReceiver(receiverRpc, senderParty, (Xxx23PrefixMaxConfig) config);
            case AMOS22:
                return new Amos22PrefixMaxReceiver(receiverRpc, senderParty, (Amos22PrefixMaxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixMaxTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default prefix-max config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static PrefixMaxConfig createDefaultPrefixMaxConfig(SecurityModel securityModel, Zl zl, boolean silent,boolean plainOutput) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
//                return new Amos22PrefixMaxConfig.Builder(zl, silent).setPlainOutput(plainOutput).build();
                return new Xxx23PrefixMaxConfig.Builder(zl, silent).setPlainOutput(plainOutput).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
