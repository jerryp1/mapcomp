package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.ra17.Ra17MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class EcdhEccSqOprfConfig implements SqOprfConfig {

    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;
    /**
     * 环境类型
     */
    private EnvType envType;

    private EcdhEccSqOprfConfig(EcdhEccSqOprfConfig.Builder builder) {
        compressEncode = builder.compressEncode;
        envType = EnvType.STANDARD;
    }

    @Override
    public SqOprfFactory.SqOprfType getPtoType() {
        return SqOprfFactory.SqOprfType.ECDH_ECC;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<EcdhEccSqOprfConfig> {
        /**
         * 是否使用压缩椭圆曲线编码
         */
        private boolean compressEncode;

        public Builder() {
            compressEncode = true;
        }

        public EcdhEccSqOprfConfig.Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public EcdhEccSqOprfConfig build() {
            return new EcdhEccSqOprfConfig(this);
        }
    }
}
