package edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain.fa;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain.PlainCompareConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain.PlainCompareFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * 基于全加器的明文比较协议配置项。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/11
 */
public class FullAdderPlainCompareConfig implements PlainCompareConfig {
    /**
     * COT协议配置项
     */
    private final CotConfig cotConfig;

    private FullAdderPlainCompareConfig(Builder builder) {
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public PlainCompareFactory.PlainCompareType getPtoType() {
        return PlainCompareFactory.PlainCompareType.FULL_ADDER;
    }

    @Override
    public int maxAllowBitNum() {
        return cotConfig.maxBaseNum();
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<FullAdderPlainCompareConfig> {
        /**
         * COT协议配置项
         */
        private CotConfig cotConfig;

        public Builder() {
            cotConfig = CotFactory.createCacheConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public FullAdderPlainCompareConfig build() {
            return new FullAdderPlainCompareConfig(this);
        }
    }
}
