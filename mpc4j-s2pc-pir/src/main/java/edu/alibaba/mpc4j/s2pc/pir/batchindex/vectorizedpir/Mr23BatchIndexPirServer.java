package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirServer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirPtoDesc.*;

/**
 * VECTORIZED_BATCH_PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirServer extends AbstractBatchIndexPirServer {

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
     * 哈希算法密钥
     */
    private byte[][] hashKeys;
    /**
     * 公钥
     */
    private byte[] publicKey;
    /**
     * Galois密钥
     */
    private byte[] galoisKeys;
    /**
     * Relin Keys密钥
     */
    private byte[] relinKeys;
    /**
     * BFV明文（点值表示）
     */
    private List<List<byte[]>> encodedDatabase;
    /**
     * 旋转的明文
     */
    private List<byte[]> rotatePlain;
    /**
     * 哈希分桶
     */
    ArrayList<ArrayList<HashBinEntry<Integer>>> completeHashBins;

    public Mr23BatchIndexPirServer(Rpc serverRpc, Party clientParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(byte[][] elementArray, int elementBitLength, int maxRetrievalSize)
        throws MpcAbortException {
        if (maxRetrievalSize > 2048) {
            MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
        }
        if (elementArray.length <= (1 << 20)) {
            if (maxRetrievalSize <= 256) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_256;
            } else if (maxRetrievalSize <= 512) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_512;
            } else if (maxRetrievalSize <= 1024) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_1024;
            } else {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_20_RETRIEVAL_SIZE_2048;
            }
        } else if (elementArray.length <= (1 << 22)) {
            if (maxRetrievalSize <= 256) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_256;
            } else if (maxRetrievalSize <= 512) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_512;
            } else if (maxRetrievalSize <= 1024) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_1024;
            } else {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_22_RETRIEVAL_SIZE_2048;
            }
        } else if (elementArray.length <= (1 << 24)) {
            if (maxRetrievalSize <= 256) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_256;
            } else if (maxRetrievalSize <= 512) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_512;
            } else if (maxRetrievalSize <= 1024) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_24_RETRIEVAL_SIZE_2048;
            }
        } else if (elementArray.length <= (1 << 26)) {
            if (maxRetrievalSize <= 256) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_26_RETRIEVAL_SIZE_256;
            } else if (maxRetrievalSize <= 512) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_26_RETRIEVAL_SIZE_512;
            } else if (maxRetrievalSize <= 1024) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_26_RETRIEVAL_SIZE_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Mr23BatchIndexPirParams.ELEMENT_LOG_SIZE_26_RETRIEVAL_SIZE_2048;
            }
        }

        setInitInput(elementArray, elementBitLength, maxRetrievalSize, params.getPlainModulusBitLength() - 1);
        logPhaseInfo(PtoState.INIT_BEGIN);
        // 服务端分桶
        stopWatch.start();
        hashKeys = CommonUtils.generateRandomKeys(params.getHashNum(), secureRandom);
        IntNoStashCuckooHashBin cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE, maxRetrievalSize, hashKeys
        );
        int maxBinSize = getMaxBinSize(cuckooHashBin.binNum());
        // 初始化参数
        innerParams = new Mr23BatchIndexPirInnerParams(params, cuckooHashBin.binNum(), maxBinSize);
        System.out.println(innerParams);
        assert (params.getPolyModulusDegree() / 2) >= cuckooHashBin.binNum();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);
        // 服务端接收客户端生成的密钥对
        DataPacketHeader keyPairHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> keyPairPayload = new ArrayList<>(rpc.receive(keyPairHeader).getPayload());
        handleKeyPairPayload(keyPairPayload);
        // 服务端对数据库进行编码
        stopWatch.start();
        encodedDatabase = new ArrayList<>();
        int count = CommonUtils.getUnitNum(innerParams.getBinNum() / 2, innerParams.getGroupBinSize());
        rotatePlain = Mr23BatchIndexPirNativeUtils.preprocessRotatePlain(
            params.getEncryptionParams(), innerParams.getGroupBinSize(), count
        );
        for (int partitionIndex = 0; partitionIndex < partitionCount; partitionIndex++) {
            // 每个分桶内的元素vectorized PIR初始化
            IntStream intStream = IntStream.range(0, cuckooHashBin.binNum());
            intStream = parallel ? intStream.parallel() : intStream;
            int finalPartitionIndex = partitionIndex;
            List<long[][]> coeffs = intStream
                .mapToObj(i -> vectorizedPirSetup(i, completeHashBins.get(i), finalPartitionIndex))
                .collect(Collectors.toList());
            // vectorized batch pir 初始化
            List<long[][]> mergedCoeffs = batchPirSetup(coeffs);
            IntStream stream = IntStream.range(0, mergedCoeffs.size());
            stream = parallel ? stream.parallel() : stream;
            encodedDatabase.addAll(stream
                .mapToObj(i -> Mr23BatchIndexPirNativeUtils.preprocessDatabase(
                    params.getEncryptionParams(), mergedCoeffs.get(i), params.getFirstTwoDimensionSize()))
                .collect(Collectors.toList()));
        }
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initTime);
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 服务端接收客户端查询信息
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();
        int count = CommonUtils.getUnitNum(innerParams.getBinNum() / 2, innerParams.getGroupBinSize());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == count * params.getDimension());

        // 服务端计算回复信息
        stopWatch.start();
        List<byte[]> serverResponsePayload = new ArrayList<>();
        for (int partitionIndex = 0; partitionIndex < partitionCount; partitionIndex++) {
            IntStream intStream = IntStream.range(0, count);
            intStream = parallel ? intStream.parallel() : intStream;
            int finalPartitionIndex = partitionIndex * count;
            List<byte[]> response = intStream
                .mapToObj(i ->
                    Mr23BatchIndexPirNativeUtils.generateReply(
                        params.getEncryptionParams(),
                        clientQueryPayload.subList(i * params.getDimension(), (i + 1) * params.getDimension()),
                        encodedDatabase.get(finalPartitionIndex + i),
                        publicKey,
                        relinKeys,
                        galoisKeys,
                        params.getFirstTwoDimensionSize()
                    ))
                .collect(Collectors.toList());
            // merge response ciphertexts
            serverResponsePayload.add(Mr23BatchIndexPirNativeUtils.mergeResponse(
                params.getEncryptionParams(), publicKey, galoisKeys, response, innerParams.getGroupBinSize(), rotatePlain
                )
            );
        }
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * 返回哈希分桶后最大桶内元素数目。
     *
     * @param binNum 分桶数目。
     * @return 最大桶内元素数目。
     */
    private int getMaxBinSize(int binNum) {
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
     * vectorized PIR初始化。
     *
     * @param binIndex       分桶索引值。
     * @param binItems       分桶内的元素。
     * @param partitionIndex 分块索引值。
     * @return 分桶内的多项式编码
     */
    private long[][] vectorizedPirSetup(int binIndex, ArrayList<HashBinEntry<Integer>> binItems, int partitionIndex) {
        long[] items = new long[innerParams.getBinSize()];
        IntStream.range(0, innerParams.getBinSize()).forEach(i -> {
            if (binItems.get(i).getHashIndex() == -1) {
                items[i] = 0L;
            } else {
                items[i] = LongUtils.byteArrayToLong(elementByteArray.get(partitionIndex)[binItems.get(i).getItem()]);
            }
        });
        int dimLength = params.getFirstTwoDimensionSize();
        int size = CommonUtils.getUnitNum(innerParams.getBinSize(), dimLength);
        int length = dimLength;
        int offset = (binIndex < (innerParams.getBinNum() / 2)) ? 0 : (params.getPolyModulusDegree() / 2);
        long[][] coeffs = new long[size][params.getPolyModulusDegree()];
        for (int i = 0; i < size; i++) {
            long[] temp = new long[params.getPolyModulusDegree()];
            if (i == (size - 1)) {
                length = innerParams.getBinSize() - dimLength * i;
            }
            for (int j = 0; j < length; j++) {
                temp[j * innerParams.getGroupBinSize() + offset] = items[i * dimLength + j];
            }
            coeffs[i] = PirUtils.plaintextRotate(temp, (i % dimLength) * innerParams.getGroupBinSize());
        }
        return coeffs;
    }

    /**
     * vectorized batch pir 初始化。
     *
     * @param binCoeffs 每个分桶对应的多项式系数。
     * @return 合并后的多项式系数。
     */
    private List<long[][]> batchPirSetup(List<long[][]> binCoeffs) {
        int count = CommonUtils.getUnitNum(innerParams.getBinNum() / 2, innerParams.getGroupBinSize());
        List<long[][]> mergedDatabase = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long[][] vec = new long[binCoeffs.get(0).length][params.getPolyModulusDegree()];
            for (int j = 0; j < innerParams.getGroupBinSize(); j++) {
                if ((i * innerParams.getGroupBinSize() + j) >= (innerParams.getBinNum() / 2)) {
                    break;
                } else {
                    long[][] temp = PirUtils.plaintextRotate(binCoeffs.get(i * innerParams.getGroupBinSize() + j), j);
                    for (int l = 0; l < temp.length; l++) {
                        for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                            vec[l][k] += temp[l][k];
                        }
                    }
                    temp = PirUtils.plaintextRotate(
                        binCoeffs.get(i * innerParams.getGroupBinSize() + j + innerParams.getBinNum() / 2), j
                    );
                    for (int l = 0; l < temp.length; l++) {
                        for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                            vec[l][k] += temp[l][k];
                        }
                    }
                }
            }
            mergedDatabase.add(vec);
        }
        return mergedDatabase;
    }

    /**
     * 服务端处理客户端密钥对。
     *
     * @param keyPair 密钥对。
     * @exception MpcAbortException 如果协议异常中止。
     */
    private void handleKeyPairPayload(List<byte[]> keyPair) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(keyPair.size() == 3);
        publicKey = keyPair.remove(0);
        relinKeys = keyPair.remove(0);
        galoisKeys = keyPair.remove(0);
    }
}
