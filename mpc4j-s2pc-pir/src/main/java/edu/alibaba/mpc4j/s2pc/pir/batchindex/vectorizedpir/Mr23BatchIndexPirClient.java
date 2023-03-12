package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     * Vectorized PIR方案参数
     */
    private Mr23BatchIndexPirParams params;
    /**
     * Vectorized PIR方案内部参数
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
     * 移位
     */
    private int[] offset;

    ArrayList<ArrayList<HashBinEntry<Integer>>> completeHashBins = new ArrayList<>();

    public Mr23BatchIndexPirClient(Rpc clientRpc, Party serverParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(serverElementSize, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        params = Mr23BatchIndexPirParams.DEFAULT_PARAMS;
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
        int binNum = (int) Math.ceil(1.5 * maxRetrievalSize);
        int binSize = generateHashBin(binNum);
        innerParams = new Mr23BatchIndexPirInnerParams(params, binNum, binSize);
        offset = new int[binNum];
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        // 客户端生成BFV算法密钥和参数
        stopWatch.start();
        List<byte[]> keyPairPayload = generateKeyPair();
        DataPacketHeader keyPairHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
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
    public Map<Integer, ByteBuffer> pir(ArrayList<Integer> indices) throws MpcAbortException {
        setPtoInput(indices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 客户端布谷鸟哈希分桶
        generateCuckooHashBin(indices);
        // 更新每个分桶的检索值
        ArrayList<Integer> binIndex = new ArrayList<>(innerParams.getBinNum());
        Map<Integer, Integer> binIndexMap = new HashMap<>(retrievalSize);
        for (int i = 0; i < cuckooHashBin.binNum(); i++) {
            if (cuckooHashBin.getBinHashIndex(i) == -1) {
                binIndex.add(-1);
            } else {
                int tempIndex = cuckooHashBin.getBinEntry(i);
                for (int j = 0; j < completeHashBins.get(i).size(); j++) {
                    if (completeHashBins.get(i).get(j).getItem() == tempIndex) {
                        binIndex.add(j);
                        binIndexMap.put(i, tempIndex);
                        break;
                    }
                }
            }
        }
        assert binIndex.size() == cuckooHashBin.binNum();
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cuckooHashKeyTime, "Client generates cuckoo hash bin");

        stopWatch.start();
        // 客户端生成查询信息
        IntStream intStream = IntStream.range(0, innerParams.getBinNum());
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> clientQueryPayload = intStream
            .mapToObj(i -> {
                int retrievalIndex = binIndex.get(i) != -1 ? binIndex.get(i) : 0;
                return generateQuery(retrievalIndex, i);
            })
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
        IntStream responseStream = IntStream.range(0, innerParams.getBinNum());
        responseStream = parallel ? responseStream.parallel() : responseStream;
        List<byte[]> result = responseStream
            .mapToObj( i -> handleServerResponsePayload(responsePayload.get(i), i))
            .collect(Collectors.toList());
        Map<Integer, ByteBuffer> pirResult = IntStream.range(0, binIndex.size())
            .filter(i -> binIndex.get(i) != -1)
            .boxed()
            .collect(
                Collectors.toMap(
                    binIndexMap::get,
                    i -> ByteBuffer.wrap(result.get(i)),
                    (a, b) -> b,
                    () -> new HashMap<>(retrievalSize)
                )
            );
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * 客户端生产查询信息。
     *
     * @param retrievalIndex 检索值。
     * @param binIndex       分桶索引。
     * @return 查询信息。
     */
    public ArrayList<byte[]> generateQuery(int retrievalIndex, int binIndex) {
        int[] dimensionLength = IntStream.range(0, params.getDimension())
            .map(i -> innerParams.getDimensionsLength())
            .toArray();
        int[] temp = IndexPirUtils.computeIndices(retrievalIndex, dimensionLength);
        int[] permutedIndices = IntStream.range(0, params.getDimension())
            .map(i -> temp[params.getDimension() - 1 - i])
            .toArray();
        int[] indices = new int[params.getDimension()];
        for (int i = 0; i < params.getDimension(); i++) {
            indices[i] = permutedIndices[i];
            for (int j = 0; j < i; j++) {
                indices[i] = (indices[i] + permutedIndices[j]) % innerParams.getSlotNum();
            }
        }
        this.offset[binIndex] = indices[params.getDimension() - 1];
        return Mr23BatchIndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, innerParams.getSlotNum()
        );
    }



    /**
     * 客户端生成密钥对。
     */
    private ArrayList<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Mr23BatchIndexPirNativeUtils.keyGen(
            params.getEncryptionParams(), innerParams.getDimensionsLength(), innerParams.getSlotNum()
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
     * 生成布谷鸟哈希分桶。
     *
     * @param indices 检索值。
     */
    private void generateCuckooHashBin(ArrayList<Integer> indices) {
        // 初始化布谷鸟哈希
        cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType,
            IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE,
            retrievalSize,
            innerParams.getBinNum(),
            hashKeys
        );
        // 将客户端消息插入到CuckooHash中
        int[] indicesArray = IntStream.range(0, retrievalSize).map(indices::get).toArray();
        cuckooHashBin.insertItems(indicesArray);
    }

    /**
     * 返回哈希分桶内元素数目。
     *
     * @return 分桶内元素数目。
     */
    private int generateHashBin(int binNum) {
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
     * 解码回复得到检索结果。
     *
     * @param response 回复。
     * @param binIndex 分桶索引。
     * @return 检索结果。
     */
    private byte[] handleServerResponsePayload(byte[] response, int binIndex) {
        long coeffs = Mr23BatchIndexPirNativeUtils.decryptReply(
            params.getEncryptionParams(),
            secretKey,
            response,
            offset[binIndex],
            innerParams.getSlotNum()
        );
        return coeffs == 1 ? new byte[]{0x01} : new byte[]{0x00};
    }
}