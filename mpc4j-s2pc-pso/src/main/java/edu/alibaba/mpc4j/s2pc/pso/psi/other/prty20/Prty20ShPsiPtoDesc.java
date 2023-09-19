package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;

/**
 * PRTY20 semi-honest PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
class Prty20ShPsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8534409963311736779L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PRTY20_SEMI_HONEST_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * client sends PaXoS key
         */
        CLIENT_SEND_PAXOS_KEY,
        /**
         * server sends PRF filter
         */
        SERVER_SEND_PRF_FILTER,
    }
    /**
     * singleton mode
     */
    private static final Prty20ShPsiPtoDesc INSTANCE = new Prty20ShPsiPtoDesc();

    /**
     * private constructor.
     */
    private Prty20ShPsiPtoDesc() {
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

    /**
     * Gets LCOT input bit length, which depends on the server element size (l_1) and the client element size (l_2).
     * The paper states that:
     * <p>
     * To instantiate our protocol for semi-honest security, it is enough to set l_1 = l_2 = σ + 2log_2(n), the minimum
     * possible value for security.
     * </p>
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return LCOT input bit length.
     */
    static int getLcotInputBitLength(int serverElementSize, int clientElementSize) {
        MathPreconditions.checkPositiveInRangeClosed("server_element_size", serverElementSize, 1 << 24);
        MathPreconditions.checkPositiveInRangeClosed("client_element_size", clientElementSize, 1 << 24);
        int n = Math.max(serverElementSize, clientElementSize);
        if (n > (1 << 20)) {
            // n = 2^24, dataword = 88, codeword = 506, the corresponding linear coder is dataword = 90, codewrod = 495
            return LinearCoderFactory.getInstance(88).getDatawordBitLength();
        } else if (n > (1 << 16)) {
            // n = 2^20, dataword = 80, codeword = 495, the corresponding linear coder is dataword = 84, codeword = 495
            return LinearCoderFactory.getInstance(80).getDatawordBitLength();
        } else if (n > (1 << 12)) {
            // n = 2^16, dataword = 72, codeword = 473, the corresponding linear coder is dataword = 72, codeword = 462
            return LinearCoderFactory.getInstance(72).getDatawordBitLength();
        } else {
            // n = 2^12, dataword = 64, codeword = 448, the corresponding linear coder is dataword = 65, codeword = 448
            return LinearCoderFactory.getInstance(64).getDatawordBitLength();
        }
    }
}
