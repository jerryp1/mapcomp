//package edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19;
//
//import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
//import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
//
///**
// * PSTY19 Circuit PSI协议信息。论文来源：
// * <p>
// * Benny Pinkas and Thomas Schneider and Oleksandr Tkachenko and Avishay Yanai.
// * Efficient Circuit-Based PSI with Linear Communication. EUROCRYPT 2019, Part III, pp. 122-153.
// * </p>
// *
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public class Psty19CpsiPtoDesc implements PtoDesc {
//    private static final int PTO_ID = Math.abs((int) 2994073248836271092L);
//
//    /**
//     * 协议名称
//     */
//    private static final String PTO_NAME = "PSTY19_CPSI";
//
//    /**
//     * 协议步骤
//     */
//    enum PtoStep {
//        /**
//         * 客户端发送布谷鸟哈希密钥
//         */
//        CLIENT_SEND_CUCKOO_HASH_KEY,
//        /**
//         * 客户端发送加密方案密钥
//         */
//        CLIENT_SEND_ENCRYPTION_PARAMS,
//        /**
//         * 客户端发送加密查询
//         */
//        CLIENT_SEND_QUERY,
//        /**
//         * 服务端返回密文匹配结果
//         */
//        SERVER_SEND_RESPONSE,
//    }
//
//    /**
//     * 单例模式
//     */
//    private static final Psty19CpsiPtoDesc INSTANCE = new Psty19CpsiPtoDesc();
//
//    /**
//     * 私有构造函数
//     */
//    private Psty19CpsiPtoDesc() {
//        // empty
//    }
//
//    public static PtoDesc getInstance() {
//        return INSTANCE;
//    }
//
//    static {
//        PtoDescManager.registerPtoDesc(getInstance());
//    }
//
//    @Override
//    public int getPtoId() {
//        return PTO_ID;
//    }
//
//    @Override
//    public String getPtoName() {
//        return PTO_NAME;
//    }
//}
