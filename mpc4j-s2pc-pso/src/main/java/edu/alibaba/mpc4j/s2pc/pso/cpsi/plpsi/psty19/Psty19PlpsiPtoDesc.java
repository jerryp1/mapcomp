package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PSTY19 payload-circuit PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. Efficient circuit-based PSI with linear
 * communication. EUROCRYPT 2019, Part III, pp. 122-153. Springer International Publishing, 2019.
 * </p>
 * The implementation has linear communication with stash-less cuckoo hashing.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class Psty19PlpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -8623086639398237627L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "PSTY19-CCPSI";
    /**
     * the singleton mode
     */
    private static final Psty19PlpsiPtoDesc INSTANCE = new Psty19PlpsiPtoDesc();

    /**
     * private constructor.
     */
    private Psty19PlpsiPtoDesc() {
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
