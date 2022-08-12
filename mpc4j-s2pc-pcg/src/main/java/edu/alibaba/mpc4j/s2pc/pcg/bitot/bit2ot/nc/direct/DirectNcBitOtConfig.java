package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.NcBitOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.NcBitOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;

/**
 * 直接NC-BitOT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/08/11
 */
public class DirectNcBitOtConfig implements NcBitOtConfig {
    /**
     * NC-COT协议配置项
     */
    private final NcCotConfig ncCotConfig;

    private DirectNcBitOtConfig(Builder builder) {
        ncCotConfig = builder.ncCotConfig;
    }

    @Override
    public EnvType getEnvType() {
        return ncCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return ncCotConfig.getSecurityModel();
    }

    @Override
    public NcBitOtFactory.NcBitOtType getPtoType() {
        return NcBitOtFactory.NcBitOtType.DIRECT;
    }

    @Override
    public int maxAllowNum() {
        return ncCotConfig.maxAllowNum();
    }

    /**
     * 获取NC-COT协议配置项。
     *
     * @return NC-COT协议配置项。
     */
    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }
    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectNcBitOtConfig> {
        /**
         * NC-COT配置项
         */
        private NcCotConfig ncCotConfig;

        public Builder(SecurityModel securityModel) {
            ncCotConfig = NcCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setNcCotConfig(NcCotConfig ncCotConfig) {
            this.ncCotConfig = ncCotConfig;
            return this;
        }

        @Override
        public DirectNcBitOtConfig build() {
            return new DirectNcBitOtConfig(this);
        }

    }
}
