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


    private MrKyber19BaseOtConfig(MrKyber19BaseOtConfig.Builder builder) {
        envType = builder.envType;
    }


    @Override
    public BaseOtFactory.BaseOtType getPtoType() { return BaseOtFactory.BaseOtType.MRKYBER19; }

    @Override
    public EnvType getEnvType() { return envType; }

    @Override
    public SecurityModel getSecurityModel() { return SecurityModel.MALICIOUS; }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MrKyber19BaseOtConfig>{
        /**
         * 环境类型
         */
        private EnvType envType;

        public Builder() {
            super();
            envType = EnvType.STANDARD;
        }

        public MrKyber19BaseOtConfig.Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        @Override
        public MrKyber19BaseOtConfig build() {
            return new MrKyber19BaseOtConfig(this);
        }
    }

}
