package edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;

/**
 * ALPR21 keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
public class Alpr21KwPirClient extends AbstractKwPirClient {
    /**
     * ALPR21 keyword PIR params
     */
    private Alpr21KwPirParams params;
    /**
     * index PIR client
     */
    private final BatchIndexPirClient indexPirClient;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * prf key
     */
    private byte[] prfKey;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * truncation byte length
     */
    private final int truncationByteLength = 6;

    public Alpr21KwPirClient(Rpc clientRpc, Party serverParty, Alpr21KwPirConfig config) {
        super(Alpr21KwPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        indexPirClient = BatchIndexPirFactory.createClient(clientRpc, serverParty, config.getBatchIndexPirConfig());
        addSubPtos(indexPirClient);
        cuckooHashBinType = config.getCuckooHashBinType();
    }

    @Override
    public void init(KwPirParams kwPirParams, int serverElementSize, int labelByteLength) throws MpcAbortException {
        assert (kwPirParams instanceof Alpr21KwPirParams);
        params = (Alpr21KwPirParams) kwPirParams;

        setInitInput(params.maxRetrievalSize(), serverElementSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfKeyHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == getHashNum(cuckooHashBinType));
        hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        int binNum = getBinNum(cuckooHashBinType, serverElementSize);
        int elementBitLength = (truncationByteLength + labelByteLength) * Byte.SIZE;
        indexPirClient.init(binNum, elementBitLength, hashKeys.length);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxRetrievalSize, int serverElementSize, int labelByteLength) throws MpcAbortException {
        params = Alpr21KwPirParams.DEFAULT_PARAMS;
        setInitInput(params.maxRetrievalSize(), serverElementSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfKeyHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == getHashNum(cuckooHashBinType));
        hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        int binNum = getBinNum(cuckooHashBinType, serverElementSize);
        int elementBitLength = (truncationByteLength + labelByteLength) * Byte.SIZE;
        indexPirClient.init(binNum, elementBitLength, hashKeys.length);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<ByteBuffer, ByteBuffer> pir(Set<ByteBuffer> retrievalKeySet) throws MpcAbortException {
        setPtoInput(retrievalKeySet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        ByteBuffer prfOutput = computePrf();
        List<Integer> indexList = computeIndex(prfOutput);
        Map<Integer, byte[]> retrievalMap = indexPirClient.pir(indexList);
        Map<ByteBuffer, ByteBuffer> pirResult = handleResponse(retrievalMap, prfOutput);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, oprfTime, "Client runs PIR");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * handle server response.
     *
     * @param retrievalMap retrieval map.
     * @param keyPrf       key prf.
     * @return retrieval result map.
     */
    private Map<ByteBuffer, ByteBuffer> handleResponse(Map<Integer, byte[]> retrievalMap, ByteBuffer keyPrf) {
        Map<ByteBuffer, ByteBuffer> result = new HashMap<>(1);
        retrievalMap.forEach((index, item) -> {
            try {
                MpcAbortPreconditions.checkArgument(item.length == truncationByteLength + valueByteLength);
            } catch (MpcAbortException e) {
                e.printStackTrace();
            }
            byte[] retrievalKeyBytes = BytesUtils.clone(item, 0, truncationByteLength);
            byte[] localKeyBytes = BytesUtils.clone(keyPrf.array(), 0, truncationByteLength);
            if (ByteBuffer.wrap(retrievalKeyBytes).equals(ByteBuffer.wrap(localKeyBytes))) {
                result.put(
                    retrievalKeyList.get(0),
                    ByteBuffer.wrap(BytesUtils.clone(item, truncationByteLength, valueByteLength))
                );
            }
        });
        return result;
    }

    /**
     * generate prf element.
     *
     * @return prf element.
     */
    private ByteBuffer computePrf() {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prf.setKey(prfKey);
        return ByteBuffer.wrap(prf.getBytes(retrievalKeyList.get(0).array()));
    }

    /**
     * compute index.
     *
     * @param prfOutput retrieval prf.
     * @return retrieval index list.
     */
    private List<Integer> computeIndex(ByteBuffer prfOutput) {
        int binNum = getBinNum(cuckooHashBinType, serverElementSize);
        Prf[] hashes = Arrays.stream(hashKeys)
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        List<Integer> indexList = new ArrayList<>();
        for (int hashIndex = 0; hashIndex < hashKeys.length; hashIndex++) {
            HashBinEntry<ByteBuffer> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, prfOutput);
            indexList.add(hashes[hashIndex].getInteger(hashBinEntry.getItemByteArray(), binNum));
        }
        return indexList;
    }
}