package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.Kkrt16OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfConfig implements SqOprfConfig {

	/**
	 * 是否使用压缩椭圆曲线编码
	 */
	private final boolean compressEncode;
	/**
	 * 环境类型
	 */
	private EnvType envType;
	/**
	 * 核COT协议配置项
	 */
	private final CoreCotConfig coreCotConfig;


	private Nr04EccSqOprfConfig(Nr04EccSqOprfConfig.Builder builder) {
		compressEncode = builder.compressEncode;
		envType = EnvType.STANDARD;
		coreCotConfig = builder.coreCotConfig;
	}

	public CoreCotConfig getCoreCotConfig() {
		return coreCotConfig;
	}

	@Override
	public SqOprfFactory.SqOprfType getPtoType() {
		return SqOprfFactory.SqOprfType.NR04_ECC;
	}

	@Override
	public void setEnvType(EnvType envType) {
		this.envType = envType;
	}

	@Override
	public EnvType getEnvType() {
		return envType;
	}

	@Override
	public SecurityModel getSecurityModel() {
		SecurityModel securityModel = SecurityModel.SEMI_HONEST;
		if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
			securityModel = coreCotConfig.getSecurityModel();
		}
		return securityModel;
	}

	public boolean getCompressEncode() {
		return compressEncode;
	}

	public static class Builder implements org.apache.commons.lang3.builder.Builder<Nr04EccSqOprfConfig> {
		/**
		 * 是否使用压缩椭圆曲线编码
		 */
		private boolean compressEncode;

		/**
		 * 核COT协议配置项
		 */
		private CoreCotConfig coreCotConfig;

		public Builder() {
			compressEncode = true;
			coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
		}

		public Nr04EccSqOprfConfig.Builder setCompressEncode(boolean compressEncode) {
			this.compressEncode = compressEncode;
			return this;
		}

		public Nr04EccSqOprfConfig.Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
			this.coreCotConfig = coreCotConfig;
			return this;
		}

		@Override
		public Nr04EccSqOprfConfig build() {
			return new Nr04EccSqOprfConfig(this);
		}
	}
}
