package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerNode;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.upsi.AbstractUpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiParams;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CMG21非平衡PSI协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class Cmg21UpsiServer<T> extends AbstractUpsiServer<T> {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * MP-OPRF协议发送方
     */
    private final MpOprfSender mpOprfSender;
    /**
     * 非平衡PSI方案参数
     */
    private Cmg21UpsiParams params;

    public Cmg21UpsiServer(Rpc serverRpc, Party clientParty, Cmg21UpsiConfig config) {
        super(Cmg21UpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        mpOprfSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        mpOprfSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        mpOprfSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        mpOprfSender.addLogLevel();
    }

    @Override
    public void init(UpsiParams upsiParams) throws MpcAbortException {
        setInitInput(upsiParams);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        assert (upsiParams instanceof Cmg21UpsiParams);
        params = (Cmg21UpsiParams) upsiParams;
        mpOprfSender.init(params.maxClientElementSize());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 服务端执行OPRF协议
        ArrayList<ByteBuffer> prfOutputList = oprf();
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // 接收客户端发送的Cuckoo hash key
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            hashKeyPayload.size() == params.getCuckooHashKeyNum(),
            "the size of hash keys " + "should be {}", params.getCuckooHashKeyNum()
        );
        byte[][] hashKeys = hashKeyPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashKeyTime);

        stopWatch.start();
        // 服务端哈希分桶
        List<ArrayList<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(prfOutputList, hashKeys);
        int binSize = hashBins.get(0).size();
        // 服务端将元素编码成多项式系数
        List<long[][]> encodeDatabase = encodeDatabase(hashBins, binSize);
        stopWatch.stop();
        long encodedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), encodedTime);

        stopWatch.start();
        // 接收客户端的加密密钥
        DataPacketHeader encryptionParamsDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> encryptionParams = rpc.receive(encryptionParamsDataPacketHeader).getPayload();
        // 接收客户端的加密查询信息
        DataPacketHeader receiverQueryDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryList = rpc.receive(receiverQueryDataPacketHeader).getPayload();
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), queryTime);

        stopWatch.start();
        // 服务端计算密文匹配结果
        List<byte[]> response = computeResponse(encodeDatabase, queryList, encryptionParams, binSize);
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        DataPacketHeader serverResponseDataPacketSpec = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseDataPacketSpec, response));
        info("{}{} Server Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), replyTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    /**
     * 生成完全哈希分桶。
     *
     * @param elementList 元素集合。
     * @param hashKeys    哈希算法密钥。
     * @return 完全哈希分桶。
     */
    private List<ArrayList<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(ArrayList<ByteBuffer> elementList,
                                                                              byte[][] hashKeys) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, params.getBinNum(),
            serverElementSize, hashKeys);
        completeHash.insertItems(elementList);
        int maxBinSize = completeHash.binSize(0);
        for (int i = 1; i < completeHash.binNum(); i++) {
            if (completeHash.binSize(i) > maxBinSize) {
                maxBinSize = completeHash.binSize(i);
            }
        }
        List<ArrayList<HashBinEntry<ByteBuffer>>> completeHashBins = new ArrayList<>();
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        for (int i = 0; i < completeHash.binNum(); i++) {
            ArrayList<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        return completeHashBins;
    }

    /**
     * 返回编码后的数据库。
     *
     * @param hashBins 哈希分桶。
     * @param binSize  分桶的元素数量。
     * @return 编码后的数据库（明文多项式的系数）。
     */
    private List<long[][]> encodeDatabase(List<ArrayList<HashBinEntry<ByteBuffer>>> hashBins, int binSize) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, (long) params.getPlainModulus());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        // we will split the hash table into partitions
        int partitionNum = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        int bigPartitionIndex = binSize / params.getMaxPartitionSizePerBin();
        long[][] coeffs = new long[params.getItemEncodedSlotSize() * itemPerCiphertext][];
        ArrayList<long[][]> coeffsPolys = new ArrayList<>();
        long[][] encodedItemArray = new long[params.getBinNum() * params.getItemEncodedSlotSize()][binSize];
        for (int i = 0; i < params.getBinNum(); i++) {
            IntStream intStream = parallel ? IntStream.range(0, binSize).parallel() : IntStream.range(0, binSize);
            int finalI = i;
            intStream.forEach(j -> {
                long[] item = params.getHashBinEntryEncodedArray(hashBins.get(finalI).get(j), false, secureRandom);
                for (int l = 0; l < params.getItemEncodedSlotSize(); l++) {
                    encodedItemArray[finalI * params.getItemEncodedSlotSize() + l][j] = item[l];
                }
            });
        }
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        for (int i = 0; i < ciphertextNum; i++) {
            for (int partition = 0; partition < partitionNum; partition++) {
                int partitionSize, partitionStart;
                partitionSize = partition < bigPartitionIndex ?
                    params.getMaxPartitionSizePerBin() : binSize % params.getMaxPartitionSizePerBin();
                partitionStart = params.getMaxPartitionSizePerBin() * partition;
                IntStream intStream = parallel ?
                    IntStream.range(0, itemPerCiphertext * params.getItemEncodedSlotSize()).parallel() :
                    IntStream.range(0, itemPerCiphertext * params.getItemEncodedSlotSize());
                int finalI = i;
                intStream.forEach(j -> {
                    long[] tempVector = new long[partitionSize];
                    System.arraycopy(encodedItemArray[finalI * itemPerCiphertext * params.getItemEncodedSlotSize() + j],
                        partitionStart, tempVector, 0, partitionSize);
                    coeffs[j] = zp64Poly.rootInterpolate(partitionSize, tempVector, 0L);
                });
                // 转换为列编码
                long[][] temp = new long[partitionSize + 1][params.getPolyModulusDegree()];
                for (int j = 0; j < partitionSize + 1; j++) {
                    for (int l = 0; l < itemPerCiphertext * params.getItemEncodedSlotSize(); l++) {
                        temp[j][l] = coeffs[l][j];
                    }
                    for (int l = itemPerCiphertext * params.getItemEncodedSlotSize(); l < params.getPolyModulusDegree();
                         l++) {
                        temp[j][l] = 0;
                    }
                }
                coeffsPolys.add(temp);
            }
        }
        return coeffsPolys;
    }

    /**
     * 服务端（发送方）执行MP-OPRF协议。
     *
     * @return 服务端元素的伪随机函数输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<ByteBuffer> oprf() throws MpcAbortException {
        MpOprfSenderOutput oprfSenderOutput = mpOprfSender.oprf(clientElementSize);
        IntStream intStream = parallel ?
            IntStream.range(0, serverElementSize).parallel() : IntStream.range(0, serverElementSize);
        return new ArrayList<>(Arrays.asList(intStream
            .mapToObj(i -> ByteBuffer.wrap(oprfSenderOutput.getPrf(serverElementArrayList.get(i).array())))
            .toArray(ByteBuffer[]::new)));
    }

    /**
     * 服务端计算密文匹配结果。
     *
     * @param plaintextPoly    明文多项式。
     * @param ciphertextPoly   密文多项式。
     * @param encryptionParams 加密方案参数。
     * @param binSize          分桶的元素数量。
     * @return 密文匹配结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private List<byte[]> computeResponse(List<long[][]> plaintextPoly, List<byte[]> ciphertextPoly,
                                         List<byte[]> encryptionParams, int binSize) throws MpcAbortException {
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int partitionCount = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        MpcAbortPreconditions.checkArgument(
            ciphertextPoly.size() == ciphertextNum * params.getQueryPowers().length,
            "The size of query is incorrect"
        );
        MpcAbortPreconditions.checkArgument(
            encryptionParams.size() == 2,
            "the size of encryption parameters should be 2"
        );
        // 计算所有的密文次方
        int[][] powerDegree;
        if (params.getPsLowDegree() > 0) {
            Set<Integer> innerPowersSet = new HashSet<>();
            Set<Integer> outerPowersSet = new HashSet<>();
            IntStream.range(0, params.getQueryPowers().length).forEach(i -> {
                if (params.getQueryPowers()[i] <= params.getPsLowDegree()) {
                    innerPowersSet.add(params.getQueryPowers()[i]);
                } else {
                    outerPowersSet.add(params.getQueryPowers()[i] / (params.getPsLowDegree() + 1));
                }
            });
            PowerNode[] innerPowerNodes = PowerUtils.computePowers(innerPowersSet, params.getPsLowDegree());
            PowerNode[] outerPowerNodes = PowerUtils.computePowers(
                outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1));
            powerDegree = new int[innerPowerNodes.length + outerPowerNodes.length][2];
            int[][] innerPowerNodesDegree = Arrays.stream(innerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            int[][] outerPowerNodesDegree = Arrays.stream(outerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            System.arraycopy(innerPowerNodesDegree, 0, powerDegree, 0, innerPowerNodesDegree.length);
            System.arraycopy(outerPowerNodesDegree, 0, powerDegree, innerPowerNodesDegree.length, outerPowerNodesDegree.length);
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(params.getQueryPowers())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            PowerNode[] powerNodes = PowerUtils.computePowers(sourcePowersSet, params.getMaxPartitionSizePerBin());
            powerDegree = Arrays.stream(powerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
        }
        IntStream queryIntStream = parallel ?
            IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum);
        List<byte[]> queryPowers = queryIntStream
            .mapToObj(i -> Cmg21UpsiNativeServer.computeEncryptedPowers(
                encryptionParams.get(0),
                encryptionParams.get(1),
                ciphertextPoly.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree())
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.getPsLowDegree() > 0) {
            return (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> Cmg21UpsiNativeServer.computeMatches(
                            encryptionParams.get(0),
                            encryptionParams.get(1),
                            plaintextPoly.get(i * partitionCount + j),
                            queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            params.getPsLowDegree())
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else if (params.getPsLowDegree() == 0) {
            return (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> Cmg21UpsiNativeServer.computeMatchesNaiveMethod(
                                encryptionParams.get(0),
                                encryptionParams.get(1),
                                plaintextPoly.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)
                            )
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            throw new MpcAbortException("ps_low_degree参数设置不正确");
        }
    }
}
