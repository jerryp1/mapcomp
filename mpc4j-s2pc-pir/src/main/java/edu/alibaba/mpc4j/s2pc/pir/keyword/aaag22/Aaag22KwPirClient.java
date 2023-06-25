package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22.Aaag22KwPirPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.createCuckooHashBin;

/**
 * AAAG22 keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
public class Aaag22KwPirClient<T> extends AbstractKwPirClient<T> {
    /**
     * CMG21 keyword PIR params
     */
    private Aaag22KwPirParams params;
    /**
     * no stash cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;
    /**
     * hash keys
     */
    private byte[] prfKey;

    private byte[] publicKey;

    private byte[] secretKey;

    private boolean isPadding;

    private byte[] relinKeys;

    private byte[] galoisKeys;

    public Aaag22KwPirClient(Rpc clientRpc, Party serverParty, Aaag22KwPirConfig config) {
        super(Aaag22KwPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(KwPirParams kwPirParams, int labelByteLength) throws MpcAbortException {
        setInitInput(kwPirParams.maxRetrievalSize(), labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        assert (kwPirParams instanceof Aaag22KwPirParams);
        params = (Aaag22KwPirParams) kwPirParams;

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        if (CommonUtils.getUnitNum(labelByteLength * Byte.SIZE, params.getPlainModulusSize()) % 2 == 1) {
            isPadding = true;
        }
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        // generate key pair
        List<byte[]> publicKeysPayload = generateKeyPair();
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxRetrievalSize, int labelByteLength) throws MpcAbortException {
        params = Aaag22KwPirParams.DEFAULT_PARAMS;
        setInitInput(params.maxRetrievalSize(), labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        if (CommonUtils.getUnitNum(labelByteLength * Byte.SIZE, params.getPlainModulusSize()) % 2 == 1) {
            isPadding = true;
        }
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        // generate key pair
        List<byte[]> publicKeysPayload = generateKeyPair();
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<T, ByteBuffer> pir(Set<T> retrievalSet) throws MpcAbortException {
        setPtoInput(retrievalSet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // run PRF
        stopWatch.start();
        List<ByteBuffer> keywordPrf = computePrf();
        Map<ByteBuffer, ByteBuffer> prfKeywordMap = IntStream.range(0, retrievalSize)
            .boxed()
            .collect(Collectors.toMap(keywordPrf::get, i -> retrievalKeywordList.get(i), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, oprfTime, "Client runs PRF");

        // generate query
        stopWatch.start();
        byte[] query = generateQuery(keywordPrf.get(0));
        DataPacketHeader queryDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryDataPacketHeader, Collections.singletonList(query)));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, genQueryTime, "Client generate query");

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        stopWatch.start();
        Map<T, ByteBuffer> pirResult = handleResponse(responsePayload.get(0), prfKeywordMap.get(keywordPrf.get(0)));
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, decodeResponseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * handle server response.
     *
     * @param response response.
     * @return retrieval result map.
     */
    private Map<T, ByteBuffer> handleResponse(byte[] response, ByteBuffer keyword) {
        int slotCount = params.getPolyModulusDegree() / 2;
        long[] coeffs = Aaag22KwPirNativeUtils.decodeReply(params.encryptionParams, secretKey, response);
        long[] items = new long[params.pirColumnNumPerObj];
        int index = IntStream.range(0, slotCount).filter(i -> coeffs[i] != 0).findFirst().orElse(-1);
        int bitLength = isPadding ? labelByteLength * Byte.SIZE + params.getPlainModulusSize() : labelByteLength * Byte.SIZE;
        if (index > 0) {
            for (int i = 0; i < params.pirColumnNumPerObj / 2; i++) {
                items[i] = coeffs[index + i];
                items[i + params.pirColumnNumPerObj / 2] = coeffs[index + i + slotCount];
            }
            byte[] bytes = PirUtils.convertCoeffsToBytes(items, params.getPlainModulusSize());
            HashMap<T, ByteBuffer> result = new HashMap<>();
            int start = isPadding ? params.getPlainModulusSize() / Byte.SIZE : 0;
            result.put(byteArrayObjectMap.get(keyword), ByteBuffer.wrap(BytesUtils.clone(bytes, start, labelByteLength)));
            return result;
        } else {
            return new HashMap<>(0);
        }
    }

    /**
     * generate query.
     *
     * @param keywordPrf keyword PRF.
     * @return client query.
     */
    private byte[] generateQuery(ByteBuffer keywordPrf) {
        long[] query = new long[params.getPolyModulusDegree()];
        long[] coeffs = PirUtils.convertBytesToCoeffs(
            params.getPlainModulusSize(), 0, params.keywordPrfByteLength, keywordPrf.array()
        );
        // todo 有可能是奇数
        assert coeffs.length == params.colNum * 2;
        int size = params.getPolyModulusDegree() / (params.colNum * 2);
        int slotCount = params.getPolyModulusDegree() / 2;
        for (int i = 0; i < params.colNum; i++) {
            for (int j = i * size; j < (i + 1) * size; j++) {
                query[j] = coeffs[2 * i];
                query[j + slotCount] = coeffs[2 * i + 1];
            }
        }
        return Aaag22KwPirNativeUtils.generateQuery(params.encryptionParams, publicKey, secretKey, query);
    }

    /**
     * generate prf element.
     *
     * @return prf element.
     */
    private List<ByteBuffer> computePrf() {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prf.setKey(prfKey);
        Stream<ByteBuffer> keywordStream = retrievalKeywordList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(byteBuffer -> prf.getBytes(byteBuffer.array()))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Aaag22KwPirNativeUtils.keyGen(params.encryptionParams, params.pirColumnNumPerObj, params.colNum);
        assert (keyPair.size() == 4);
        this.secretKey = keyPair.get(0);
        this.publicKey = keyPair.get(1);
        this.relinKeys = keyPair.get(2);
        this.galoisKeys = keyPair.get(3);
        return keyPair.subList(1, 4);
    }
}