package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngineFactory.KyberType;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberParams;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;

/**
 * MR19-KYBER-基础OT协议配置项。论文来源：
 * <p>
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * </p>
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/05
 */
public class Mr19KyberBaseOtConfig implements BaseOtConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * Kyber安全系数
     */
    private final int paramsK;
    /**
     * Kyber方案类型
     */
    private final KyberType kyberType;

    private Mr19KyberBaseOtConfig(Mr19KyberBaseOtConfig.Builder builder) {
        assert KyberParams.validParamsK(builder.paramsK) : KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + builder.paramsK;
        envType = builder.envType;
        paramsK = builder.paramsK;
        kyberType = builder.kyberType;
    }

    @Override
    public BaseOtFactory.BaseOtType getPtoType() {
        return BaseOtFactory.BaseOtType.MR19_KYBER;
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

    public KyberType getKyberType() {
        return kyberType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mr19KyberBaseOtConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * Kyber安全系数
         */
        private int paramsK;
        /**
         * 方案类型
         */
        private KyberType kyberType;

        public Builder() {
            envType = EnvType.STANDARD;
            paramsK = 4;
            kyberType = KyberType.KYBER_CCA;
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setParamsK(int paramsK) {
            this.paramsK = paramsK;
            return this;
        }

        public Builder setKyberType(KyberType kyberType) {
            this.kyberType = kyberType;
            return this;
        }

        @Override
        public Mr19KyberBaseOtConfig build() {
            return new Mr19KyberBaseOtConfig(this);
        }
    }

}
