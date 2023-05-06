package edu.alibaba.mpc4j.s2pc.pso.psic.cgt12;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGT12 based on ECC PSI Cardinality protocol. This protocol is implicitly introduced in the following paper:
 * <p>
 * Emiliano De Cristofaro, Paolo Gasti, and Gene Tsudik. Fast and private computation of cardinality
 * of set intersection and union. In Cryptology and Network Security, 11th International Conference,
 * CANS 2012, volume 7712, pages 218–231. Springer, 2012.
 * </p>
 * In Cryptographic Details: Private Preference Matching paragraph.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Cgt12EccPsicPtoDesc implements PtoDesc {
	/**
	 * ptotocol id
	 */
	private static final int PTO_ID = Math.abs((int) 7883654564002247205L);
	/**
	 * 协议名称
	 */
	private static final String PTO_NAME = "CGT12_ECC_PSIC";

	/**
	 * 协议步骤
	 */
	enum PtoStep {
		/**
		 * 客户端发送H(Y)^β
		 */
		CLIENT_SEND_HY_BETA,
		/**
		 * 服务端发送 Peqt-Hash(H(X)^α)
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
	private static final Cgt12EccPsicPtoDesc INSTANCE = new Cgt12EccPsicPtoDesc();

	/**
	 * 私有构造函数
	 */
	private Cgt12EccPsicPtoDesc() {
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

