package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * DSZ15 OT-based Zl core multiplication triple generation protocol configuration.
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/2/20
 */
public class Dsz15OtZlCoreMtgConfig implements ZlCoreMtgConfig {
    /**
     * the l bit length
     */
    private final int l;
    /**
     * the COT configuration
     */
    private final CotConfig cotConfig;

    private Dsz15OtZlCoreMtgConfig(Builder builder) {
        l = builder.l;
        cotConfig = builder.cotConfig;
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return ZlCoreMtgFactory.ZlCoreMtgType.DSZ15_OT;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int maxAllowNum() {
        return (int) Math.floor((double) cotConfig.maxBaseNum() / l);
    }

    @Override
    public void setEnvType(EnvType envType) {
        cotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return cotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (cotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = cotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dsz15OtZlCoreMtgConfig> {
        /**
         * the l bit length
         */
        private final int l;
        /**
         * the COT configuration
         */
        private CotConfig cotConfig;

        public Builder(int l) {
            super();
            assert l > 0 : "l must be greater than 0: " + l;
            this.l = l;
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Dsz15OtZlCoreMtgConfig build() {
            return new Dsz15OtZlCoreMtgConfig(this);
        }
    }
}
