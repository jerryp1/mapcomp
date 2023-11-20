package edu.alibaba.mpc4j.s2pc.pjc.main.pmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapServer;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PmapMain4Batch {
    private static final Logger LOGGER = LoggerFactory.getLogger(PmapMain4Batch.class);
    /**
     * 协议类型名称
     */
    public static final String PTO_TYPE_NAME = "PMAP";
    /**
     * 预热元素字节长度
     */
    private static final int ELEMENT_BYTE_LENGTH = 16;
    /**
     * 预热
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
    /**
     * server stop watch
     */
    private final StopWatch serverStopWatch;
    /**
     * server stop watch
     */
    private final StopWatch clientStopWatch;
    /**
     * 配置参数
     */
    private final Properties properties;

    public PmapMain4Batch(Properties properties) {
        this.properties = properties;
        serverStopWatch = new StopWatch();
        clientStopWatch = new StopWatch();
    }

    public void runNetty(Rpc ownRpc) throws Exception {
        if (ownRpc.ownParty().getPartyId() == 0) {
            runServer(ownRpc, ownRpc.getParty(1));
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            runClient(ownRpc, ownRpc.getParty(0));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }

    public void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        PmapConfig config = PmapConfigUtils.createConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSize : setSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + PropertiesUtils.readString(properties, "append_string", "")
            + "_" + ELEMENT_BYTE_LENGTH * Byte.SIZE
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Element Size\tClient Element Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        // 启动测试
        int taskId = 0;
        // 预热
        warmupServer(serverRpc, clientParty, config, taskId);
        taskId++;
        // 正式测试
        for (int setSize : setSizes) {
            // 读取输入文件
            List<ByteBuffer> serverElementSet = new ArrayList<>(readServerElementSet(setSize));
            // 多线程
            runServer(serverRpc, clientParty, config, taskId, true, serverElementSet, setSize, printWriter);
            taskId++;
            // 单线程
            runServer(serverRpc, clientParty, config, taskId, false, serverElementSet, setSize, printWriter);
            taskId++;
        }
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readServerElementSet(int setSize) throws IOException {
        // 读取输入文件
        LOGGER.info("Server read element set, size = " + setSize);
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, ELEMENT_BYTE_LENGTH)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> serverElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        return serverElementSet;
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, PmapConfig config, int taskId) throws Exception {
        List<ByteBuffer> serverElementSet = new ArrayList<>(readServerElementSet(WARMUP_SET_SIZE));
        PmapServer<ByteBuffer> pmapServer = PmapFactory.createServer(serverRpc, clientParty, config);
        pmapServer.setTaskId(taskId);
        pmapServer.setParallel(true);
        pmapServer.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", pmapServer.ownParty().getPartyName());
        pmapServer.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        pmapServer.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pmapServer.ownParty().getPartyName());
        pmapServer.map(serverElementSet, WARMUP_SET_SIZE);
        pmapServer.getRpc().synchronize();
        pmapServer.getRpc().reset();
        pmapServer.destroy();
        LOGGER.info("(warmup) {} finish", pmapServer.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, PmapConfig config, int taskId, boolean parallel,
                           List<ByteBuffer> serverElementList, int clientSetSize,
                           PrintWriter printWriter) throws MpcAbortException {
        int serverSetSize = serverElementList.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        PmapServer<ByteBuffer> pmapServer = PmapFactory.createServer(serverRpc, clientParty, config);
        pmapServer.setTaskId(taskId);
        pmapServer.setParallel(parallel);
        // 启动测试
        pmapServer.getRpc().synchronize();
        pmapServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pmapServer.ownParty().getPartyName());
        serverStopWatch.start();
        pmapServer.init(serverSetSize, clientSetSize);
        serverStopWatch.stop();
        long initTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long initDataPacketNum = pmapServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pmapServer.getRpc().getPayloadByteLength();
        long initSendByteLength = pmapServer.getRpc().getSendByteLength();
        pmapServer.getRpc().synchronize();
        pmapServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pmapServer.ownParty().getPartyName());
        serverStopWatch.start();
        pmapServer.map(serverElementList, clientSetSize);
        serverStopWatch.stop();
        long ptoTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long ptoDataPacketNum = pmapServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pmapServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pmapServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pmapServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + pmapServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        pmapServer.getRpc().synchronize();
        pmapServer.getRpc().reset();
        pmapServer.destroy();
        LOGGER.info("{} finish", pmapServer.ownParty().getPartyName());
    }

    public void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        PmapConfig config = PmapConfigUtils.createConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSize : setSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + PropertiesUtils.readString(properties, "append_string", "")
            + "_" + ELEMENT_BYTE_LENGTH * Byte.SIZE
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // 启动测试
        int taskId = 0;
        // 预热
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int setSize : setSizes) {
            List<ByteBuffer> clientElementSet = new ArrayList<>(readClientElementSet(setSize));
            // 多线程
            runClient(clientRpc, serverParty, config, taskId, true, clientElementSet, setSize, printWriter);
            taskId++;
            // 单线程
            runClient(clientRpc, serverParty, config, taskId, false, clientElementSet, setSize, printWriter);
            taskId++;
        }
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readClientElementSet(int setSize) throws IOException {
        LOGGER.info("Client read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, ELEMENT_BYTE_LENGTH)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> clientElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        return clientElementSet;
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, PmapConfig config, int taskId) throws Exception {
        // 读取输入文件
        List<ByteBuffer> clientElementSet = new ArrayList<>(readClientElementSet(WARMUP_SET_SIZE));
        PmapClient<ByteBuffer> pmapClient = PmapFactory.createClient(clientRpc, serverParty, config);
        pmapClient.setTaskId(taskId);
        pmapClient.setParallel(true);
        pmapClient.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", pmapClient.ownParty().getPartyName());
        pmapClient.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        pmapClient.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pmapClient.ownParty().getPartyName());
        pmapClient.map(clientElementSet, WARMUP_SET_SIZE);
        pmapClient.getRpc().synchronize();
        pmapClient.getRpc().reset();
        pmapClient.destroy();
        LOGGER.info("(warmup) {} finish", pmapClient.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, PmapConfig config, int taskId, boolean parallel,
                           List<ByteBuffer> clientElementList, int serverSetSize,
                           PrintWriter printWriter) throws MpcAbortException {
        int clientSetSize = clientElementList.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        PmapClient<ByteBuffer> pmapClient = PmapFactory.createClient(clientRpc, serverParty, config);
        pmapClient.setTaskId(taskId);
        pmapClient.setParallel(parallel);
        // 启动测试
        pmapClient.getRpc().synchronize();
        pmapClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pmapClient.ownParty().getPartyName());
        clientStopWatch.start();
        pmapClient.init(clientSetSize, serverSetSize);
        clientStopWatch.stop();
        long initTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long initDataPacketNum = pmapClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pmapClient.getRpc().getPayloadByteLength();
        long initSendByteLength = pmapClient.getRpc().getSendByteLength();
        pmapClient.getRpc().synchronize();
        pmapClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pmapClient.ownParty().getPartyName());
        clientStopWatch.start();
        pmapClient.map(clientElementList, serverSetSize);
        clientStopWatch.stop();
        long ptoTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long ptoDataPacketNum = pmapClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pmapClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pmapClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pmapClient.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + pmapClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        pmapClient.getRpc().synchronize();
        pmapClient.getRpc().reset();
        pmapClient.destroy();
        LOGGER.info("{} finish", pmapClient.ownParty().getPartyName());
    }
}
