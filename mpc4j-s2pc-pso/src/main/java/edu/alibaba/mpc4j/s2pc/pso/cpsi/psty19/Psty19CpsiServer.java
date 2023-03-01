//package edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.Rpc;
//import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
//import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
//import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
//import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
//import edu.alibaba.mpc4j.common.tool.okve.okvs.Okvs;
//import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
//import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
//import edu.alibaba.mpc4j.s2pc.pso.cpsi.AbstractCpsiServer;
//import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfSender;
//import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
//
//
//import java.nio.ByteBuffer;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
///**
// * PSTY19协议服务端。
// *
// * @author Liqiang Peng
// * @date 2023/1/30
// */
//public class Psty19CpsiServer extends AbstractCpsiServer {
//    /**
//     * MP-OPRF协议发送方
//     */
//    private final MpOprfSender mpOprfSender;
//    /**
//     * 布谷鸟哈希类型
//     */
//    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
//    /**
//     * 布谷鸟哈希函数数量
//     */
//    private final int cuckooHashNum;
//
//    public Psty19CpsiServer(Rpc serverRpc, Party clientParty, Psty19CpsiConfig config) {
//        super(Psty19CpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
//        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
//        mpOprfSender.addLogLevel();
//        cuckooHashBinType = config.getCuckooHashBinType();
//        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
//    }
//
//    @Override
//    public void setTaskId(long taskId) {
//        super.setTaskId(taskId);
//        mpOprfSender.setTaskId(taskId);
//    }
//
//    @Override
//    public void setParallel(boolean parallel) {
//        super.setParallel(parallel);
//        mpOprfSender.setParallel(parallel);
//    }
//
//    @Override
//    public void addLogLevel() {
//        super.addLogLevel();
//        mpOprfSender.addLogLevel();
//    }
//
//    @Override
//    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
//        setInitInput(maxServerElementSize, maxClientElementSize);
//        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
//
//        stopWatch.start();
//        int maxOprfBatchNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize)
//            + CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxClientElementSize);
//        mpOprfSender.init(maxOprfBatchNum);
//        stopWatch.stop();
//        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        info("{}{} Server Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);
//
//
//
//        initialized = true;
//        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
//    }
//
//    @Override
//    public void psi(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) throws MpcAbortException {
//        setPtoInput(serverElementSet, clientElementSize, elementByteLength);
//        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
//
//
//        // 接收客户端发送的Cuckoo hash key
//        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
//            taskId, getPtoDesc().getPtoId(), Psty19CpsiPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_KEY.ordinal(), extraInfo,
//            otherParty().getPartyId(), rpc.ownParty().getPartyId()
//        );
//        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
//        MpcAbortPreconditions.checkArgument(
//            hashKeyPayload.size() == cuckooHashNum, "the size of hash keys " + "should be {}", cuckooHashNum
//        );
//
//        // 服务端哈希分桶
//        stopWatch.start();
//        RandomPadHashBin<ByteBuffer> completeHashBins = generateCompleteHashBin(hashKeyPayload.toArray(new byte[0][]));
//        stopWatch.stop();
//        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        info("{}{} Server Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashKeyTime);
//
//
//        stopWatch.start();
//
//        stopWatch.stop();
//        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        info("{}{} Server Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);
//
//    }
//
//
//
//    private RandomPadHashBin<ByteBuffer> generateCompleteHashBin(byte[][] hashKeys) {
//        RandomPadHashBin<ByteBuffer> completeHashBins = new RandomPadHashBin<>(
//            envType, cuckooHashNum, serverElementSize, hashKeys
//        );
//        completeHashBins.insertItems(serverElementArrayList);
//        return completeHashBins;
//    }
//
//    private List<byte[]> generateOkvsPayload() {
//        // 计算OKVS键值
//        Vector<byte[][]> keyArrayVector = IntStream.range(0, cuckooHashNum)
//            .mapToObj(hashIndex -> serverElementArrayList.stream()
//                .map(element -> {
//                    byte[] entryBytes = element.array();
//                    ByteBuffer extendEntryByteBuffer = ByteBuffer.allocate(entryBytes.length + Integer.BYTES);
//                    // y || i
//                    extendEntryByteBuffer.put(entryBytes);
//                    extendEntryByteBuffer.putInt(hashIndex);
//                    return extendEntryByteBuffer.array();
//                })
//                .toArray(byte[][]::new)
//            ).collect(Collectors.toCollection(Vector::new));
//
//
//            //
////        // For each j ∈ [m], Bob choose a random s_j.
////        sVector = IntStream.range(0, binNum)
////            .mapToObj(index -> {
////                byte[] si = new byte[Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH];
////                secureRandom.nextBytes(si);
////                return si;
////            })
////            .collect(Collectors.toCollection(Vector::new));
////        // 计算OKVS键值
////        Vector<byte[][]> keyArrayVector = IntStream.range(0, cuckooHashNum)
////            .mapToObj(hashIndex -> clientElementArrayList.stream()
////                .map(receiverElement -> {
////                    byte[] entryBytes = receiverElement.array();
////                    ByteBuffer extendEntryByteBuffer = ByteBuffer.allocate(entryBytes.length + Integer.BYTES);
////                    // y || i
////                    extendEntryByteBuffer.put(entryBytes);
////                    extendEntryByteBuffer.putInt(hashIndex);
////                    return extendEntryByteBuffer.array();
////                })
////                .toArray(byte[][]::new)
////            ).collect(Collectors.toCollection(Vector::new));
////        // Bob interpolates a polynomial P of degree < 3n such that for every y ∈ Y and i ∈ {1, 2, 3}, we have
////        // P(y || i) = s_{h_i(y)} ⊕ PRF(k_{h_i(y)}, y || i)
////        byte[][] valueArray = IntStream.range(0, cuckooHashNum)
////            .mapToObj(hashIndex -> {
////                // 计算OPRF有密码学运算，并发处理
////                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
////                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
////                return clientElementIntStream
////                    .mapToObj(clientElementIndex -> {
////                        byte[] clientElement = clientElementArrayList.get(clientElementIndex).array();
////                        byte[] extendBytes = keyArrayVector.elementAt(hashIndex)[clientElementIndex];
////                        int binIndex = binHashes[hashIndex].getInteger(clientElement, binNum);
////                        byte[] oprf = cuckooHashOprfSenderOutput.getPrf(binIndex, extendBytes);
////                        byte[] value = finiteFieldHash.digestToBytes(oprf);
////                        BytesUtils.xori(value, sVector.elementAt(binIndex));
////                        return value;
////                    })
////                    .toArray(byte[][]::new);
////            })
////            .flatMap(Arrays::stream)
////            .toArray(byte[][]::new);
////        ByteBuffer[] hashKeyArray = IntStream.range(0, cuckooHashNum)
////            .mapToObj(hashIndex -> {
////                // 计算OPRF有密码学运算，并发处理
////                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
////                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
////                return clientElementIntStream
////                    .mapToObj(clientElementIndex -> {
////                        byte[] extendBytes = keyArrayVector.elementAt(hashIndex)[clientElementIndex];
////                        return finiteFieldHash.digestToBytes(extendBytes);
////                    })
////                    .toArray(byte[][]::new);
////            })
////            .flatMap(Arrays::stream)
////            .map(ByteBuffer::wrap)
////            .toArray(ByteBuffer[]::new);
////        Map<ByteBuffer, byte[]> keyValueMap = IntStream.range(0, cuckooHashNum * clientElementSize)
////            .boxed()
////            .collect(Collectors.toMap(
////                index -> hashKeyArray[index],
////                index -> valueArray[index]
////            ));
////        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(
////            envType, okvsType, cuckooHashNum * clientElementSize,
////            Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH * Byte.SIZE, okvsHashKeys
////        );
////        // OKVS编码可以并行处理
////        okvs.setParallelEncode(parallel);
////        return Arrays.stream(okvs.encode(keyValueMap)).collect(Collectors.toList());
//        return null;
//    }
//}
