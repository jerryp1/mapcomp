package edu.alibaba.mpc4j.s2pc.pso.psic.cpsic;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingConfig;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psic.PsicConfig;
import edu.alibaba.mpc4j.s2pc.pso.psic.PsicFactory;

/**
 * Circuit-PSI Cardinality Config
 *
 * @author Qixian Zhou
 * @date 2023/4/24
 */
public class CpsicConfig implements PsicConfig {

	/**
	 * Client payload Circuit PSI
	 */
	private final CcpsiConfig ccpsiConfig;

	/**
	 * hamming config
	 */
	private final HammingConfig hammingConfig;


	private CpsicConfig(Builder builder) {

		this.ccpsiConfig = builder.ccpsiConfig;
		this.hammingConfig = builder.hammingConfig;
	}


	public CcpsiConfig getCcpsiConfig() {
		return ccpsiConfig;
	}

	public HammingConfig getHammingConfig() {
		return hammingConfig;
	}

	@Override
	public PsicFactory.PsicType getPtoType() {
		return PsicFactory.PsicType.CIRCUIT_PSIC;
	}


	@Override
	public void setEnvType(EnvType envType) {
		ccpsiConfig.setEnvType(envType);
		hammingConfig.setEnvType(envType);
	}

	@Override
	public EnvType getEnvType() {
		assert ccpsiConfig.getEnvType() == hammingConfig.getEnvType() : "two sub config envType must be equal";
		return ccpsiConfig.getEnvType();
	}

	@Override
	public SecurityModel getSecurityModel() {
		return SecurityModel.SEMI_HONEST;
	}

	public static class Builder implements org.apache.commons.lang3.builder.Builder<CpsicConfig> {

		/**
		 * Client payload Circuit PSI
		 */
		private CcpsiConfig ccpsiConfig;

		/**
		 * hamming config
		 */
		private HammingConfig hammingConfig;

		public Builder(boolean silent) {
			ccpsiConfig = CcpsiFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
			hammingConfig = HammingFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
		}

		public Builder setCcpsiConfig(CcpsiConfig ccpsiConfig) {
			this.ccpsiConfig = ccpsiConfig;
			return this;
		}

		public Builder setHammingConfig(HammingConfig hammingConfig) {
			this.hammingConfig = hammingConfig;
			return this;
		}

		@Override
		public CpsicConfig build() {
			return new CpsicConfig(this);
		}
	}
}
