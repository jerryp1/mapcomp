package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;

/**
 * AAAG22 keyword PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
public class Aaag22KwPirConfig implements KwPirConfig {
    /**
     * ecc point compress encode
     */
    private final boolean compressEncode;

    public Aaag22KwPirConfig(Builder builder) {
        compressEncode = builder.compressEncode;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public void setEnvType(EnvType envType) {
        if (envType.equals(EnvType.STANDARD_JDK) || envType.equals(EnvType.INLAND_JDK)) {
            throw new IllegalArgumentException("Protocol using " + CommonConstants.MPC4J_NATIVE_FHE_NAME
                + " must not be " + EnvType.STANDARD_JDK.name() + " or " + EnvType.INLAND_JDK.name()
                + ": " + envType.name());
        }
    }

    @Override
    public EnvType getEnvType() {
        return EnvType.STANDARD;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    @Override
    public KwPirFactory.KwPirType getProType() {
        return KwPirFactory.KwPirType.CMG21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aaag22KwPirConfig> {
        /**
         * ecc point compress encode
         */
        private boolean compressEncode;

        public Builder() {
            compressEncode = true;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Aaag22KwPirConfig build() {
            return new Aaag22KwPirConfig(this);
        }
    }
}
