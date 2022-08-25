package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mrkyber19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;

/**
 * MRKYBER19-基础OT协议配置项。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * @author Sheng Hu
 * @date 2022/08/05
 */
public class MrKyber19BaseOtConfig implements BaseOtConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * Kyber安全系数
     */
    private final int paramsK;

    private MrKyber19BaseOtConfig(MrKyber19BaseOtConfig.Builder builder) {
        envType = builder.envType;
        paramsK = builder.paramsK;
    }

    @Override
    public BaseOtFactory.BaseOtType getPtoType() { return BaseOtFactory.BaseOtType.MRKYBER19; }

    @Override
    public EnvType getEnvType() { return envType; }

    @Override
    public SecurityModel getSecurityModel() { return SecurityModel.MALICIOUS; }
    public int getParamsK() {return paramsK;}

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MrKyber19BaseOtConfig>{
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

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setParamsK(int paramsK){
            this.paramsK = paramsK;
            return this;
        }

        @Override
        public MrKyber19BaseOtConfig build() {
            return new MrKyber19BaseOtConfig(this);
        }
    }

}
