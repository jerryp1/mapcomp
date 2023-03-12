package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirUtils;



import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirPtoDesc.*;
import static java.util.stream.Collectors.toCollection;

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
     * SEAL上下文参数
     */
    private byte[] sealContext;
    /**
     * 多项式系数
     */
    private ArrayList<long[][]> coeffs = new ArrayList<>();
    /**
     * 序列化的多项式
     */
    private ArrayList<ArrayList<byte[]>> encodedPlaintexts = new ArrayList<>();
    /**
     * 哈希桶索引对应哈希桶的元素数量
     */
    private int maxBinSize;

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
    private ArrayList<ArrayList<byte[]>> encodedDatabase = new ArrayList<>();


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
        hashKeys = CommonUtils.generateRandomKeys(params.getHashNum(), secureRandom);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        completeHashBins = computeBinIndex();

        DataPacketHeader keyPairHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> keyPairPayload = new ArrayList<>(rpc.receive(keyPairHeader).getPayload());
        MpcAbortPreconditions.checkArgument(keyPairPayload.size() == 3);
        publicKey = keyPairPayload.remove(0);
        relinKeys = keyPairPayload.remove(0);
        galoisKeys = keyPairPayload.remove(0);



        // 服务端对数据库进行编码
        stopWatch.start();
        innerParams = new Mr23BatchIndexPirInnerParams(params, completeHashBins.get(0).size());
        int dimensionsLength = innerParams.getDimensionsLength();
        int totalSize = IntStream.range(0, params.getDimension() - 1)
            .map(j -> dimensionsLength)
            .reduce(1, (a, b) -> a * b);
        for (int i = 0; i < completeHashBins.size(); i++) {
            encodedDatabase.add(preprocessDatabase(completeHashBins.get(i), totalSize));
        }
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);


        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();
        assert clientQueryPayload.size() == encodedDatabase.size() * params.getDimension();

        List<byte[]> response = new ArrayList<>();
        for (int i = 0; i < encodedDatabase.size(); i++) {
            response.add(handleClientQueryPayload(
                clientQueryPayload.subList(i * params.getDimension(), (i + 1) * params.getDimension()), i
            ));
        }

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, response));


        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * 返回哈希桶索引对应哈希桶的元素数量。
     *
     * @return 哈希桶索引对应哈希桶的元素数量。
     */
    private ArrayList<ArrayList<HashBinEntry<Integer>>> computeBinIndex() {
        ArrayList<Integer> totalIndexList = IntStream.range(0, serverElementSize)
            .boxed()
            .collect(Collectors.toCollection(() -> new ArrayList<>(serverElementSize)));
        int binNum = IntCuckooHashBinFactory.getBinNum(
            IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE, maxRetrievalSize
        );
        RandomPadHashBin<Integer> completeHash = new RandomPadHashBin<>(envType, binNum, serverElementSize, hashKeys);
        completeHash.insertItems(totalIndexList);
        int maxBinSize = completeHash.binSize(0);
        for (int i = 1; i < completeHash.binNum(); i++) {
            if (completeHash.binSize(i) > maxBinSize) {
                maxBinSize = completeHash.binSize(i);
            }
        }
        ArrayList<ArrayList<HashBinEntry<Integer>>> completeHashBins = new ArrayList<>();
        HashBinEntry<Integer> paddingEntry = HashBinEntry.fromEmptyItem(-1);
        for (int i = 0; i < completeHash.binNum(); i++) {
            ArrayList<HashBinEntry<Integer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        return completeHashBins;
    }

    /**
     * 返回数据库编码后的多项式。
     *
     * @param totalSize 多项式数量。
     * @return 数据库编码后的多项式。
     */
    private ArrayList<byte[]> preprocessDatabase(ArrayList<HashBinEntry<Integer>> elements, int totalSize) {
        long[] coeffs = new long[elements.size()];
        int byteLength = elementByteArray.get(0).length;
        IntStream.range(0, elements.size()).forEach(i -> {
            if (elements.get(i).getHashIndex() == -1) {
                coeffs[i] = 1L << params.getPlainModulusBitLength();
            } else {
//                long[] temp = IndexPirUtils.convertBytesToCoeffs(
//                    params.getPlainModulusBitLength(), 0, byteLength, elementByteArray.get(elements.get(i).getItem())
//                );
//                assert temp.length == 1;
//                coeffs[i] = temp[0];
                if (elementByteArray.get(elements.get(i).getItem())[0] == 0x01) {
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
     * @param clientQueryPayload 客户端查询信息。
     * @return 检索结果密文。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] handleClientQueryPayload(List<byte[]> clientQueryPayload, int index) throws MpcAbortException {
        return Mr23BatchIndexPirNativeUtils.generateReply(
            params.getEncryptionParams(), clientQueryPayload, encodedDatabase.get(index),publicKey, relinKeys, galoisKeys, innerParams.getDimensionsLength(), innerParams.getSlotNum(), params.getDimension());
    }
}
