package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PSTY19 circuit PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. Efficient circuit-based PSI with linear
 * communication. EUROCRYPT 2019, Part III, pp. 122-153. Springer International Publishing, 2019.
 * </p>
 * The implementation has linear communication with stash-less cuckoo hashing.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
class Psty19UnbalancedCpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6547825468874236541L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "PRTY19-UCPSI";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends cuckoo hash keys
         */
        SENDER_SEND_CUCKOO_HASH_KEYS,
    }

    /**
     * the singleton mode
     */
    private static final Psty19UnbalancedCpsiPtoDesc INSTANCE = new Psty19UnbalancedCpsiPtoDesc();

    /**
     * private constructor.
     */
    private Psty19UnbalancedCpsiPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
