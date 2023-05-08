package edu.alibaba.mpc4j.s2pc.pso.psic.cpsic;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.s2pc.pso.psic.cgt12.Cgt12EccPsicPtoDesc;

/**
 * Circuit-PSI Cardinality PtoDesc.
 * Any Circuit-PSI + Secure HammingWeight Protocol = Circuit-PSI Cardinality Pto
 *
 * @author Qixian Zhou
 * @date 2023/4/24
 */
public class CpsicPtoDesc implements PtoDesc {
	/**
	 * ptotocol id
	 */
	private static final int PTO_ID = Math.abs((int) 6638744674193559661L);
	/**
	 * 协议名称
	 */
	private static final String PTO_NAME = "CIRCUIT_PSIC";

	/**
	 * 协议步骤
	 */
	enum PtoStep {

	}

	/**
	 * 单例模式
	 */
	private static final CpsicPtoDesc INSTANCE = new CpsicPtoDesc();

	/**
	 * 私有构造函数
	 */
	private CpsicPtoDesc() {
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
