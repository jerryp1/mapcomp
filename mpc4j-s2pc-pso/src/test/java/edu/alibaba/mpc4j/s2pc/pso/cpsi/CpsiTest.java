//package edu.alibaba.mpc4j.s2pc.pso.cpsi;
//
//import edu.alibaba.mpc4j.common.rpc.Rpc;
//import edu.alibaba.mpc4j.common.rpc.RpcManager;
//import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
//import edu.alibaba.mpc4j.common.tool.CommonConstants;
//import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
//import edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19.Psty19CpsiConfig;
//import edu.alibaba.mpc4j.s2pc.pso.upsi.*;
//import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiConfig;
//import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiParams;
//import org.junit.Assert;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.nio.ByteBuffer;
//import java.security.SecureRandom;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
///**
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public class CpsiTest {
//    private static final Logger LOGGER = LoggerFactory.getLogger(CpsiTest.class);
//    /**
//     * 服务端
//     */
//    private final Rpc serverRpc;
//    /**
//     * 客户端
//     */
//    private final Rpc clientRpc;
//    /**
//     * 元素字节长度
//     */
//    private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
//
//    public CpsiTest() {
//        RpcManager rpcManager = new MemoryRpcManager(2);
//        serverRpc = rpcManager.getRpc(0);
//        clientRpc = rpcManager.getRpc(1);
//    }
//
//    @Test
//    public void testCpsiParallel() {
//        Psty19CpsiConfig config = new Psty19CpsiConfig.Builder().build();
//        testCpsi(config, 10, 10, true);
//    }
//
//    public void testCpsi(Psty19CpsiConfig config, int serverSetSize, int clientSetSize, boolean parallel) {
//        ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
//        Set<ByteBuffer> serverElementSet = sets.get(0);
//        Set<ByteBuffer> clientElementSet = sets.get(1);
//        // 创建参与方实例
//        CpsiServer server = CpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
//        CpsiClient client = CpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
//        long randomTaskId = Math.abs(new SecureRandom().nextLong());
//        server.setTaskId(randomTaskId);
//        client.setTaskId(randomTaskId);
//        // 设置并发
//        server.setParallel(parallel);
//        client.setParallel(parallel);
//        CpsiServerThread serverThread = new CpsiServerThread(
//            server, serverElementSet, clientSetSize, ELEMENT_BYTE_LENGTH
//        );
//        CpsiClientThread clientThread = new CpsiClientThread(
//            client, clientElementSet, serverSetSize, ELEMENT_BYTE_LENGTH
//        );
//        try {
//            // 开始执行协议
//            serverThread.start();
//            clientThread.start();
//            // 等待线程停止
//            serverThread.join();
//            clientThread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        // 验证结果
//        Map<ByteBuffer, Boolean> psiResult = clientThread.getIntersectionSet();
//        LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
//        LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
//        sets.get(0).retainAll(sets.get(1));
//    }
//
//}
