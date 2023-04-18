package edu.alibaba.mpc4j.s2pc.opf.sqoprf.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04.Nr04EccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * PSSW09 based LowMc single-query OPRF config.
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class LowMcSqOprfConfig implements SqOprfConfig {

	/**
	 * oprp config
	 */
	private final OprpConfig oprpConfig;

	private LowMcSqOprfConfig(LowMcSqOprfConfig.Builder builder) {
		oprpConfig = builder.oprpConfig;
	}

	public OprpConfig getOprpConfig() {
		return oprpConfig;
	}

	@Override
	public SqOprfFactory.SqOprfType getPtoType() {
		return SqOprfFactory.SqOprfType.LOW_MC;
	}

	@Override
	public void setEnvType(EnvType envType) {
		oprpConfig.setEnvType(envType);
	}

	@Override
	public EnvType getEnvType() {
		return oprpConfig.getEnvType();
	}

	@Override
	public SecurityModel getSecurityModel() {
		SecurityModel securityModel = SecurityModel.SEMI_HONEST;
		if (oprpConfig.getSecurityModel().compareTo(securityModel) < 0) {
			securityModel = oprpConfig.getSecurityModel();
		}
		return securityModel;
	}


	public static class Builder implements org.apache.commons.lang3.builder.Builder<LowMcSqOprfConfig> {

		/**
		 * Oprp Config
		 */
		private OprpConfig oprpConfig;

		public Builder() {
			oprpConfig = OprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
		}

		public LowMcSqOprfConfig.Builder setOprpConfig(OprpConfig oprpConfig) {
			this.oprpConfig = oprpConfig;
			return this;
		}

		@Override
		public LowMcSqOprfConfig build() {
			return new LowMcSqOprfConfig(this);
		}
	}
}
