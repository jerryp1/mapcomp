package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;

/**
 * RRK+20 Zl Max Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlMaxConfig implements ZlMaxConfig {
    /**
     * Zl greater config.
     */
    private final ZlGreaterConfig zlGreaterConfig;

    private Rrk20ZlMaxConfig(Builder builder) {
        zlGreaterConfig = builder.zlGreaterConfig;
    }

    public ZlGreaterConfig getZlGreaterConfig() {
        return zlGreaterConfig;
    }

    @Override
    public ZlMaxFactory.ZlMaxType getPtoType() {
        return ZlMaxFactory.ZlMaxType.RRK20;
    }

    @Override
    public Zl getZl() {
        return zlGreaterConfig.getZl();
    }

    @Override
    public void setEnvType(EnvType envType) {
        zlGreaterConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return zlGreaterConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (zlGreaterConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = zlGreaterConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlMaxConfig> {
        /**
         * Zl greater config.
         */
        private final ZlGreaterConfig zlGreaterConfig;

        public Builder(Zl zl) {
            zlGreaterConfig = ZlGreaterFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true, zl);
        }

        @Override
        public Rrk20ZlMaxConfig build() {
            return new Rrk20ZlMaxConfig(this);
        }
    }
}
