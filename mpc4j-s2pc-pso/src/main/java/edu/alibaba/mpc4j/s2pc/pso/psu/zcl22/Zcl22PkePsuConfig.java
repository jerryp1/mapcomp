package edu.alibaba.mpc4j.s2pc.pso.psu.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc.EccOvdmFactory;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc.EccOvdmFactory.EccOvdmType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.zp.ZpOvdmFactory.ZpOvdmType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * ZCL22-PKE-PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl22PkePsuConfig implements PsuConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;
    /**
     * Zp-OVDM类型
     */
    private final ZpOvdmType zpOvdmType;
    /**
     * ECC-OVDM类型
     */
    private final EccOvdmType eccOvdmType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;
    /**
     * 流水线数量
     */
    private final int pipeSize;

    private Zcl22PkePsuConfig(Builder builder) {
        rcotConfig = builder.rcotConfig;
        zpOvdmType = builder.zpOvdmType;
        eccOvdmType = builder.eccOvdmType;
        compressEncode = builder.compressEncode;
        pipeSize = builder.pipeSize;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.ZCL22_PKE;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    public ZpOvdmType getZpOvdmType() {
        return zpOvdmType;
    }

    public EccOvdmType getEccOvdmType() {
        return eccOvdmType;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public int getPipeSize() {
        return pipeSize;
    }

    @Override
    public EnvType getEnvType() {
        return rcotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (rcotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = rcotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl22PkePsuConfig> {
        /**
         * RCOT协议配置项
         */
        private RcotConfig rcotConfig;
        /**
         * Zp-OVDM类型
         */
        private ZpOvdmType zpOvdmType;
        /**
         * ECC-OVDM类型
         */
        private EccOvdmType eccOvdmType;
        /**
         * 是否使用压缩椭圆曲线编码
         */
        private boolean compressEncode;
        /**
         * 流水线数量
         */
        private int pipeSize;

        public Builder() {
            rcotConfig = RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            zpOvdmType = ZpOvdmType.H3_SINGLETON_GCT;
            eccOvdmType = EccOvdmType.H3_SINGLETON_GCT;
            compressEncode = true;
            pipeSize = (1 << 8);
        }

        public Builder setRcotConfig(RcotConfig rcotConfig) {
            this.rcotConfig = rcotConfig;
            return this;
        }

        public Builder setEccOvdmType(EccOvdmType eccOvdmType) {
            this.eccOvdmType = eccOvdmType;
            this.zpOvdmType = EccOvdmFactory.getRelatedEccOvdmType(eccOvdmType);
            return this;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        public Builder setPipeSize(int pipeSize) {
            assert pipeSize > 0 : "Pipeline Size must be greater than 0: " + pipeSize;
            this.pipeSize = pipeSize;
            return this;
        }

        @Override
        public Zcl22PkePsuConfig build() {
            return new Zcl22PkePsuConfig(this);
        }
    }
}
