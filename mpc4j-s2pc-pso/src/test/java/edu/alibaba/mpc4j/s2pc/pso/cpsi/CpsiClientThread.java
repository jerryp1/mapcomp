//package edu.alibaba.mpc4j.s2pc.pso.cpsi;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//
//import java.nio.ByteBuffer;
//import java.util.Map;
//import java.util.Set;
//
//
///**
// * Circuit PSI协议客户端线程。
// *
// * @author Liqiang Peng
// * @date 2023/2/1
// */
//public class CpsiClientThread extends Thread {
//    /**
//     * 客户端
//     */
//    private final CpsiClient client;
//    /**
//     * 客户端集合
//     */
//    private final Set<ByteBuffer> clientElementSet;
//    /**
//     * 服务端元素数量
//     */
//    private final int serverElementSize;
//    /**
//     * 元素字节长度
//     */
//    private final int elementByteLength;
//    /**
//     * 客户端交集
//     */
//    private Map<ByteBuffer, Boolean> intersectionSet;
//
//    CpsiClientThread(CpsiClient client, Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength) {
//        this.client = client;
//        this.clientElementSet = clientElementSet;
//        this.serverElementSize = serverElementSize;
//        this.elementByteLength = elementByteLength;
//    }
//
//    @Override
//    public void run() {
//        try {
//            client.getRpc().connect();
//            client.init(clientElementSet.size(), serverElementSize);
//            client.getRpc().synchronize();
//            intersectionSet = client.psi(clientElementSet, serverElementSize, elementByteLength);
//            client.getRpc().disconnect();
//        } catch (MpcAbortException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public Map<ByteBuffer, Boolean> getIntersectionSet() {
//        return intersectionSet;
//    }
//}
