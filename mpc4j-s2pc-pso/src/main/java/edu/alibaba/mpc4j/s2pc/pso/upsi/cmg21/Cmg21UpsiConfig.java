package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiFactory.UpsiType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;

/**
 * CMG21协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public class Cmg21UpsiConfig implements UpsiConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * MP-OPRF协议
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * 非平衡PSI方案参数
     */
    private final Cmg21UpsiParams params;

    public Cmg21UpsiConfig(Builder builder) {
        envType = builder.envType;
        cuckooHashBinType = builder.cuckooHashBinType;
        mpOprfConfig = builder.mpOprfConfig;
        params = builder.params;
    }

    /**
     * 返回布谷鸟哈希类型
     * @return 布谷鸟哈希类型
     */
    public CuckooHashBinFactory.CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    /**
     * 返回非平衡PSI方案参数
     * @return 非平衡PSI方案参数
     */
    public Cmg21UpsiParams getParams() {
        return params;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public UpsiType getPtoType() {
        return UpsiType.CMG21;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cmg21UpsiConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
        /**
         * MP-OPRF协议
         */
        private MpOprfConfig mpOprfConfig;
        /**
         * 非平衡PSI方案参数
         */
        private Cmg21UpsiParams params;

        public Builder() {
            envType = EnvType.INLAND_JDK;
            cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH;
            mpOprfConfig = OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setMpOprfConfig(MpOprfConfig mpOprfConfig) {
            this.mpOprfConfig = mpOprfConfig;
            return this;
        }

        public Builder setUpsiParams(Cmg21UpsiParams params) {
            this.params = params;
            return this;
        }

        @Override
        public Cmg21UpsiConfig build() {
            return new Cmg21UpsiConfig(this);
        }
    }
}
