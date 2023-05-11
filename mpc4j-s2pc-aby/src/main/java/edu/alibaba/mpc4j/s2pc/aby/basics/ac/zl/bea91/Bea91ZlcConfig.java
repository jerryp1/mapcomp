package edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgFactory;

/**
 * Bea91 Zl circuit config.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class Bea91ZlcConfig implements ZlcConfig {
    /**
     * multiplication triple generation config
     */
    private final ZlMtgConfig mtgConfig;

    private Bea91ZlcConfig(Builder builder) {
        mtgConfig = builder.mtgConfig;
    }

    public ZlMtgConfig getMtgConfig() {
        return mtgConfig;
    }

    @Override
    public ZlcFactory.ZlType getPtoType() {
        return ZlcFactory.ZlType.BEA91;
    }

    @Override
    public Zl getZl() {
        return mtgConfig.getZl();
    }

    @Override
    public void setEnvType(EnvType envType) {
        mtgConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return mtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (mtgConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = mtgConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91ZlcConfig> {
        /**
         * multiplication triple generation config
         */
        private ZlMtgConfig mtgConfig;

        public Builder(Zl zl) {
            mtgConfig = ZlMtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
        }

        public Builder setMtgConfig(ZlMtgConfig mtgConfig) {
            this.mtgConfig = mtgConfig;
            return this;
        }

        @Override
        public Bea91ZlcConfig build() {
            return new Bea91ZlcConfig(this);
        }
    }
}
