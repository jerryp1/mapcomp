package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * batched OPPRF-based payload-circuit PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. Efficient circuit-based PSI with linear
 * communication. EUROCRYPT 2019, Part III, pp. 122-153. Springer International Publishing, 2019.
 * </p>
 * <p>
 * Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: fast OPRF and circuit-PSI from vector-OLE. EUROCRYPT 2021,
 * pp. 901-930. Cham: Springer International Publishing, 2021.
 * </p>
 * The implementation has linear communication with stash-less cuckoo hashing.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class BopprfPlpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6251801160606438604L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "BOPPRF-PLPSI";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the client sends cuckoo hash keys
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
    }

    /**
     * the singleton mode
     */
    private static final BopprfPlpsiPtoDesc INSTANCE = new BopprfPlpsiPtoDesc();

    /**
     * private constructor.
     */
    private BopprfPlpsiPtoDesc() {
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
