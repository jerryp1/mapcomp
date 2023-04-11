package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.direct.DirectNcLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.direct.DirectNcLnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.direct.DirectNcLnotSender;

/**
 * no-choice 1-out-of-n (with n = 2^l) factory.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class NcLnotFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private NcLnotFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum NcLnotType {
        /**
         * direct 1-out-of-2^l COT
         */
        DIRECT,
        /**
         * silent no-choice COT
         */
        SILENT,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static NcLnotSender createSender(Rpc senderRpc, Party receiverParty, NcLnotConfig config) {
        NcLnotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectNcLnotSender(senderRpc, receiverParty, (DirectNcLnotConfig) config);
            case SILENT:
            default:
                throw new IllegalArgumentException("Invalid " + NcLnotType.class.getSimpleName() + ": " + type.name());
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
    public static NcLnotReceiver createReceiver(Rpc receiverRpc, Party senderParty, NcLnotConfig config) {
        NcLnotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectNcLnotReceiver(receiverRpc, senderParty, (DirectNcLnotConfig) config);
            case SILENT:
            default:
                throw new IllegalArgumentException("Invalid " + NcLnotType.class.getSimpleName() + ": " + type.name());
        }
    }
}
