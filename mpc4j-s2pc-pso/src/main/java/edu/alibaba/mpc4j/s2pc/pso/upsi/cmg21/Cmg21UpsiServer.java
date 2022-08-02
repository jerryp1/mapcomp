package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.upsi.AbstractUpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.upsi.PolynomialUtils;
import edu.alibaba.mpc4j.s2pc.pso.upsi.PowersNode;

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
public class Cmg21UpsiServer extends AbstractUpsiServer {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 非平衡PSI方案参数
     */
    private final Cmg21UpsiParams params;
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 无贮存区布谷鸟哈希类型
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * MP-OPRF协议发送方
     */
    private final MpOprfSender mpOprfSender;

    public Cmg21UpsiServer(Rpc serverRpc, Party clientParty, Cmg21UpsiConfig config) {
        super(Cmg21UpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        this.envType = config.getEnvType();
        this.params = config.getParams();
        this.cuckooHashBinType = config.getCuckooHashBinType();
        this.mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        mpOprfSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        mpOprfSender.setParallel(parallel);
    }

    @Override
    public void init() throws MpcAbortException {
        stopWatch.start();
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        int maxClientElementSize = CuckooHashBinFactory.getMaxItemSize(cuckooHashBinType, params.getBinNum());
        setInitInput(maxClientElementSize);
        mpOprfSender.init(maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void psi(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        // 服务端执行OPRF协议
        stopWatch.start();
        ArrayList<ByteBuffer> prfOutputList = oprf();
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step OPRF 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        // 接收客户端发送的Cuckoo hash key
        info("{}{} Server receive cuckoo hash keys", ptoStepLogPrefix, getPtoDesc().getPtoName());
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(),
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getHashNum(), "the size of hash keys " +
            "should be {}", params.getHashNum());
        byte[][] hashKeys = hashKeyPayload.toArray(new byte[hashKeyPayload.size()][]);

        // 服务端哈希分桶
        stopWatch.start();
        List<ArrayList<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(prfOutputList, hashKeys);
        int binSize = hashBins.get(0).size();
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step complete hash 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), hashTime);

        // 服务端将元素编码成多项式系数
        stopWatch.start();
        List<long[][]> encodeDatabase = encodeDatabase(hashBins, binSize);
        stopWatch.stop();
        long encodedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step encode database 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), encodedTime);

        // 接收客户端的加密密钥
        info("{}{} Server receive encryption parameter and relinearization keys", ptoStepLogPrefix,
            getPtoDesc().getPtoName());
        DataPacketHeader encryptionParamsDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(),
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> encryptionParams = rpc.receive(encryptionParamsDataPacketHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encryptionParams.size() == 2, "the size of encryption parameters " +
            "should be 2");

        // 接收客户端的加密查询信息
        info("{}{} Server receive Client's query", ptoStepLogPrefix, getPtoDesc().getPtoName());
        DataPacketHeader receiverQueryDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(),
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryList = rpc.receive(receiverQueryDataPacketHeader).getPayload();
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        MpcAbortPreconditions.checkArgument(queryList.size() == ciphertextNum * params.getQueryPowers().length,
            "The size of query is incorrect");

        // 服务端计算密文匹配结果
        stopWatch.start();
        List<byte[]> response = computeResponse(encodeDatabase, queryList, encryptionParams, binSize);
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        DataPacketHeader serverResponseDataPacketSpec = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(),
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseDataPacketSpec, response));
        info("{}{} Server Step 4/4 (reply generation) ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), replyTime);

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
                    coeffs[j] = PolynomialUtils.polynomialFromRoots(tempVector, params.getPlainModulus());
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
     * 计算给定范围内的幂次方。
     *
     * @param sourcePowers 源幂次方。
     * @param upperBound   上界。
     * @return 给定范围内的幂次方。
     */
    private int[][] computePowers(Set<Integer> sourcePowers, int upperBound) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(upperBound > 1, "upperBound must be greater than 1 : " + upperBound);
        Set<Integer> targetPowers = IntStream.rangeClosed(1, upperBound)
            .boxed()
            .collect(Collectors.toCollection(HashSet::new));
        Integer[] sortSourcePowers = Arrays.stream(sourcePowers.toArray(new Integer[0]))
            .sorted()
            .toArray(Integer[]::new);
        MpcAbortPreconditions.checkArgument(sortSourcePowers[sortSourcePowers.length - 1] <= upperBound, "Source powers "
            + "must be a subset of target powers");
        PowersNode[] powersNodes = new PowersNode[upperBound];
        IntStream.range(0, sortSourcePowers.length)
            .forEach(i -> powersNodes[sortSourcePowers[i] - 1] = new PowersNode(sortSourcePowers[i], 0));
        int currDepth = 0;
        for (int currPower = 1; currPower <= upperBound; currPower++) {
            if (powersNodes[currPower - 1] != null) {
                continue;
            }
            int optimalDepth = currPower - 1;
            int optimalS1 = currPower - 1;
            int optimalS2 = 1;
            for (int s1 = 1; s1 <= targetPowers.size(); s1++) {
                if (s1 >= currPower) {
                    break;
                }
                int s2 = currPower - s1;
                if (!targetPowers.contains(s2)) {
                    continue;
                }
                int depth = Math.max(powersNodes[s1 - 1].depth, powersNodes[s2 - 1].depth) + 1;
                if (depth < optimalDepth) {
                    optimalDepth = depth;
                    optimalS1 = s1;
                    optimalS2 = s2;
                }
            }
            powersNodes[currPower - 1] = new PowersNode(currPower, optimalDepth, optimalS1, optimalS2);
            currDepth = Math.max(currDepth, optimalDepth);
        }
        return IntStream.range(0, upperBound).mapToObj(i -> {
            int[] parentPowers = new int[2];
            parentPowers[0] = powersNodes[i].leftParentPower;
            parentPowers[1] = powersNodes[i].rightParentPower;
            return parentPowers;
        }).toArray(int[][]::new);
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
        int partitionCount = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        // 计算所有的密文次方
        int[][] powers;
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
            int[][] innerPowers = computePowers(innerPowersSet, params.getPsLowDegree());
            int[][] outerPowers = computePowers(outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1));
            powers = new int[innerPowers.length + outerPowers.length][2];
            System.arraycopy(innerPowers, 0, powers, 0, innerPowers.length);
            System.arraycopy(outerPowers, 0, powers, innerPowers.length, outerPowers.length);
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(params.getQueryPowers())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            powers = computePowers(sourcePowersSet, params.getMaxPartitionSizePerBin());
        }
        IntStream queryIntStream = parallel ?
            IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum);
        List<byte[]> queryPowers = queryIntStream
            .mapToObj(i -> Cmg21UpsiNativeServer.computeEncryptedPowers(
                ciphertextPoly.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                encryptionParams.get(1),
                encryptionParams.get(0),
                powers,
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
                            plaintextPoly.get(i * partitionCount + j),
                            queryPowers.subList(i * powers.length, (i + 1) * powers.length),
                            encryptionParams.get(1),
                            encryptionParams.get(0),
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
                            plaintextPoly.get(i * partitionCount + j),
                            queryPowers.subList(i * powers.length, (i + 1) * powers.length),
                            encryptionParams.get(1),
                            encryptionParams.get(0))
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            throw new MpcAbortException("ps_low_degree参数设置不正确");
        }
    }
}
