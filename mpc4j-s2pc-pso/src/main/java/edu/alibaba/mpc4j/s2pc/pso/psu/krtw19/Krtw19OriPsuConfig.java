package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

/**
 * KRTW19-ORI-PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public class Krtw19OriPsuConfig implements PsuConfig {
    /**
     * RPMT所用OPRF配置项
     */
    private final OprfConfig rpmtOprfConfig;
    /**
     * PEQT所用OPRF配置项
     */
    private final OprfConfig peqtOprfConfig;
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;
    /**
     * OKVS类型
     */
    private final OkvsType okvsType;
    /**
     * 流水线数量
     */
    private final int pipeSize;

    private Krtw19OriPsuConfig(Builder builder) {
        // 协议的环境类型必须相同
        assert builder.rpmtOprfConfig.getEnvType().equals(builder.peqtOprfConfig.getEnvType());
        assert builder.rpmtOprfConfig.getEnvType().equals(builder.rcotConfig.getEnvType());
        rpmtOprfConfig = builder.rpmtOprfConfig;
        peqtOprfConfig = builder.peqtOprfConfig;
        rcotConfig = builder.rcotConfig;
        okvsType = builder.okvsType;
        pipeSize = builder.pipeSize;
    }

    @Override
    public PsuFactory.PsuType getPtoType() {
        return PsuFactory.PsuType.KRTW19_ORI;
    }

    @Override
    public EnvType getEnvType() {
        return rpmtOprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (rpmtOprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = rpmtOprfConfig.getSecurityModel();
        }
        if (peqtOprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = peqtOprfConfig.getSecurityModel();
        }
        if (rcotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = rcotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public OprfConfig getRpmtOprfConfig() {
        return rpmtOprfConfig;
    }

    public OprfConfig getPeqtOprfConfig() {
        return peqtOprfConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    public OkvsType getOkvsType() {
        return okvsType;
    }

    public int getPipeSize() {
        return pipeSize;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Krtw19OriPsuConfig> {
        /**
         * RPMT所用OPRF配置项
         */
        private OprfConfig rpmtOprfConfig;
        /**
         * PEQT所用OPRF配置项
         */
        private OprfConfig peqtOprfConfig;
        /**
         * RCOT协议配置项
         */
        private RcotConfig rcotConfig;
        /**
         * OKVS类型
         */
        private OkvsType okvsType;
        /**
         * 流水线数量
         */
        private int pipeSize;

        public Builder() {
            rpmtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            peqtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            rcotConfig = RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = OkvsType.POLYNOMIAL;
            pipeSize = (1 << 8);
        }

        public Builder setRpmtOprfConfig(OprfConfig rpmtOprfConfig) {
            this.rpmtOprfConfig = rpmtOprfConfig;
            return this;
        }

        public Builder setPeqtOprfConfig(OprfConfig peqtOprfConfig) {
            this.peqtOprfConfig = peqtOprfConfig;
            return this;
        }

        public Builder setRcotConfig(RcotConfig rcotConfig) {
            this.rcotConfig = rcotConfig;
            return this;
        }

        public Builder setOkvsType(OkvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        public Builder setPipeSize(int pipeSize) {
            assert pipeSize > 0 : "Pipeline Size must be greater than 0: " + pipeSize;
            this.pipeSize = pipeSize;
            return this;
        }

        @Override
        public Krtw19OriPsuConfig build() {
            return new Krtw19OriPsuConfig(this);
        }
    }
}
