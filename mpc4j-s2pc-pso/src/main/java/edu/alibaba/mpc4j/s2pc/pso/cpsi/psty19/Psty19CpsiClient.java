//package edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.Rpc;
//import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
//import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
//import edu.alibaba.mpc4j.common.tool.CommonConstants;
//import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
//import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
//import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
//import edu.alibaba.mpc4j.s2pc.pso.cpsi.AbstractCpsiClient;
//import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfReceiver;
//import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
//import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfReceiverOutput;
//
//import java.nio.ByteBuffer;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
///**
// * PSTY19协议客户端。
// *
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public class Psty19CpsiClient extends AbstractCpsiClient {
//    /**
//     * MP-OPRF协议接收方
//     */
//    private final MpOprfReceiver mpOprfReceiver;
//    /**
//     * 布谷鸟哈希类型
//     */
//    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
//    /**
//     * 布谷鸟哈希函数数量
//     */
//    private final int cuckooHashNum;
//    /**
//     * 无贮存区布谷鸟哈希分桶
//     */
//    private CuckooHashBin<ByteBuffer> cuckooHashBin;
//
//    public Psty19CpsiClient(Rpc clientRpc, Party serverParty, Psty19CpsiConfig config) {
//        super(Psty19CpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
//        mpOprfReceiver = OprfFactory.createMpOprfReceiver(clientRpc, serverParty, config.getMpOprfConfig());
//        mpOprfReceiver.addLogLevel();
//        cuckooHashBinType = config.getCuckooHashBinType();
//        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
//    }
//
//    @Override
//    public void setTaskId(long taskId) {
//        super.setTaskId(taskId);
//        mpOprfReceiver.setTaskId(taskId);
//    }
//
//    @Override
//    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
//        setInitInput(maxClientElementSize, maxServerElementSize);
//        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
//
//        stopWatch.start();
//        int maxOprfBatchNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize)
//            + CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxClientElementSize);
//        mpOprfReceiver.init(maxOprfBatchNum);
//        stopWatch.stop();
//        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);
//
//        initialized = true;
//        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
//    }
//
//    @Override
//    public HashMap<ByteBuffer, Boolean> psi(Set<ByteBuffer> clientElementSet, int serverElementSize,
//                                            int elementByteLength) throws MpcAbortException {
//        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
//        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
//
//        stopWatch.start();
//        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
//        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
//            taskId, getPtoDesc().getPtoId(), Psty19CpsiPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_KEY.ordinal(), extraInfo,
//            ownParty().getPartyId(), otherParty().getPartyId()
//        );
//        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
//        stopWatch.stop();
//        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        info("{}{} Client Step 1/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);
//
//
//        return null;
//    }
//
//    private List<byte[]> generateCuckooHashKeyPayload() {
//        // 设置布谷鸟哈希，如果发现不能构造成功，则可以重复构造
//        boolean success = false;
//        byte[][] cuckooHashKeys = null;
//        while (!success) {
//            try {
//                cuckooHashKeys = IntStream.range(0, cuckooHashNum)
//                    .mapToObj(hashIndex -> {
//                        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
//                        secureRandom.nextBytes(key);
//                        return key;
//                    })
//                    .toArray(byte[][]::new);
//                cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
//                    envType, cuckooHashBinType, clientElementSize, cuckooHashKeys
//                );
//                // 将客户端消息插入到CuckooHash中
//                cuckooHashBin.insertItems(clientElementArrayList);
//                if (cuckooHashBin.itemNumInStash() == 0) {
//                    success = true;
//                }
//            } catch (ArithmeticException ignored) {
//                // 如果插入不成功，就重新插入
//            }
//        }
//        // 如果成功，则向布谷鸟哈希的空余位置插入空元素
//        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
//        return Arrays.stream(cuckooHashKeys).collect(Collectors.toList());
//    }
//}
