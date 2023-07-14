package edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.crypto.matrix.zp64.Zl64Matrix;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirClient;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir.CuckooHashBatchSimplePirPtoDesc.PtoStep;

/**
 * Batch Simple PIR based on Cuckoo Hash client.
 *
 * @author Liqiang Peng
 * @date 2023/7/7
 */
public class CuckooHashBatchSimplePirClient extends AbstractBatchIndexPirClient {

    /**
     * cuckoo hash bin type
     */
    private final IntCuckooHashBinType cuckooHashBinType;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * bin index and database index map
     */
    private Map<Integer, Integer> binIndexRetrievalIndexMap;
    /**
     * simple hash bin
     */
    private int[][] hashBin;
    /**
     * simple PIR client
     */
    private final Hhcm23SimpleSingleIndexPirClient simplePirClient;
    /**
     * bin num
     */
    private int binNum;
    /**
     * hint
     */
    private Zl64Matrix[][] hint;

    public CuckooHashBatchSimplePirClient(Rpc clientRpc, Party serverParty, CuckooHashBatchSimplePirConfig config) {
        super(CuckooHashBatchSimplePirPtoDesc.getInstance(), clientRpc, serverParty, config);
        simplePirClient = new Hhcm23SimpleSingleIndexPirClient(clientRpc, serverParty, config.getSimplePirConfig());
        cuckooHashBinType = config.getCuckooHashBinType();
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        setInitInput(serverElementSize, elementBitLength, maxRetrievalSize);

        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        // client generate simple hash bin
        binNum = IntCuckooHashBinFactory.getBinNum(cuckooHashBinType, maxRetrievalSize);
        int hashNum = IntCuckooHashBinFactory.getHashNum(cuckooHashBinType);
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == hashNum);
        hashKeys = hashKeyPayload.toArray(new byte[0][]);
        int maxBinSize = generateSimpleHashBin();
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        // receive seed and hint
        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> seedPayload = rpc.receive(seedPayloadHeader).getPayload();
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hintPayload = rpc.receive(hintPayloadHeader).getPayload();

        stopWatch.start();
        // client init single index PIR client
        simplePirClient.setParallel(parallel);
        simplePirClient.setDefaultParams();
        simplePirClient.clientBatchSetup(serverElementSize, maxBinSize, binNum, elementBitLength);
        simplePirClient.handledSeedPayload(seedPayload);
        partitionSize = simplePirClient.partitionSize;
        MpcAbortPreconditions.checkArgument(hintPayload.size() == binNum * partitionSize);
        hint = new Zl64Matrix[binNum][partitionSize];
        for (int i = 0; i < binNum; i++) {
            hint[i] = simplePirClient.handledHintPayload(hintPayload.subList(i*partitionSize, (i+1)*partitionSize));
        }
        stopWatch.stop();
        long initIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initIndexPirTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, byte[]> pir(List<Integer> indexList) throws MpcAbortException {
        setPtoInput(indexList);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // generate bin index list
        TIntList binIndexList = updateBinIndex();
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cuckooHashKeyTime, "Client generates cuckoo hash bin");

        stopWatch.start();
        IntStream intStream = IntStream.range(0, binNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> clientQueryPayload = intStream
            .mapToObj(i -> {
                if (binIndexList.get(i) == -1) {
                    return simplePirClient.generateQuery(0);
                } else {
                    return simplePirClient.generateQuery(binIndexList.get(i));
                }})
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

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(responsePayload.size() == binNum * partitionSize);

        stopWatch.start();
        IntStream decodeStream = IntStream.range(0, binNum);
        decodeStream = parallel ? decodeStream.parallel() : decodeStream;
        List<byte[]> result = decodeStream
            .mapToObj(i -> {
                if (binIndexList.get(i) != -1) {
                    try {
                        return simplePirClient.decodeResponse(
                            responsePayload.subList(i * partitionSize, (i + 1) * partitionSize),
                            binIndexList.get(i),
                            hint[i],
                            elementBitLength
                        );
                    } catch (MpcAbortException e) {
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    return new byte[0];
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, decodeResponseTime, "Client decodes response");

        logPhaseInfo(PtoState.PTO_END);
        return IntStream.range(0, binIndexList.size())
            .filter(i -> binIndexList.get(i) != -1)
            .boxed()
            .collect(Collectors.toMap(
                integer -> binIndexRetrievalIndexMap.get(integer),
                result::get,
                (a, b) -> b,
                () -> new HashMap<>(retrievalSize))
            );
    }

    /**
     * update retrieval index list.
     *
     * @return bin index list.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private TIntList updateBinIndex() throws MpcAbortException {
        IntNoStashCuckooHashBin cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, cuckooHashBinType, maxRetrievalSize, binNum, hashKeys
        );
        int[] indicesArray = IntStream.range(0, retrievalSize).map(indexList::get).toArray();
        cuckooHashBin.insertItems(indicesArray);
        TIntList binIndex = new TIntArrayList();
        binIndexRetrievalIndexMap = new HashMap<>(retrievalSize);
        for (int i = 0; i < binNum; i++) {
            if (cuckooHashBin.getBinHashIndex(i) == -1) {
                binIndex.add(-1);
            } else {
                for (int j = 0; j < hashBin[i].length; j++) {
                    if (hashBin[i][j] == cuckooHashBin.getBinEntry(i)) {
                        binIndex.add(j);
                        binIndexRetrievalIndexMap.put(i, cuckooHashBin.getBinEntry(i));
                        break;
                    }
                }
            }
        }
        MpcAbortPreconditions.checkArgument(
            binIndex.size() == binNum && binIndexRetrievalIndexMap.size() == retrievalSize
        );
        return binIndex;
    }

    /**
     * generate simple hash bin.
     *
     * @return max bin size.
     */
    private int generateSimpleHashBin() {
        int[] totalIndex = IntStream.range(0, serverElementSize).toArray();
        IntHashBin intHashBin = new SimpleIntHashBin(envType, binNum, serverElementSize, hashKeys);
        intHashBin.insertItems(totalIndex);
        int maxBinSize = intHashBin.binSize(0);
        for (int i = 1; i < binNum; i++) {
            if (intHashBin.binSize(i) > maxBinSize) {
                maxBinSize = intHashBin.binSize(i);
            }
        }
        hashBin = new int[binNum][];
        for (int i = 0; i < binNum; i++) {
            hashBin[i] = new int[intHashBin.binSize(i)];
            for (int j = 0; j < intHashBin.binSize(i); j++) {
                hashBin[i][j] = intHashBin.getBin(i)[j];
            }
        }
        intHashBin.clear();
        return maxBinSize;
    }
}