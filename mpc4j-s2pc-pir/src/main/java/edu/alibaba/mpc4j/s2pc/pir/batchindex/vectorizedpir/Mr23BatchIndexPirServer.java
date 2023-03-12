package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirServer;

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
     * Vectorized PIR方案参数
     */
    private Mr23BatchIndexPirParams params;
    /**
     * Vectorized PIR方案内部参数
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


    ArrayList<ArrayList<HashBinEntry<Integer>>> completeHashBins;

    public Mr23BatchIndexPirServer(Rpc serverRpc, Party clientParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(ArrayList<ByteBuffer> elementArrayList, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(elementArrayList, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        params = Mr23BatchIndexPirParams.DEFAULT_PARAMS;
        // 服务端分桶
        stopWatch.start();
        hashKeys = CommonUtils.generateRandomKeys(params.getHashNum(), secureRandom);
        int binNum = (int) Math.ceil(1.5 * maxRetrievalSize);
        int binSize = generateHashBin(binNum);
        // 初始化batch pir参数
        innerParams = new Mr23BatchIndexPirInnerParams(params, binNum, binSize);
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
        int totalSize = IntStream.range(0, params.getDimension() - 1)
            .map(j -> innerParams.getDimensionsLength())
            .reduce(1, (a, b) -> a * b);
        IntStream intStream = IntStream.range(0, binNum);
        intStream = parallel ? intStream.parallel() : intStream;
        encodedDatabase = intStream
            .mapToObj(i -> preprocessDatabase(completeHashBins.get(i), totalSize))
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
        assert clientQueryPayload.size() == innerParams.getBinNum() * params.getDimension();

        // 服务端计算回复信息
        stopWatch.start();
        int totalSize = IntStream.range(0, params.getDimension() - 1)
            .map(j -> innerParams.getDimensionsLength())
            .reduce(1, (a, b) -> a * b);
        IntStream intStream = IntStream.range(0, innerParams.getBinNum());
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> response = intStream
            .mapToObj(i -> handleClientQueryPayload(
                clientQueryPayload.subList(i * params.getDimension(), (i + 1) * params.getDimension()),
                encodedDatabase.subList(i * totalSize, (i + 1) * totalSize))
            )
            .collect(Collectors.toList());
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, response));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
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
     * 返回数据库编码后的多项式。
     *
     * @param totalSize 多项式数量。
     * @return 数据库编码后的多项式。
     */
    private ArrayList<byte[]> preprocessDatabase(ArrayList<HashBinEntry<Integer>> elements, int totalSize) {
        long[] coeffs = new long[elements.size()];
        IntStream.range(0, elements.size()).forEach(i -> {
            if (elements.get(i).getHashIndex() == -1) {
                coeffs[i] = 1L << params.getPlainModulusBitLength();
            } else {
                if (elementByteArray.get(elements.get(i).getItem())[0] == (byte) 0x01) {
                    coeffs[i] = 1;
                } else {
                    coeffs[i] = 0;
                }
            }
        });
        return Mr23BatchIndexPirNativeUtils.preprocessDatabase(
            params.getEncryptionParams(), coeffs, innerParams.getDimensionsLength(), innerParams.getSlotNum(), totalSize
        );
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
