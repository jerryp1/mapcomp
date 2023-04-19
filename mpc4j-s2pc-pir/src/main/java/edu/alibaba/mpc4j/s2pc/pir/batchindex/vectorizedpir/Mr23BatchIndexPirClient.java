package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirClient;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirPtoDesc.*;

/**
 * VECTORIZED_BATCH_PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirClient extends AbstractBatchIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Vectorized Batch PIR方案参数
     */
    private Mr23BatchIndexPirParams params;
    /**
     * Vectorized Batch PIR方案内部参数
     */
    private Mr23BatchIndexPirInnerParams innerParams;
    /**
     * 无贮存区布谷鸟哈希分桶
     */
    private IntNoStashCuckooHashBin cuckooHashBin;
    /**
     * 哈希算法密钥
     */
    private byte[][] hashKeys;
    /**
     * 公钥
     */
    private byte[] publicKey;
    /**
     * 私钥
     */
    private byte[] secretKey;
    /**
     * 分桶内索引与原索引值映射
     */
    private Map<Integer, Integer> binIndexRetrievalIndexMap;
    /**
     * 哈希分桶
     */
    ArrayList<ArrayList<HashBinEntry<Integer>>> completeHashBins = new ArrayList<>();

    public Mr23BatchIndexPirClient(Rpc clientRpc, Party serverParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        if (serverElementSize <= (1 << 20)) {
            if (maxRetrievalSize <= 256) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_256;
            } else if (maxRetrievalSize <= 512) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_512;
            } else if (maxRetrievalSize <= 1024) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_2048;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        } else if (serverElementSize <= (1 << 22)) {
            if (maxRetrievalSize <= 256) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_256;
            } else if (maxRetrievalSize <= 512) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_512;
            } else if (maxRetrievalSize <= 1024) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_2048;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        } else if (serverElementSize <= (1 << 24)) {
            if (maxRetrievalSize <= 256) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_256;
            } else if (maxRetrievalSize <= 512) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_512;
            } else if (maxRetrievalSize <= 1024) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_2048;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        } else if (serverElementSize <= (1 << 26)) {
            if (maxRetrievalSize <= 256) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_26_RETRIEVAL_SIZE_256;
            } else if (maxRetrievalSize <= 512) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_26_RETRIEVAL_SIZE_512;
            } else if (maxRetrievalSize <= 1024) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_26_RETRIEVAL_SIZE_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_26_RETRIEVAL_SIZE_2048;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        }
        setInitInput(serverElementSize, elementBitLength, maxRetrievalSize, params.getPlainModulusBitLength() - 1);
        logPhaseInfo(PtoState.INIT_BEGIN);
        // 客户端接收服务端哈希密钥
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getHashNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        // 客户端模拟服务端分桶
        stopWatch.start();
        cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE, maxRetrievalSize, hashKeys
        );
        int maxBinSize = maxBinSize(cuckooHashBin.binNum());
        innerParams = new Mr23BatchIndexPirInnerParams(params, cuckooHashBin.binNum(), maxBinSize);
        System.out.println(innerParams);
        assert (params.getPolyModulusDegree() / 2) >= cuckooHashBin.binNum();
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        // 客户端生成BFV算法密钥
        stopWatch.start();
        List<byte[]> keyPairPayload = generateKeyPair();
        DataPacketHeader keyPairHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keyPairHeader, keyPairPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyGenTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, byte[]> pir(ArrayList<Integer> indexList) throws MpcAbortException {
        setPtoInput(indexList);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 客户端布谷鸟哈希分桶
        generateCuckooHashBin(indexList);
        // 更新每个分桶的检索值
        ArrayList<Integer> binIndexList = updateBinIndex();
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cuckooHashKeyTime, "Client generates cuckoo hash bin");

        stopWatch.start();
        System.out.println(parallel);
        // client generate queries for each hash bin
        IntStream intStream = IntStream.range(0, innerParams.getBinNum());
        intStream = parallel ? intStream.parallel() : intStream;
        List<long[][]> queries = intStream
            .mapToObj(i -> binIndexList.get(i) != -1 ? generateVectorizedPirQuery(i, binIndexList.get(i))
                    : new long[params.getDimension()][params.getPolyModulusDegree()])
            .collect(Collectors.toList());
        // merge queries
        List<long[][]> mergedQueries = mergeQueries(queries);
        // encrypt merged queries
        Stream<long[][]> queryStream = mergedQueries.stream();
        queryStream = parallel ? queryStream.parallel() : queryStream;
        List<byte[]> clientQueryPayload = queryStream
            .map(query ->
                Mr23BatchIndexPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, query)
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, genQueryTime, "Client generates query");

        // 客户端接收回复
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        // 客户端解密检索结果
        stopWatch.start();
        Map<Integer, byte[]> retrievalResult = handleServerResponse(responsePayload, binIndexList);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return retrievalResult;
    }

    /**
     * 更新索引值。
     *
     * @return 每个分桶的索引值。
     * @throws MpcAbortException 如果协议异常终止。
     */
    private ArrayList<Integer> updateBinIndex() throws MpcAbortException {
        ArrayList<Integer> binIndex = new ArrayList<>(innerParams.getBinNum());
        binIndexRetrievalIndexMap = new HashMap<>(retrievalSize);
        for (int i = 0; i < cuckooHashBin.binNum(); i++) {
            if (cuckooHashBin.getBinHashIndex(i) == -1) {
                binIndex.add(-1);
            } else {
                for (int j = 0; j < completeHashBins.get(i).size(); j++) {
                    if (completeHashBins.get(i).get(j).getItem() == cuckooHashBin.getBinEntry(i)) {
                        binIndex.add(j);
                        binIndexRetrievalIndexMap.put(i, cuckooHashBin.getBinEntry(i));
                        break;
                    }
                }
            }
        }
        MpcAbortPreconditions.checkArgument(
            binIndex.size() == cuckooHashBin.binNum() && binIndexRetrievalIndexMap.size() == retrievalSize
        );
        return binIndex;
    }

    /**
     * 客户端生产查询信息。
     *
     * @param binIndex       分桶索引值。
     * @param retrievalIndex 检索索引值。
     * @return 查询信息。
     */
    private long[][] generateVectorizedPirQuery(int binIndex, int retrievalIndex) {
        int[] temp = PirUtils.computeIndices(retrievalIndex, innerParams.getDimensionsSize());
        int[] permutedIndices = IntStream.range(0, params.getDimension())
            .map(i -> temp[params.getDimension() - 1 - i])
            .toArray();
        int[] indices = new int[params.getDimension()];
        int offset = (binIndex < (innerParams.getBinNum() / 2)) ? 0 : params.getPolyModulusDegree() / 2;
        for (int i = 0; i < params.getDimension(); i++) {
            indices[i] = permutedIndices[i];
            for (int j = 0; j < i; j++) {
                indices[i] = (indices[i] + permutedIndices[j]) % params.getFirstTwoDimensionSize();
            }
        }
        long[][] query = new long[params.getDimension()][params.getPolyModulusDegree()];
        IntStream.range(0, params.getDimension())
            .forEach(i -> query[i][indices[i] * innerParams.getGroupBinSize() + offset] = 1L);
        return query;
    }

    /**
     * 合并查询信息。
     *
     * @param queries 查询信息。
     * @return 合并后的查询信息。
     */
    private List<long[][]> mergeQueries(List<long[][]> queries) {
        int count = CommonUtils.getUnitNum(innerParams.getBinNum() / 2, innerParams.getGroupBinSize());
        List<long[][]> mergeQueries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long[][] query = new long[params.getDimension()][params.getPolyModulusDegree()];
            for (int j = 0; j < innerParams.getGroupBinSize(); j++) {
                if ((i * innerParams.getGroupBinSize() + j) >= (innerParams.getBinNum() / 2)) {
                    break;
                } else {
                    long[][] temp = PirUtils.plaintextRotate(queries.get(i * innerParams.getGroupBinSize() + j), j);
                    for (int l = 0; l < params.getDimension(); l++) {
                        for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                            query[l][k] += temp[l][k];
                        }
                    }
                    temp = PirUtils.plaintextRotate(queries.get(
                        i * innerParams.getGroupBinSize() + j + innerParams.getBinNum() / 2), j
                    );
                    for (int l = 0; l < params.getDimension(); l++) {
                        for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                            query[l][k] += temp[l][k];
                        }
                    }
                }
            }
            mergeQueries.add(query);
        }
        return mergeQueries;
    }

    /**
     * 客户端生成密钥对。
     *
     * @return 密钥对。
     */
    private ArrayList<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Mr23BatchIndexPirNativeUtils.keyGen(
            params.getEncryptionParams(), params.getFirstTwoDimensionSize()
        );
        assert (keyPair.size() == 4);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        byte[] relinKeys = keyPair.remove(0);
        byte[] galoisKeys = keyPair.remove(0);
        ArrayList<byte[]> keyPairPayload = new ArrayList<>();
        keyPairPayload.add(publicKey);
        keyPairPayload.add(relinKeys);
        keyPairPayload.add(galoisKeys);
        return keyPairPayload;
    }

    /**
     * 布谷鸟哈希分桶。
     *
     * @param indexList 检索值列表。
     */
    private void generateCuckooHashBin(ArrayList<Integer> indexList) {
        // 将客户端消息插入到CuckooHash中
        int[] indicesArray = IntStream.range(0, retrievalSize).map(indexList::get).toArray();
        cuckooHashBin.insertItems(indicesArray);
    }

    /**
     * 返回哈希分桶后最大桶内元素数目。
     *
     * @return 最大桶内元素数目。
     */
    private int maxBinSize(int binNum) {
        ArrayList<Integer> totalIndexList = IntStream.range(0, serverElementSize)
            .boxed()
            .collect(Collectors.toCollection(() -> new ArrayList<>(serverElementSize)));
        RandomPadHashBin<Integer> completeHash = new RandomPadHashBin<>(envType, binNum, serverElementSize, hashKeys);
        completeHash.insertItems(totalIndexList);
        int maxBinSize = completeHash.binSize(0);
        for (int i = 1; i < completeHash.binNum(); i++) {
            if (completeHash.binSize(i) > maxBinSize) {
                maxBinSize = completeHash.binSize(i);
            }
        }
        completeHashBins = new ArrayList<>();
        HashBinEntry<Integer> paddingEntry = HashBinEntry.fromEmptyItem(-1);
        for (int i = 0; i < completeHash.binNum(); i++) {
            ArrayList<HashBinEntry<Integer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        return maxBinSize;
    }

    /**
     * 处理服务端回复信息。
     *
     * @param serverResponse 服务端回复。
     * @param binIndex       分桶索引值。
     * @return 查询结果映射。
     * @throws MpcAbortException 如果协议异常终止。
     */
    private Map<Integer, byte[]> handleServerResponse(List<byte[]> serverResponse, List<Integer> binIndex)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() == partitionCount);
        BigInteger[] retrievalResult = IntStream.range(0, binIndex.size())
            .mapToObj(i -> BigInteger.ZERO)
            .toArray(BigInteger[]::new);
        IntStream intStream = IntStream.range(0, partitionCount);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            long[] coeffs = Mr23BatchIndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, serverResponse.get(i)
            );
            int shiftBits = partitionBitLength * i;
            for (int j = 0; j < binIndex.size(); j++) {
                if (binIndex.get(j) != -1) {
                    int offset =  (j < (binIndex.size()/2)) ? 0 : ((params.getPolyModulusDegree() - binIndex.size())/2);
                    BigInteger temp = BigInteger.valueOf(coeffs[j + offset]).shiftLeft(shiftBits);
                    retrievalResult[j] = retrievalResult[j].add(temp);
                }
            }
        });
        // 建立检索结果映射
        int byteLength = CommonUtils.getByteLength(elementBitLength);
        return IntStream.range(0, binIndex.size())
            .filter(i -> binIndex.get(i) != -1)
            .boxed()
            .collect(
                Collectors.toMap(
                    integer -> binIndexRetrievalIndexMap.get(integer),
                    integer -> BigIntegerUtils.nonNegBigIntegerToByteArray(retrievalResult[integer], byteLength),
                    (a, b) -> b,
                    () -> new HashMap<>(retrievalSize)
                )
            );
    }
}