//package edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19;
//
//import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
//import edu.alibaba.mpc4j.common.tool.EnvType;
//import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
//import edu.alibaba.mpc4j.s2pc.pso.cpsi.CpsiConfig;
//import edu.alibaba.mpc4j.s2pc.pso.cpsi.CpsiFactory;
//import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
//import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
//
//import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;
//
///**
// * PSTY19协议配置项。
// *
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public class Psty19CpsiConfig implements CpsiConfig {
//    /**
//     * MP-OPRF协议配置项
//     */
//    private final MpOprfConfig mpOprfConfig;
//    /**
//     * 布谷鸟哈希类型
//     */
//    private final CuckooHashBinType cuckooHashBinType;
//
//    public Psty19CpsiConfig(Builder builder) {
//        mpOprfConfig = builder.mpOprfConfig;
//        this.cuckooHashBinType = builder.cuckooHashBinType;
//    }
//
//    @Override
//    public void setEnvType(EnvType envType) {
//        mpOprfConfig.setEnvType(envType);
//    }
//
//    @Override
//    public EnvType getEnvType() {
//        return mpOprfConfig.getEnvType();
//    }
//
//    @Override
//    public SecurityModel getSecurityModel() {
//        return SecurityModel.SEMI_HONEST;
//    }
//
//    @Override
//    public CpsiFactory.CpsiType getPtoType() {
//        return CpsiFactory.CpsiType.PSTY19;
//    }
//
//    public MpOprfConfig getMpOprfConfig() {
//        return mpOprfConfig;
//    }
//
//    public CuckooHashBinType getCuckooHashBinType() {
//        return cuckooHashBinType;
//    }
//
//    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psty19CpsiConfig> {
//        /**
//         * MP-OPRF协议配置项
//         */
//        private MpOprfConfig mpOprfConfig;
//        /**
//         * 布谷鸟哈希类型
//         */
//        private CuckooHashBinType cuckooHashBinType;
//
//        public Builder() {
//            mpOprfConfig = OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST);
//            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
//        }
//
//        public Builder setMpOprfConfig(MpOprfConfig mpOprfConfig) {
//            this.mpOprfConfig = mpOprfConfig;
//            return this;
//        }
//
//        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
//            this.cuckooHashBinType = cuckooHashBinType;
//            return this;
//        }
//
//        @Override
//        public Psty19CpsiConfig build() {
//            return new Psty19CpsiConfig(this);
//        }
//    }
//}
