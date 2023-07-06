package edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ALPR21 keyword PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
public class Alpr21KwPirServer extends AbstractKwPirServer {
    /**
     * ALPR21 keyword PIR params
     */
    private Alpr21KwPirParams params;
    /**
     * index PIR server
     */
    private final BatchIndexPirServer indexPirServer;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * prf key
     */
    private byte[] prfKey;
    /**
     * hash keys
     */
    private byte[][] hashKeys;

    public Alpr21KwPirServer(Rpc serverRpc, Party clientParty, Alpr21KwPirConfig config) {
        super(Alpr21KwPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        indexPirServer = BatchIndexPirFactory.createServer(serverRpc, clientParty, config.getBatchIndexPirConfig());
        addSubPtos(indexPirServer);
        cuckooHashBinType = config.getCuckooHashBinType();
    }

    @Override
    public void init(KwPirParams kwPirParams, Map<ByteBuffer, ByteBuffer> serverKeywordLabelMap, int labelByteLength)
        throws MpcAbortException {
        setInitInput(serverKeywordLabelMap, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        assert (kwPirParams instanceof Alpr21KwPirParams);
        params = (Alpr21KwPirParams) kwPirParams;

        stopWatch.start();
        byte[] botElementByteArray = new byte[params.keywordPrfByteLength];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        List<ByteBuffer> keywordPrf = computeKeywordPrf();
        Map<ByteBuffer, ByteBuffer> prfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(
                Collectors.toMap(keywordPrf::get, i -> serverKeywordLabelMap.get(keywordList.get(i)), (a, b) -> b)
            );
        NaiveDatabase database = generateCuckooHashBin(keywordPrf, prfLabelMap);
        indexPirServer.init(database, hashKeys.length);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfKeyHeader, Collections.singletonList(prfKey)));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(Map<ByteBuffer, ByteBuffer> serverKeywordLabelMap, int maxRetrievalSize, int labelByteLength)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(maxRetrievalSize == 1);
        setInitInput(serverKeywordLabelMap, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        params = Alpr21KwPirParams.DEFAULT_PARAMS;

        stopWatch.start();
        byte[] botElementByteArray = new byte[params.keywordPrfByteLength];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        List<ByteBuffer> keywordPrf = computeKeywordPrf();
        Map<ByteBuffer, ByteBuffer> prfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(
                Collectors.toMap(keywordPrf::get, i -> serverKeywordLabelMap.get(keywordList.get(i)), (a, b) -> b)
            );
        NaiveDatabase database = generateCuckooHashBin(keywordPrf, prfLabelMap);
        indexPirServer.init(database, hashKeys.length);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfKeyHeader, Collections.singletonList(prfKey)));
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

        stopWatch.start();
        indexPirServer.pir();
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * compute keyword prf.
     *
     * @return keyword prf.
     */
    private List<ByteBuffer> computeKeywordPrf() {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prfKey);
        prf.setKey(prfKey);
        Stream<ByteBuffer> keywordStream = keywordList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(byteBuffer -> prf.getBytes(byteBuffer.array()))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    /**
     * generate cuckoo hash bin.
     *
     * @param keywordPrf  keyword prf list.
     * @param prfLabelMap keyword prf label map.
     * @return database.
     */
    private NaiveDatabase generateCuckooHashBin(List<ByteBuffer> keywordPrf, Map<ByteBuffer, ByteBuffer> prfLabelMap) {
        CuckooHashBin<ByteBuffer> cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, keywordSize, keywordPrf, secureRandom
        );
        hashKeys = cuckooHashBin.getHashKeys();
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        byte[][] cuckooHashBinItems = new byte[cuckooHashBin.binNum()][];
        int truncationByteLength = 6;
        for (int i = 0; i < cuckooHashBin.binNum(); i++) {
            ByteBuffer item = cuckooHashBin.getHashBinEntry(i).getItem();
            byte[] value = new byte[labelByteLength];
            if (prfLabelMap.get(item) != null) {
                value = prfLabelMap.get(item).array();
            } else {
                secureRandom.nextBytes(value);
            }
            cuckooHashBinItems[i] = Bytes.concat(BytesUtils.clone(item.array(), 0, truncationByteLength), value);
        }
        return NaiveDatabase.create((truncationByteLength + labelByteLength) * Byte.SIZE, cuckooHashBinItems);
    }
}