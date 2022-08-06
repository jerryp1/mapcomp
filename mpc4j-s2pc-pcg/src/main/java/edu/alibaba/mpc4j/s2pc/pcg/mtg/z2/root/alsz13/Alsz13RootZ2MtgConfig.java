package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.alsz13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.RootZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.RootZ2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotFactory;

/**
 * ALSZ13根布尔三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/6
 */
public class Alsz13RootZ2MtgConfig implements RootZ2MtgConfig {
    /**
     * NC-COT协议配置项
     */
    private final NcCotConfig ncCotConfig;

    private Alsz13RootZ2MtgConfig(Builder builder) {
        ncCotConfig = builder.ncCotConfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    @Override
    public RootZ2MtgFactory.RbtgType getPtoType() {
        return RootZ2MtgFactory.RbtgType.ALSZ13;
    }

    @Override
    public int maxAllowNum() {
        return ncCotConfig.maxAllowNum();
    }

    @Override
    public EnvType getEnvType() {
        return ncCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (ncCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = ncCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alsz13RootZ2MtgConfig> {
        /**
         * NC-COT协议配置项
         */
        private NcCotConfig ncCotConfig;

        public Builder() {
            ncCotConfig = NcCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setNcCotConfig(NcCotConfig ncCotConfig) {
            this.ncCotConfig = ncCotConfig;
            return this;
        }

        @Override
        public Alsz13RootZ2MtgConfig build() {
            return new Alsz13RootZ2MtgConfig(this);
        }
    }
}
