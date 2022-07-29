package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.mspcot.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.mspcot.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotFactory;

/**
 * YWL20-NC-COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/27
 */
public class Ywl20NcCotConfig implements NcCotConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;
    /**
     * MSP-COT协议配置项
     */
    private final MspCotConfig mspCotConfig;

    private Ywl20NcCotConfig(Builder builder) {
        // 两个协议的环境配型必须相同
        assert builder.rcotConfig.getEnvType().equals(builder.mspcotConfig.getEnvType());
        rcotConfig = builder.rcotConfig;
        mspCotConfig = builder.mspcotConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    public MspCotConfig getMspCotConfig() {
        return mspCotConfig;
    }

    @Override
    public NcCotFactory.NcCotType getPtoType() {
        return NcCotFactory.NcCotType.YWL20;
    }

    @Override
    public int maxAllowNum() {
        return 1 << Ywl20NcCotPtoDesc.MAX_LOG_N;
    }

    @Override
    public EnvType getEnvType() {
        return mspCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (rcotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = rcotConfig.getSecurityModel();
        }
        if (mspCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = mspCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20NcCotConfig> {
        /**
         * RCOT协议配置项
         */
        private RcotConfig rcotConfig;
        /**
         * MSPCOT协议配置项
         */
        private MspCotConfig mspcotConfig;

        public Builder(SecurityModel securityModel) {
            rcotConfig = RcotFactory.createDefaultConfig(securityModel);
            mspcotConfig = MspCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setRcotConfig(RcotConfig rcotConfig) {
            this.rcotConfig = rcotConfig;
            return this;
        }

        public Builder setMspcotConfig(MspCotConfig mspcotConfig) {
            this.mspcotConfig = mspcotConfig;
            return this;
        }

        @Override
        public Ywl20NcCotConfig build() {
            return new Ywl20NcCotConfig(this);
        }
    }
}
