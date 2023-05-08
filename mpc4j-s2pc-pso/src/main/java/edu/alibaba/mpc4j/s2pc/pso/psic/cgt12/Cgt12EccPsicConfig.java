package edu.alibaba.mpc4j.s2pc.pso.psic.cgt12;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.psic.PsicConfig;
import edu.alibaba.mpc4j.s2pc.pso.psic.PsicFactory;

/**
 * CGT12 based on ECC PSCI config.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Cgt12EccPsicConfig implements PsicConfig {
	/**
	 * 环境类型
	 */
	private EnvType envType;

	/**
	 * compress encode
	 */
	private final boolean compressEncode;

	private Cgt12EccPsicConfig(Cgt12EccPsicConfig.Builder builder) {
		this.compressEncode = builder.compressEncode;
		envType = EnvType.STANDARD;
	}

	@Override
	public PsicFactory.PsicType getPtoType() {
		return PsicFactory.PsicType.CGT12_ECC;
	}

	@Override
	public void setEnvType(EnvType envType) {
		this.envType = envType;
	}

	public boolean getCompressEncode() {
		return compressEncode;
	}

	@Override
	public EnvType getEnvType() {
		return envType;
	}

	@Override
	public SecurityModel getSecurityModel() {
		return SecurityModel.SEMI_HONEST;
	}

	public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgt12EccPsicConfig> {

		/**
		 * 是否压缩编码
		 */
		private boolean compressEncode;

		public Builder() {
			compressEncode = true;
		}

		public Cgt12EccPsicConfig.Builder setCompressEncode(boolean compressEncode) {
			this.compressEncode = compressEncode;
			return this;
		}

		@Override
		public Cgt12EccPsicConfig build() {
			return new Cgt12EccPsicConfig(this);
		}
	}
}

