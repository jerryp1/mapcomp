package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mrkyber19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotFactory;

/**
 * MRKYBER19-基础n选1-OT协议配置项。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 *
 * @author Sheng Hu
 * @date 2022/08/25
 */
public class MrKyber19BnotConfig implements BnotConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * Kyber安全系数
     */
    private final int paramsK;

    private MrKyber19BnotConfig(MrKyber19BnotConfig.Builder builder) {
        envType = builder.envType;
        paramsK = builder.paramsK;
    }

    @Override
    public BnotFactory.BnotType getPtoType() {
        return BnotFactory.BnotType.MRKYBER19;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.MALICIOUS;
    }

    public int getParamsK() {
        return paramsK;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MrKyber19BnotConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * Kyber安全系数
         */
        private int paramsK;

        public Builder() {
            super();
            envType = EnvType.STANDARD;
            paramsK = 4;
        }

        public MrKyber19BnotConfig.Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public MrKyber19BnotConfig.Builder setParamsK(int paramsK) {
            this.paramsK = paramsK;
            return this;
        }

        @Override
        public MrKyber19BnotConfig build() {
            return new MrKyber19BnotConfig(this);
        }
    }
}
