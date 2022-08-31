package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotFactory;

/**
 * @author Hanwen Feng
 */
public class DirectNcLotConfig implements NcLotConfig {
    /**
     * NC-COT协议配置项
     */
    private final LhotConfig lhotConfig;

    private DirectNcLotConfig(Builder builder){
        this.lhotConfig = builder.lhotConfig;
    }

    public LhotConfig getLhotConfig() {
        return lhotConfig;
    }

    @Override
    public NcLotFactory.NcLotType getPtoType() {
        return NcLotFactory.NcLotType.Direct;
    }

    @Override
    public int maxAllowNum() {
        return 1 << 24;
    }

    @Override
    public EnvType getEnvType() {
        return lhotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return lhotConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectNcLotConfig> {
        private LhotConfig lhotConfig;

        public Builder(SecurityModel securityModel) {
            this.lhotConfig = LhotFactory.createDefaultConfig(securityModel);
        }

        public Builder setLotConfig(LhotConfig config) {
            this.lhotConfig = config;
            return this;
        }

        @Override
        public DirectNcLotConfig build() {
            return new DirectNcLotConfig(this);
        }
    }
}
