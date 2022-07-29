package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotFactory;

/**
 * 缓存COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CacheCotConfig implements CotConfig {
    /**
     * NCCOT协议配置项
     */
    private final NcCotConfig nccotConfig;
    /**
     * PCOT协议配置项
     */
    private final PcotConfig pcotConfig;

    private CacheCotConfig(Builder builder) {
        // 两个协议的环境类型必须相同
        assert builder.nccotConfig.getEnvType().equals(builder.pcotConfig.getEnvType());
        nccotConfig = builder.nccotConfig;
        pcotConfig = builder.pcotConfig;
    }

    public NcCotConfig getNccotConfig() {
        return nccotConfig;
    }

    public PcotConfig getPcotConfig() {
        return pcotConfig;
    }

    @Override
    public CotFactory.CotType getPtoType() {
        return CotFactory.CotType.CACHE;
    }

    @Override
    public int maxBaseNum() {
        return nccotConfig.maxAllowNum();
    }

    @Override
    public EnvType getEnvType() {
        return nccotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (nccotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = nccotConfig.getSecurityModel();
        }
        if (pcotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = pcotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CacheCotConfig> {
        /**
         * NCCOT协议配置项
         */
        private NcCotConfig nccotConfig;
        /**
         * POT协议配置项
         */
        private PcotConfig pcotConfig;

        public Builder(SecurityModel securityModel) {
            nccotConfig = NcCotFactory.createDefaultConfig(securityModel);
            pcotConfig = PcotFactory.createDefaultConfig(securityModel);
        }

        public Builder setNcCotConfig(NcCotConfig ncCotConfig) {
            this.nccotConfig = ncCotConfig;
            return this;
        }

        public Builder setPcotConfig(PcotConfig pcotConfig) {
            this.pcotConfig = pcotConfig;
            return this;
        }

        @Override
        public CacheCotConfig build() {
            return new CacheCotConfig(this);
        }
    }
}
