//package edu.alibaba.mpc4j.s2pc.pso.cpsi;
//
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.Rpc;
//import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
//import edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19.Psty19CpsiClient;
//import edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19.Psty19CpsiConfig;
//import edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19.Psty19CpsiServer;
//
///**
// * Circuit PSI协议工厂。
// *
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public class CpsiFactory implements PtoFactory {
//    /**
//     * 私有构造函数
//     */
//    private CpsiFactory() {
//        // empty
//    }
//
//    /**
//     * Circuit PSI协议类型。
//     */
//    public enum CpsiType {
//        /**
//         * PSTY19方案
//         */
//        PSTY19,
//    }
//
//    /**
//     * 构建服务端。
//     *
//     * @param serverRpc   服务端通信接口。
//     * @param clientParty 客户端信息。
//     * @param config      配置项。
//     * @return 服务端。
//     */
//    public static CpsiServer createServer(Rpc serverRpc, Party clientParty, CpsiConfig config) {
//        CpsiType type = config.getPtoType();
//        //noinspection SwitchStatementWithTooFewBranches
//        switch (type) {
//            case PSTY19:
//                return new Psty19CpsiServer(serverRpc, clientParty, (Psty19CpsiConfig) config);
//            default:
//                throw new IllegalArgumentException("Invalid " + CpsiType.class.getSimpleName() + ": " + type.name());
//        }
//    }
//
//    /**
//     * 构建客户端。
//     *
//     * @param clientRpc   客户端通信接口。
//     * @param serverParty 服务端信息。
//     * @param config      配置项。
//     * @return 客户端。
//     */
//    public static CpsiClient createClient(Rpc clientRpc, Party serverParty, CpsiConfig config) {
//        CpsiType type = config.getPtoType();
//        //noinspection SwitchStatementWithTooFewBranches
//        switch (type) {
//            case PSTY19:
//                return new Psty19CpsiClient(clientRpc, serverParty, (Psty19CpsiConfig) config);
//            default:
//                throw new IllegalArgumentException("Invalid " + CpsiType.class.getSimpleName() + ": " + type.name());
//        }
//    }
//}
