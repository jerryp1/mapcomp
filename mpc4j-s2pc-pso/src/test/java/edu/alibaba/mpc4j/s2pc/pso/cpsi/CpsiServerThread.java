//package edu.alibaba.mpc4j.s2pc.pso.cpsi;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiParams;
//import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiServer;
//
//import java.nio.ByteBuffer;
//import java.util.Map;
//import java.util.Set;
//
///**
// * Circuit PSI协议服务端线程。
// *
// * @author Liqiang Peng
// * @date 2023/2/1
// */
//public class CpsiServerThread extends Thread {
//    /**
//     * 服务端
//     */
//    private final CpsiServer server;
//    /**
//     * 服务端集合
//     */
//    private final Set<ByteBuffer> serverElementSet;
//    /**
//     * 客户端元素数量
//     */
//    private final int clientElementSize;
//    /**
//     * 元素字节长度
//     */
//    private final int elementByteLength;
//    /**
//     * 服务端交集
//     */
//    private Set<Boolean> intersectionSet;
//
//    CpsiServerThread(CpsiServer server, Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) {
//        this.server = server;
//        this.serverElementSet = serverElementSet;
//        this.clientElementSize = clientElementSize;
//        this.elementByteLength = elementByteLength;
//    }
//
//    @Override
//    public void run() {
//        try {
//            server.getRpc().connect();
//            server.init(serverElementSet.size(), clientElementSize);
//            server.getRpc().synchronize();
//            server.psi(serverElementSet, clientElementSize, elementByteLength);
//            server.getRpc().disconnect();
//        } catch (MpcAbortException e) {
//            e.printStackTrace();
//        }
//    }
//}
