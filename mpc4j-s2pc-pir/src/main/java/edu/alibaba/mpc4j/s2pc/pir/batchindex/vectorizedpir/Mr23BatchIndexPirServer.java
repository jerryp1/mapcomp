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
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private List<byte[]> encodedDatabase = new ArrayList<>();
    /**
     * 哈希分桶
     */
    ArrayList<ArrayList<HashBinEntry<Integer>>> completeHashBins;

    public Mr23BatchIndexPirServer(Rpc serverRpc, Party clientParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(ArrayList<ByteBuffer> elementArrayList, int elementBitLength, int maxRetrievalSize)
        throws MpcAbortException {
        setInitInput(elementArrayList, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        params = Mr23BatchIndexPirParams.DEFAULT_PARAMS;
        // 服务端分桶
        stopWatch.start();
        hashKeys = CommonUtils.generateRandomKeys(params.getHashNum(), secureRandom);
        IntNoStashCuckooHashBin cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE, maxRetrievalSize, hashKeys
        );
        int binNum = cuckooHashBin.binNum();
        int max = maxBinSize(binNum);
        // 初始化batch pir参数
        innerParams = new Mr23BatchIndexPirInnerParams(params, binNum, max);
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
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> keyPairPayload = new ArrayList<>(rpc.receive(keyPairHeader).getPayload());
        handleKeyPairPayload(keyPairPayload);

        // 服务端对数据库进行编码
        stopWatch.start();
        // 每个分桶内的元素vectorized PIR初始化
        IntStream intStream = IntStream.range(0, binNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<long[][]> coeffs = intStream
            .mapToObj(i -> {
                try {
                    return vectorizedPirSetup(completeHashBins.get(i));
                } catch (MpcAbortException e) {
                    e.printStackTrace();
                    return new long[0][];
                }
            })
            .collect(Collectors.toList());
        // vectorized batch pir 初始化
        List<long[][]> mergedCoeffs = batchPirSetup(coeffs);
        encodedDatabase = mergedCoeffs.stream()
            .map(mergedCoeff ->
                Mr23BatchIndexPirNativeUtils.preprocessDatabase(
                    params.getEncryptionParams(), mergedCoeff, innerParams.getTotalSize()
                )
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
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

        // 服务端计算回复信息
        stopWatch.start();
        int count = CommonUtils.getUnitNum(innerParams.getBinNum(), innerParams.getGroupBinSize());
        IntStream intStream = IntStream.range(0, count);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> response = intStream
            .mapToObj(i -> handleClientQueryPayload(
                clientQueryPayload.subList(i * params.getDimension(), (i + 1) * params.getDimension()),
                encodedDatabase.subList(i * innerParams.getTotalSize(), (i + 1) * innerParams.getTotalSize()))
            )
            .collect(Collectors.toList());
        // merge response ciphertexts
        byte[] mergedResponse = Mr23BatchIndexPirNativeUtils.mergeResponse(params.getEncryptionParams(),
            publicKey, galoisKeys, response, innerParams.getGroupBinSize());
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, Collections.singletonList(mergedResponse)));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * 返回哈希分桶的最大桶内元素数目。
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
     * vectorized PIR初始化。
     *
     * @param binElements 分桶内的元素。
     * @return 分桶内的多项式编码
     * @throws MpcAbortException 如果协议异常终止。
     */
    private long[][] vectorizedPirSetup(ArrayList<HashBinEntry<Integer>> binElements) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(binElements.size() == innerParams.getBinSize());
        long[] elementArray = new long[innerParams.getBinSize()];
        IntStream.range(0, innerParams.getBinSize()).forEach(i -> {
            if (binElements.get(i).getHashIndex() == -1) {
                elementArray[i] = 0;
            } else {
                elementArray[i] = elementByteArray.get(binElements.get(i).getItem())[0];
            }
        });
        int dimLength = innerParams.getDimensionsLength();
        int size = CommonUtils.getUnitNum(innerParams.getBinSize(), dimLength);
        int length = dimLength;
        long[][] coeffs = new long[size][params.getPolyModulusDegree()];
        for (int i = 0; i < size; i++) {
            long[] temp = new long[params.getPolyModulusDegree()];
            if (i == (size - 1)) {
                length = innerParams.getBinSize() - dimLength * i;
            }
            for (int j = 0; j < length; j++) {
                temp[j * innerParams.getGroupBinSize()] = elementArray[i * dimLength + j];
            }
            coeffs[i] = BatchIndexPirUtils.plaintextRotate(temp, (i % dimLength) * innerParams.getGroupBinSize());
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
        int g = (params.getPolyModulusDegree() / 2) / innerParams.getSlotNum();
        int count = CommonUtils.getUnitNum(innerParams.getBinNum(), g);
        int rowCount = params.getPolyModulusDegree() / 2;
        List<long[][]> mergedDatabase = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long[][] vec = new long[binCoeffs.get(0).length][params.getPolyModulusDegree()];
            for (int j = 0; j < g; j++) {
                if ((i * g + j) >= innerParams.getBinNum()) {
                    break;
                } else {
                    long[][] temp = BatchIndexPirUtils.plaintextRotate(binCoeffs.get(i * g + j), j);
                    for (int l = 0; l < temp.length; l++) {
                        for (int k = 0; k < rowCount; k++) {
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
     * 服务端处理客户端查询信息。
     *
     * @param clientQuery      客户端查询信息。
     * @param encodedPlaintext 编码后的多项式。
     * @return 检索结果密文。
     */
    private byte[] handleClientQueryPayload(List<byte[]> clientQuery, List<byte[]> encodedPlaintext) {
        return Mr23BatchIndexPirNativeUtils.generateReply(
            params.getEncryptionParams(),
            clientQuery,
            encodedPlaintext,
            publicKey,
            relinKeys,
            galoisKeys,
            innerParams.getDimensionsLength(),
            innerParams.getSlotNum(),
            params.getDimension()
        );
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
