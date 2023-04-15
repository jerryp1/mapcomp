package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 *
 * This protocol is implicitly introduced in the following paper:
 * <p>
 *  Moni Naor and Omer Reingold. Number-theoretic constructions of e cient pseudo-random functions. J.
 *  ACM, 51(2):231{262, 2004.
 * </p>
 * The implementation here refers to Figure 3 in the following paper:
 * <p>
 * Á Kiss, Liu J , Schneider T , et al. Private Set Intersection for Unequal Set Sizes with
 * Mobile Applications[J]. Proceedings on Privacy Enhancing Technologies, 2017, 2017(4).
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfPtoDesc implements PtoDesc{

    /**
     * 协议ID random get a long value as PTO_ID
     */
    private static final int PTO_ID = Math.abs((int)4711347230656983538L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "NR04_ECC_SQ_OPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * Sender sends g^{r^{inv}}
         */
        SENDER_SEND_GR_INV,
        /**
         * Receiver sends y[i] \oplus r_i, y[i] is the i-th bit of input, r_i is the i-th Random COT Choice bit
         */
        RECEIVER_SEND_BINARY,
        /**
         * Sender sends masked R0 and R1
          */
        SENDER_SEND_MESSAGE,
    }

    /**
     * 单例模式
     */
    private static final Nr04EccSqOprfPtoDesc INSTANCE = new Nr04EccSqOprfPtoDesc();

    /**
     * 私有构造函数
     */
    private Nr04EccSqOprfPtoDesc() {
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
