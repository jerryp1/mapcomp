package edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;

/**
 * PSI-PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzl24BatchIndexPirConfig implements BatchIndexPirConfig {
    /**
     * UPSI config
     */
    private final UpsiConfig upsiConfig;

    public Lpzl24BatchIndexPirConfig(Builder builder) {
        upsiConfig = builder.upsiConfig;
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

    public UpsiConfig getUpsiConfig() {
        return upsiConfig;
    }

    @Override
    public BatchIndexPirFactory.BatchIndexPirType getPtoType() {
        return BatchIndexPirFactory.BatchIndexPirType.PSI_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lpzl24BatchIndexPirConfig> {
        /**
         * UPSI config
         */
        private UpsiConfig upsiConfig;

        public Builder() {
            upsiConfig = new Cmg21UpsiConfig.Builder().build();
        }

        public Builder setUpsiConfig(UpsiConfig upsiConfig) {
            this.upsiConfig = upsiConfig;
            return this;
        }

        @Override
        public Lpzl24BatchIndexPirConfig build() {
            return new Lpzl24BatchIndexPirConfig(this);
        }
    }
}
