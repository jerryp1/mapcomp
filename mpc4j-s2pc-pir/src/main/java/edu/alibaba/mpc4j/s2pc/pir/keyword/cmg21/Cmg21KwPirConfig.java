package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;

/**
 * CMG21关键词索引PIR协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirConfig implements KwPirConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * 方案参数
     */
    private final Cmg21KwPirParams params;

    public Cmg21KwPirConfig(Builder builder) {
        this.envType = builder.envType;
        this.cuckooHashBinType = builder.cuckooHashBinType;
        this.params = builder.params;
    }

    /**
     * 返回布谷鸟哈希类型。
     *
     * @return 布谷鸟哈希类型。
     */
    public CuckooHashBinFactory.CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    /**
     * 返回关键字索引PIR方案参数。
     *
     * @return 关键字索引PIR方案参数。
     */
    public Cmg21KwPirParams getParams() {
        return params;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public KwPirFactory.PirType getProType() {
        return KwPirFactory.PirType.CMG21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cmg21KwPirConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
        /**
         * 方案参数
         */
        private Cmg21KwPirParams params;

        public Builder() {
            envType = EnvType.INLAND_JDK;
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setPirParams(Cmg21KwPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public Cmg21KwPirConfig build() {
            return new Cmg21KwPirConfig(this);
        }
    }
}
