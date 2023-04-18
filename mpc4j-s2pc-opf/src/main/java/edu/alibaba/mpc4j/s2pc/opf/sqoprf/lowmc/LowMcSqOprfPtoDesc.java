package edu.alibaba.mpc4j.s2pc.opf.sqoprf.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LowMc single-query OPRF protocol description. This protocol is implicitly introduced in the following paper:
 * <p>
 * Benny Pinkas, Thomas Schneider, Nigel P. Smart, and Stephen C. Williams. Secure two-party compu-
 * tation is practical. In Advances in Cryptology { ASIACRYPT'09, volume 5912 of LNCS, pages 250{267.
 * Springer, 2009.
 * <p/>
 * Here, we use LowMc instead of AES with the goal of reducing the number of AND gates.
 * More specific, the implementation here refers to Figure 4 in the following paper:
 * <p>
 * Á Kiss, Liu J , Schneider T , et al. Private Set Intersection for Unequal Set Sizes with
 * Mobile Applications[J]. Proceedings on Privacy Enhancing Technologies, 2017, 2017(4).
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class LowMcSqOprfPtoDesc implements PtoDesc {


	/**
	 * protocol ID
	 */
	private static final int PTO_ID = Math.abs((int) 8968002409527651469L);
	/**
	 * protocol name
	 */
	private static final String PTO_NAME = "LOW_MC_SQ_OPRF";

	/**
	 * protocol step
	 */
	enum PtoStep {
		/**
		 * sender sends shares of oprp result
		 */
		SENDER_SEND_SHARES,
	}

	/**
	 * singleton mode
	 */
	private static final LowMcSqOprfPtoDesc INSTANCE = new LowMcSqOprfPtoDesc();

	/**
	 * private constructor
	 */
	private LowMcSqOprfPtoDesc() {
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
