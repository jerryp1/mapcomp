package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * Batch single-point GF2K-VOLE factory.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public class Gf2kBspVoleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kBspVoleFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Gf2kBspVoleType {
        /**
         * WYKW21 (semi-honest)
         */
        WYKW21_SEMI_HONEST,
        /**
         * WYKW21 (malicious)
         */
        WYKW21_MALICIOUS,
    }

    /**
     * Gets the pre-computed num.
     *
     * @param config config.
     * @param batch  batch num.
     * @param num    num.
     * @return pre-computed num.
     */
    public static int getPrecomputeNum(Gf2kBspVoleConfig config, int batch, int num) {
        assert num > 0 && batch > 0;
        Gf2kBspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return batch;
            case WYKW21_MALICIOUS:
                return batch + 1;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVoleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static Gf2kBspVoleSender createSender(Rpc senderRpc, Party receiverParty, Gf2kBspVoleConfig config) {
        Gf2kBspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
            case WYKW21_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVoleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static Gf2kBspVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kBspVoleConfig config) {
        Gf2kBspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
            case WYKW21_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVoleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static Gf2kBspVoleConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
