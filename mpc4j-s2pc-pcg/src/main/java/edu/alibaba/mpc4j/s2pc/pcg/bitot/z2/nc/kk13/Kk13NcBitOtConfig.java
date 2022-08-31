package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.kk13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.NcBitOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.NcBitOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotFactory;

/**
 * KK13-BitOt协议配置项。
 *
 * @author Hanwen Feng
 */
public class Kk13NcBitOtConfig implements NcBitOtConfig {
    /**
     * NC-LOT协议配置项
     */
    private final NcLotConfig ncLotConfig;

    /**
     * NC-LOT(2^l选1 OT)的l值
     */
    private final int l;

    private Kk13NcBitOtConfig(Builder builder) {
        this.ncLotConfig = builder.ncLotConfig;
        this.l = builder.l;

    }

    /**
     * 获取NC-LOT协议配置项。
     *
     * @return NC-LOT协议配置项
     */
    public NcLotConfig getNcLotConfig() {
        return ncLotConfig;
    }

    /**
     * 获取选择数组的比特长度。
     *
     * @return 选择数组的比特长度。
     */
    public int getL() {
        return l;
    }

    @Override
    public NcBitOtFactory.NcBitOtType getPtoType() {
        return NcBitOtFactory.NcBitOtType.KK13;
    }

    @Override
    public int maxAllowNum() {
        // 原则上没有限制
        return 1 << 24;
    }

    @Override
    public EnvType getEnvType() {
        return ncLotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kk13NcBitOtConfig> {
        /**
         * NC-LOT配置项
         */
        private NcLotConfig ncLotConfig;
        /**
         * NC-LOT协议选择数组比特长度
         */
        private int l;

        public Builder() {
            ncLotConfig = NcLotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            // 经测试，l = 3时效率高，默认为3
            l = 3;
        }

        public Builder setNcLotConfig(NcLotConfig ncLotConfig) {
            this.ncLotConfig = ncLotConfig;
            return this;
        }

        public Builder setL(int l) {
            // 限制l大小
            assert l > 1 && l < 17 : "l must be in the range [2, 16]: " + l;

            this.l = l;
            return this;
        }

        @Override
        public Kk13NcBitOtConfig build() {
            return new Kk13NcBitOtConfig(this);
        }
    }
}
