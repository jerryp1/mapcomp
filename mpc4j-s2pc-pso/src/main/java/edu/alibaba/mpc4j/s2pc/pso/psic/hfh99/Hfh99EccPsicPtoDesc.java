package edu.alibaba.mpc4j.s2pc.pso.psic.hfh99;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * HFH99-ECC PSI Cardinality protocol. This protocol is implicitly introduced in the following paper:
 * <p>
 * Huberman B A, Franklin M, Hogg T. Enhancing privacy and trust in electronic communities. FC 1999, Citeseer, pp. 78-86.
 * </p>
 * In Cryptographic Details: Private Preference Matching paragraph.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Hfh99EccPsicPtoDesc implements PtoDesc {
	/**
	 * ptotocol id
	 */
	private static final int PTO_ID = Math.abs((int) 5376138649799085240L);
	/**
	 * protocol name
	 */
	private static final String PTO_NAME = "HFH99_ECC_PSIC";

	/**
	 * protocol step
	 */
	enum PtoStep {
		/**
		 * 客户端发送H(Y)^β
		 */
		CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA,
		/**
		 * 服务端发送H(X)^α
		 */
		SERVER_SEND_RANDOMLY_PERMUTED_HX_ALPHA,
		/**
		 * 服务端发送H(Y)^βα
		 */
		CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA_ALPHA,
	}

	/**
	 * 单例模式
	 */
	private static final Hfh99EccPsicPtoDesc INSTANCE = new Hfh99EccPsicPtoDesc();

	/**
	 * 私有构造函数
	 */
	private Hfh99EccPsicPtoDesc() {
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

