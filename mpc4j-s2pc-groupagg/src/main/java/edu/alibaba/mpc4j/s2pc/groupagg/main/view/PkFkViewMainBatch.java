package edu.alibaba.mpc4j.s2pc.groupagg.main.view;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.*;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author Feng Han
 * @date 2024/7/24
 */
public class PkFkViewMainBatch {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkFkViewMainBatch.class);
    /**
     * 协议类型名称
     */
    public static final String PTO_TYPE_NAME = "PK_FK_VIEW";
    /**
     * key字节长度
     */
    private static final int ELEMENT_BYTE_LENGTH = 128;
    /**
     * warm up payload bit length
     */
    private static final int WARMUP_PAYLOAD_BIT_LENGTH = 128;
    /**
     * 预热
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
    /**
     * server stop watch
     */
    private final StopWatch senderStopWatch;
    /**
     * server stop watch
     */
    private final StopWatch receiverStopWatch;
    /**
     * 配置参数
     */
    private final Properties properties;
    /**
     * secure random
     */
    private final SecureRandom secureRandom;

    public PkFkViewMainBatch(Properties properties) {
        this.properties = properties;
        senderStopWatch = new StopWatch();
        receiverStopWatch = new StopWatch();
        secureRandom = new SecureRandom();
    }

    public void runNetty(Rpc ownRpc) throws Exception {
        if (ownRpc.ownParty().getPartyId() == 0) {
            runReceiver(ownRpc, ownRpc.getParty(1));
        } else {
            runSender(ownRpc, ownRpc.getParty(0));
        }
    }

    public void runReceiver(Rpc receiverRpc, Party senderParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", receiverRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取集合大小
        int[] logPayloadBitLens = PropertiesUtils.readLogIntArray(properties, "log_payload_bit_len");
        int[] bitLens = Arrays.stream(logPayloadBitLens).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", receiverRpc.ownParty().getPartyName());
        PkFkViewConfig config = PkFkViewConfigUtils.createConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", receiverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", receiverRpc.ownParty().getPartyName());
        for (int setSize : setSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        LOGGER.info("{} create result file", receiverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + PropertiesUtils.readString(properties, "append_string", "")
            + "_" + ELEMENT_BYTE_LENGTH * Byte.SIZE
            + "_" + receiverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tReceiver Element Size\tSender Element Size\tPayload Bit Length\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tGenerate  Time(ms)\tGenerate  DataPacket Num\tGenerate  Payload Bytes(B)\tGenerate  Send Bytes(B)"
            + "\tRefresh   Time(ms)\tRefresh   DataPacket Num\tRefresh   Payload Bytes(B)\tRefresh   Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", receiverRpc.ownParty().getPartyName());
        // 启动测试
        int taskId = 0;
        // 预热
        warmupReceiver(receiverRpc, senderParty, config, taskId);
        taskId++;
        // 正式测试
        for (int setSize : setSizes) {
            // 读取输入文件
            byte[][] serverKeys = readReceiverKeys(setSize);
            for (int bitlen : bitLens) {
                // 多线程
                runReceiver(receiverRpc, senderParty, config, taskId, true, serverKeys, setSize, bitlen, printWriter);
                taskId++;
                // 单线程
                runReceiver(receiverRpc, senderParty, config, taskId, false, serverKeys, setSize, bitlen, printWriter);
                taskId++;
            }
        }
        printWriter.close();
        fileWriter.close();
    }

    private byte[][] readReceiverKeys(int setSize) throws IOException {
        // 读取输入文件
        LOGGER.info("Receiver read element set, size = " + setSize);
        InputStreamReader inputStreamReader = new InputStreamReader(
            Files.newInputStream(Paths.get(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, ELEMENT_BYTE_LENGTH))),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        byte[][] serverKeys = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .map(ByteBuffer::array)
            .toArray(byte[][]::new);
        bufferedReader.close();
        inputStreamReader.close();
        return serverKeys;
    }

    private void warmupReceiver(Rpc receiverRpc, Party senderParty, PkFkViewConfig config, int taskId) throws Exception {
        byte[][] receiverKeys = readReceiverKeys(WARMUP_SET_SIZE);
        BitVector[] receiverPayload1 = IntStream.range(0, WARMUP_SET_SIZE)
            .mapToObj(i -> BitVectorFactory.createRandom(WARMUP_PAYLOAD_BIT_LENGTH, secureRandom))
            .toArray(BitVector[]::new);
        BitVector[] receiverPayload2 = IntStream.range(0, WARMUP_SET_SIZE)
            .mapToObj(i -> BitVectorFactory.createRandom(WARMUP_PAYLOAD_BIT_LENGTH, secureRandom))
            .toArray(BitVector[]::new);
        PkFkViewReceiver receiver = PkFkViewFactory.createPkFkViewReceiver(receiverRpc, senderParty, config);
        receiver.setTaskId(taskId);
        receiver.setParallel(true);
        receiver.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", receiver.ownParty().getPartyName());
        receiver.init(WARMUP_PAYLOAD_BIT_LENGTH, WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        // 执行协议
        LOGGER.info("(warmup generate) {} execute", receiver.ownParty().getPartyName());
        PkFkViewReceiverOutput out1 = receiver.generate(receiverKeys, receiverPayload1, WARMUP_SET_SIZE, WARMUP_PAYLOAD_BIT_LENGTH);
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        LOGGER.info("(warmup refresh) {} execute", receiver.ownParty().getPartyName());
        receiver.refresh(out1, receiverPayload2);
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        receiver.destroy();
        LOGGER.info("(warmup) {} finish", receiver.ownParty().getPartyName());
    }

    private void runReceiver(Rpc receiverRpc, Party senderParty, PkFkViewConfig config, int taskId, boolean parallel,
                             byte[][] receiverKeys, int senderSize, int payloadBitLen, PrintWriter printWriter) throws MpcAbortException {
        int receiverSize = receiverKeys.length;
        LOGGER.info(
            "{}: receiverSize = {}, senderSize = {}, payloadBitLen ={}, parallel = {}",
            receiverRpc.ownParty().getPartyName(), receiverSize, senderSize, payloadBitLen, parallel
        );
        BitVector[] receiverPayload1 = IntStream.range(0, receiverSize)
            .mapToObj(i -> BitVectorFactory.createRandom(payloadBitLen, secureRandom))
            .toArray(BitVector[]::new);
        BitVector[] receiverPayload2 = IntStream.range(0, receiverSize)
            .mapToObj(i -> BitVectorFactory.createRandom(payloadBitLen, secureRandom))
            .toArray(BitVector[]::new);
        PkFkViewReceiver receiver = PkFkViewFactory.createPkFkViewReceiver(receiverRpc, senderParty, config);
        receiver.setTaskId(taskId);
        receiver.setParallel(parallel);
        // 启动测试
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", receiver.ownParty().getPartyName());
        senderStopWatch.start();
        receiver.init(payloadBitLen, senderSize, receiverSize);
        senderStopWatch.stop();
        long initTime = senderStopWatch.getTime(TimeUnit.MILLISECONDS);
        senderStopWatch.reset();
        long initDataPacketNum = receiver.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = receiver.getRpc().getPayloadByteLength();
        long initSendByteLength = receiver.getRpc().getSendByteLength();
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute generate", receiver.ownParty().getPartyName());
        senderStopWatch.start();
        PkFkViewReceiverOutput out1 = receiver.generate(receiverKeys, receiverPayload1, senderSize, payloadBitLen);
        senderStopWatch.stop();
        long ptoTime = senderStopWatch.getTime(TimeUnit.MILLISECONDS);
        senderStopWatch.reset();
        long ptoDataPacketNum = receiver.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = receiver.getRpc().getPayloadByteLength();
        long ptoSendByteLength = receiver.getRpc().getSendByteLength();
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        // 执行refresh
        LOGGER.info("{} execute refresh", receiver.ownParty().getPartyName());
        senderStopWatch.start();
        receiver.refresh(out1, receiverPayload2);
        senderStopWatch.stop();
        long refreshTime = senderStopWatch.getTime(TimeUnit.MILLISECONDS);
        senderStopWatch.reset();
        long refreshDataPacketNum = receiver.getRpc().getSendDataPacketNum();
        long refreshPayloadByteLength = receiver.getRpc().getPayloadByteLength();
        long refreshSendByteLength = receiver.getRpc().getSendByteLength();
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        // 写入统计结果
        String info = receiver.ownParty().getPartyId()
            + "\t" + receiverSize + "\t" + senderSize + "\t" + payloadBitLen
            + "\t" + receiver.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength
            + "\t" + refreshTime + "\t" + refreshDataPacketNum + "\t" + refreshPayloadByteLength + "\t" + refreshSendByteLength;
        printWriter.println(info);
        // 同步
        receiver.destroy();
        LOGGER.info("{} finish", receiver.ownParty().getPartyName());
    }

    public void runSender(Rpc senderRpc, Party receiverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", senderRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取集合大小
        int[] logPayloadBitLens = PropertiesUtils.readLogIntArray(properties, "log_payload_bit_len");
        int[] bitLens = Arrays.stream(logPayloadBitLens).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", senderRpc.ownParty().getPartyName());
        PkFkViewConfig config = PkFkViewConfigUtils.createConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", senderRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", senderRpc.ownParty().getPartyName());
        for (int setSize : setSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", senderRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + PropertiesUtils.readString(properties, "append_string", "")
            + "_" + ELEMENT_BYTE_LENGTH * Byte.SIZE
            + "_" + senderRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tPayload Bit Length\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tGenerate  Time(ms)\tGenerate  DataPacket Num\tGenerate  Payload Bytes(B)\tGenerate  Send Bytes(B)"
            + "\tRefresh   Time(ms)\tRefresh   DataPacket Num\tRefresh   Payload Bytes(B)\tRefresh   Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", senderRpc.ownParty().getPartyName());
        // 启动测试
        int taskId = 0;
        // 预热
        warmupSender(senderRpc, receiverParty, config, taskId);
        taskId++;
        for (int setSize : setSizes) {
            byte[][] senderKeys = readSenderKeys(setSize);
            for(int bitLen : bitLens){
                // 多线程
                runSender(senderRpc, receiverParty, config, taskId, true, senderKeys, setSize, bitLen, printWriter);
                taskId++;
                // 单线程
                runSender(senderRpc, receiverParty, config, taskId, false, senderKeys, setSize, bitLen, printWriter);
                taskId++;
            }

        }
        printWriter.close();
        fileWriter.close();
    }

    private byte[][] readSenderKeys(int setSize) throws IOException {
        LOGGER.info("Sender read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            Files.newInputStream(Paths.get(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, ELEMENT_BYTE_LENGTH))),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        byte[][] clientKeys = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .map(ByteBuffer::array)
            .toArray(byte[][]::new);
        bufferedReader.close();
        inputStreamReader.close();
        return clientKeys;
    }

    private void warmupSender(Rpc senderRpc, Party receiverParty, PkFkViewConfig config, int taskId) throws Exception {
        // 读取输入文件
        byte[][] senderKeys = readSenderKeys(WARMUP_SET_SIZE);
        BitVector[] senderPayload1 = IntStream.range(0, WARMUP_SET_SIZE)
            .mapToObj(i -> BitVectorFactory.createRandom(WARMUP_PAYLOAD_BIT_LENGTH, secureRandom))
            .toArray(BitVector[]::new);
        BitVector[] senderPayload2 = IntStream.range(0, WARMUP_SET_SIZE)
            .mapToObj(i -> BitVectorFactory.createRandom(WARMUP_PAYLOAD_BIT_LENGTH, secureRandom))
            .toArray(BitVector[]::new);
        PkFkViewSender sender = PkFkViewFactory.createPkFkViewSender(senderRpc, receiverParty, config);
        sender.setTaskId(taskId);
        sender.setParallel(true);
        sender.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", sender.ownParty().getPartyName());
        sender.init(WARMUP_PAYLOAD_BIT_LENGTH, WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        // 执行协议
        LOGGER.info("(warmup generate) {} execute", sender.ownParty().getPartyName());
        PkFkViewSenderOutput out1 = sender.generate(senderKeys, senderPayload1, WARMUP_SET_SIZE);
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        // 执行协议
        LOGGER.info("(warmup refresh) {} execute", sender.ownParty().getPartyName());
        sender.refresh(out1, senderPayload2);
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        sender.destroy();
        LOGGER.info("(warmup) {} finish", sender.ownParty().getPartyName());
    }

    private void runSender(Rpc senderRpc, Party receiverParty, PkFkViewConfig config, int taskId, boolean parallel,
                           byte[][] senderKeys, int receiverSize, int payloadBitLen, PrintWriter printWriter) throws MpcAbortException {
        int senderSetSize = senderKeys.length;
        LOGGER.info(
            "{}: receiverSize = {}, senderSetSize = {}, payloadBitLen ={}, parallel = {}",
            senderRpc.ownParty().getPartyName(), receiverSize, senderSetSize, payloadBitLen, parallel
        );
        BitVector[] senderPayload1 = IntStream.range(0, receiverSize)
            .mapToObj(i -> BitVectorFactory.createRandom(payloadBitLen, secureRandom))
            .toArray(BitVector[]::new);
        BitVector[] senderPayload2 = IntStream.range(0, receiverSize)
            .mapToObj(i -> BitVectorFactory.createRandom(payloadBitLen, secureRandom))
            .toArray(BitVector[]::new);
        PkFkViewSender sender = PkFkViewFactory.createPkFkViewSender(senderRpc, receiverParty, config);
        sender.setTaskId(taskId);
        sender.setParallel(parallel);
        // 启动测试
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", sender.ownParty().getPartyName());
        receiverStopWatch.start();
        sender.init(payloadBitLen, senderSetSize, receiverSize);
        receiverStopWatch.stop();
        long initTime = receiverStopWatch.getTime(TimeUnit.MILLISECONDS);
        receiverStopWatch.reset();
        long initDataPacketNum = sender.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = sender.getRpc().getPayloadByteLength();
        long initSendByteLength = sender.getRpc().getSendByteLength();
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute generate", sender.ownParty().getPartyName());
        receiverStopWatch.start();
        PkFkViewSenderOutput out1 = sender.generate(senderKeys, senderPayload1, receiverSize);
        receiverStopWatch.stop();
        long ptoTime = receiverStopWatch.getTime(TimeUnit.MILLISECONDS);
        receiverStopWatch.reset();
        long ptoDataPacketNum = sender.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = sender.getRpc().getPayloadByteLength();
        long ptoSendByteLength = sender.getRpc().getSendByteLength();
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        // 执行refresh
        LOGGER.info("{} execute refresh", sender.ownParty().getPartyName());
        receiverStopWatch.start();
        sender.refresh(out1, senderPayload2);
        receiverStopWatch.stop();
        long refreshTime = receiverStopWatch.getTime(TimeUnit.MILLISECONDS);
        receiverStopWatch.reset();
        long refreshDataPacketNum = sender.getRpc().getSendDataPacketNum();
        long refreshPayloadByteLength = sender.getRpc().getPayloadByteLength();
        long refreshSendByteLength = sender.getRpc().getSendByteLength();
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        // 写入统计结果
        String info = sender.ownParty().getPartyId()
            + "\t" + receiverSize + "\t" + senderSetSize + "\t" + payloadBitLen
            + "\t" + sender.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength
            + "\t" + refreshTime + "\t" + refreshDataPacketNum + "\t" + refreshPayloadByteLength + "\t" + refreshSendByteLength;
        printWriter.println(info);
        // 同步
        sender.destroy();
        LOGGER.info("{} finish", sender.ownParty().getPartyName());
    }
}
