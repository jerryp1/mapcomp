package edu.alibaba.mpc4j.s2pc.aby.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.AggTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.Amos22OneSideGroupConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class OneSideGroupMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(OneSideGroupMain.class);

    /**
     * 主函数。
     *
     * @param args 第一个输入是配置文件所在的目录
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // 读取配置文件
        LOGGER.info("read PTO config");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        properties.setProperty("own_name", args[1]);
        Rpc ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "server", "client");
        ownRpc.connect();

        Party otherParty = null;
        if (ownRpc.ownParty().getPartyId() == 0) {
            otherParty = ownRpc.getParty(1);
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            otherParty = ownRpc.getParty(0);
        }

        // 读取协议参数
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] logDomainSizes = PropertiesUtils.readLogIntArray(properties, "log_domain_size");
        int[] domainSizes = Arrays.stream(logDomainSizes).map(logDomainSize -> 1 << logDomainSize).toArray();
        int logMaxBatchNum = PropertiesUtils.readIntWithDefault(properties, "logMaxBatchNum", 24);
        int maxBatchNum = 1<<logMaxBatchNum;
        int aggAttrBitNum = PropertiesUtils.readIntWithDefault(properties, "aggAttrBitNum", 64);

        // 创建统计结果文件
        String filePath = "OneSideGroup"
            + "_" + args[1]
            + "_" + logMaxBatchNum
            + "_" + aggAttrBitNum
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Element Size\tbitmapLen\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", ownRpc.ownParty().getPartyName());
        // 启动测试
        int taskId = 0;

        SecureRandom secureRandom = new SecureRandom();
        Amos22OneSideGroupConfig config = new Amos22OneSideGroupConfig.Builder(false).build();
        for(int setSize : setSizes){
            SquareZ2Vector[] attr = IntStream.range(0, aggAttrBitNum).mapToObj(i ->
                SquareZ2Vector.createRandom(setSize, secureRandom)).toArray(SquareZ2Vector[]::new);
            BitVector gFlag = ownRpc.ownParty().getPartyId() == 0 ? BitVectorFactory.createRandom(setSize, secureRandom) : null;
            SquareZ2Vector eFlag = SquareZ2Vector.createRandom(setSize, secureRandom);
            for(int domainSize : domainSizes){
                LOGGER.info("setSize:{}, domainSize:{}", setSize, domainSize);
                SquareZ2Vector[] bitmap = IntStream.range(0, domainSize).mapToObj(i ->
                    SquareZ2Vector.createRandom(setSize, secureRandom)).toArray(SquareZ2Vector[]::new);
                // 多线程
                runParty(ownRpc, otherParty, config, taskId, true, attr, eFlag, bitmap, gFlag, maxBatchNum, printWriter);
                taskId++;
//                // 单线程
//                runParty(ownRpc, otherParty, config, taskId, false, attr, eFlag, bitmap, gFlag, maxBatchNum, printWriter);
//                taskId++;
            }
        }

        ownRpc.disconnect();
        System.exit(0);
    }

    private static void runParty(Rpc ownRpc, Party otherParty, Amos22OneSideGroupConfig config, int taskId, boolean parallel,
                                 SquareZ2Vector[] attr, SquareZ2Vector eFlag, SquareZ2Vector[] bitmap, BitVector gFlag,
                                 int maxBatchNum, PrintWriter printWriter) throws MpcAbortException {
        int dataSize = eFlag.getNum();
        int attrBitLen = attr.length;
        int bitmapLen = bitmap.length;
        StopWatch stopWatch = new StopWatch();
        Z2cParty z2cParty;
        OneSideGroupParty oneSideGroupParty;
        if(ownRpc.ownParty().getPartyId() != 0){
            z2cParty = Z2cFactory.createSender(ownRpc, otherParty, config.getZ2cConfig());
            oneSideGroupParty = OneSideGroupFactory.createSender(ownRpc, otherParty, config);
        }else{
            z2cParty = Z2cFactory.createReceiver(ownRpc, otherParty, config.getZ2cConfig());
            oneSideGroupParty = OneSideGroupFactory.createReceiver(ownRpc, otherParty, config);
        }
        z2cParty.setTaskId(taskId);
        z2cParty.setParallel(parallel);
        oneSideGroupParty.setTaskId(taskId);
        oneSideGroupParty.setParallel(parallel);
        // 启动测试
        ownRpc.synchronize();
        ownRpc.reset();
        // 初始化协议
        LOGGER.info("{} init", ownRpc.ownParty().getPartyName());
        stopWatch.start();
        z2cParty.init(bitmapLen * dataSize);
        oneSideGroupParty.init(bitmapLen, dataSize, attrBitLen);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = ownRpc.getSendDataPacketNum();
        long initPayloadByteLength = ownRpc.getPayloadByteLength();
        long initSendByteLength = ownRpc.getSendByteLength();
        ownRpc.synchronize();
        ownRpc.reset();

        // 执行协议
        LOGGER.info("{} execute", ownRpc.ownParty().getPartyName());
        stopWatch.start();
        int maxParallelGroupNum = Math.max(maxBatchNum / dataSize, 1);
        int batchNum = Math.max(bitmapLen / maxParallelGroupNum, 1);
        batchNum += (bitmapLen > maxParallelGroupNum && bitmapLen % maxParallelGroupNum > 0) ? 1 : 0;
        for(int i = 0; i < batchNum; i++){
            LOGGER.info("execute batch number {}/{} for data size:{}, bitmap domain:{}, maxBatchNum:{}", i, batchNum, dataSize, bitmapLen, maxBatchNum);
            int startIndex = i * maxParallelGroupNum;
            int endIndex = Math.min(i * maxParallelGroupNum + maxParallelGroupNum, bitmapLen);
            SquareZ2Vector[][] data = IntStream.range(startIndex, endIndex).mapToObj(bath ->
                Arrays.copyOf(attr, attr.length)).toArray(SquareZ2Vector[][]::new);
            SquareZ2Vector[] vFlags = Arrays.copyOfRange(bitmap, startIndex, endIndex);
            SquareZ2Vector[] eFlags = IntStream.range(startIndex, endIndex).mapToObj(bath -> eFlag.copy()).toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] f = z2cParty.and(vFlags, eFlags);
            AggTypes[] aggTypes = IntStream.range(startIndex, endIndex).mapToObj(bath -> AggTypes.MAX).toArray(AggTypes[]::new);
            oneSideGroupParty.groupAgg(data, f, aggTypes, gFlag);
        }

        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = ownRpc.getSendDataPacketNum();
        long ptoPayloadByteLength = ownRpc.getPayloadByteLength();
        long ptoSendByteLength = ownRpc.getSendByteLength();
        // 写入统计结果
        String info = ownRpc.ownParty().getPartyId()
            + "\t" + dataSize
            + "\t" + bitmapLen
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        ownRpc.synchronize();
        ownRpc.reset();
        z2cParty.destroy();
        oneSideGroupParty.destroy();
        LOGGER.info("{} finish", ownRpc.ownParty().getPartyName());
    }
}
