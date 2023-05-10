package edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.SquareZlConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.SquareZlFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgFactory;

/**
 * Bea91 Zl protocol config.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class Bea91SquareZlConfig implements SquareZlConfig {
    /**
     * Zl triple generation config
     */
    private final ZlMtgConfig zlMtgConfig;

    private Bea91SquareZlConfig(Builder builder) {
        zlMtgConfig = builder.zlMtgConfig;
    }

    public ZlMtgConfig getZlMtgConfig() {
        return zlMtgConfig;
    }

    @Override
    public SquareZlFactory.SquareZlType getPtoType() {
        return SquareZlFactory.SquareZlType.BEA91;
    }

    @Override
    public Zl getZl() {
        return zlMtgConfig.getZl();
    }

    @Override
    public void setEnvType(EnvType envType) {
        zlMtgConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return zlMtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (zlMtgConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = zlMtgConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91SquareZlConfig> {
        /**
         * Boolean triple generation config
         */
        private ZlMtgConfig zlMtgConfig;

        public Builder(Zl zl) {
            zlMtgConfig = ZlMtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
        }

        public Builder setZlMtgConfig(ZlMtgConfig zlMtgConfig) {
            this.zlMtgConfig = zlMtgConfig;
            return this;
        }

        @Override
        public Bea91SquareZlConfig build() {
            return new Bea91SquareZlConfig(this);
        }
    }
}
