package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.nco;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotFactory;

/**
 * NCO-COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/04
 */
public class NcoCotConfig implements CotConfig {
    /**
     * NCCOT协议配置项
     */
    private final NcCotConfig ncCotConfig;
    /**
     * PCOT协议配置项
     */
    private final PcotConfig pcotConfig;

    private NcoCotConfig(Builder builder) {
        // 两个协议的环境类型必须相同
        assert builder.sccotConfig.getEnvType().equals(builder.pcotConfig.getEnvType());
        ncCotConfig = builder.sccotConfig;
        pcotConfig = builder.pcotConfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    public PcotConfig getPcotConfig() {
        return pcotConfig;
    }

    @Override
    public CotFactory.CotType getPtoType() {
        return CotFactory.CotType.NO_CHOICE_ONCE;
    }

    @Override
    public int maxBaseNum() {
        return ncCotConfig.maxAllowNum();
    }

    @Override
    public EnvType getEnvType() {
        return ncCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (ncCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = ncCotConfig.getSecurityModel();
        }
        if (pcotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = pcotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NcoCotConfig> {
        /**
         * NCCOT协议配置项
         */
        private NcCotConfig sccotConfig;
        /**
         * POT协议配置项
         */
        private PcotConfig pcotConfig;

        public Builder(SecurityModel securityModel) {
            sccotConfig = NcCotFactory.createDefaultConfig(securityModel);
            pcotConfig = PcotFactory.createDefaultConfig(securityModel);
        }

        public Builder setNcCotConfig(NcCotConfig ncCotConfig) {
            this.sccotConfig = ncCotConfig;
            return this;
        }

        public Builder setPcotConfig(PcotConfig pcotConfig) {
            this.pcotConfig = pcotConfig;
            return this;
        }

        @Override
        public NcoCotConfig build() {
            return new NcoCotConfig(this);
        }
    }
}
