//package edu.alibaba.mpc4j.s2pc.pir.batchpir.mr23;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirServer;
//import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirServer;
//import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirServer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * MR23批量索引PIR协议服务端线程。
// *
// * @author Liqiang Peng
// * @date 2023/3/31
// */
//public class Mr23BatchPirServerThread extends Thread {
//    private static final Logger LOGGER = LoggerFactory.getLogger(Mr23BatchPirServerThread.class);
//    /**
//     * 服务端
//     */
//    private final Mr23BatchIndexPirServer server;
//    /**
//     * 服务端元素数组
//     */
//    private final byte[][] serverElementArray;
//    /**
//     * 元素比特长度
//     */
//    private final int elementBitLength;
//    /**
//     * 支持的最大查询数目
//     */
//    private final int maxRetrievalSize;
//
//    Mr23BatchPirServerThread(Mr23BatchIndexPirServer server, byte[][] serverElementArray, int elementBitLength,
//                             int maxRetrievalSize) {
//        this.server = server;
//        this.serverElementArray = serverElementArray;
//        this.elementBitLength = elementBitLength;
//        this.maxRetrievalSize = maxRetrievalSize;
//    }
//
//    @Override
//    public void run() {
//        try {
//            server.init(serverElementArray, elementBitLength, maxRetrievalSize);
//            LOGGER.info(
//                "Server: Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
//            );
//            server.getRpc().synchronize();
//            server.getRpc().reset();
//            server.pir();
//            LOGGER.info(
//                "Server: Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
//            );
//            server.getRpc().synchronize();
//            server.getRpc().reset();
//        } catch (MpcAbortException e) {
//            e.printStackTrace();
//        }
//    }
//}