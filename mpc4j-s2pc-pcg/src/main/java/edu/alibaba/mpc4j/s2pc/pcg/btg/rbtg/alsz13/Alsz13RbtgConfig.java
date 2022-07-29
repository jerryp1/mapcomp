package edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.alsz13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotFactory;

/**
 * ALSZ13-RBTG协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/6
 */
public class Alsz13RbtgConfig implements RbtgConfig {
    /**
     * NC-COT协议配置项
     */
    private final NcCotConfig ncCotConfig;

    private Alsz13RbtgConfig(Builder builder) {
        ncCotConfig = builder.ncCotCOnfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    @Override
    public RbtgFactory.RbtgType getPtoType() {
        return RbtgFactory.RbtgType.ALSZ13;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alsz13RbtgConfig> {
        /**
         * NC-COT协议配置项
         */
        private NcCotConfig ncCotCOnfig;

        public Builder() {
            ncCotCOnfig = NcCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setNcCotCOnfig(NcCotConfig ncCotCOnfig) {
            this.ncCotCOnfig = ncCotCOnfig;
            return this;
        }

        @Override
        public Alsz13RbtgConfig build() {
            return new Alsz13RbtgConfig(this);
        }
    }
}
