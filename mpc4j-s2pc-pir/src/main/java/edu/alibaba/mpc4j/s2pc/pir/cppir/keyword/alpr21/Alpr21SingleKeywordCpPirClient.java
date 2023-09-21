package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleKeywordCpPirClient;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getBinNum;
import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getHashNum;
import static edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleKeywordCpPirDesc.*;

/**
 * ALPR21 client-specific preprocessing PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class Alpr21SingleKeywordCpPirClient extends AbstractSingleKeywordCpPirClient {
    /**
     * single index client-specific preprocessing PIR client
     */
    private final SingleIndexCpPirClient singleIndexCpPirClient;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * digest byte length
     */
    private final int digestByteLength;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * hash
     */
    private final Hash hash;

    public Alpr21SingleKeywordCpPirClient(Rpc clientRpc, Party serverParty, Alpr21SingleKeywordCpPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        singleIndexCpPirClient = SingleIndexCpPirFactory.createClient(clientRpc, serverParty, config.getIndexCpPirConfig());
        addSubPtos(singleIndexCpPirClient);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = getHashNum(cuckooHashBinType);
        digestByteLength = config.getDigestByteLength();
        hash = HashFactory.createInstance(envType, digestByteLength);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);
        MathPreconditions.checkGreaterOrEqual("statistical security",
            digestByteLength * Byte.SIZE, PirUtils.getBitLength(n) + CommonConstants.STATS_BIT_LENGTH);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == hashNum);

        stopWatch.start();
        hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);
        singleIndexCpPirClient.init(getBinNum(cuckooHashBinType, n), (digestByteLength + byteL) * Byte.SIZE);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ByteBuffer pir(ByteBuffer item) throws MpcAbortException {
        setPtoInput(item);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        ByteBuffer digest = ByteBuffer.wrap(hash.digestToBytes(item.array()));
        List<Integer> indexList = computeIndex(digest);
        byte[][] retrievalItem = new byte[hashNum][byteL];
        for (int i = 0; i < hashNum; i++) {
            retrievalItem[i] = singleIndexCpPirClient.pir(indexList.get(i));
        }
        ByteBuffer pirResult = handleResponse(retrievalItem, digest.array());
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, oprfTime, "Client runs PIR");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * compute index.
     *
     * @param item item.
     * @return retrieval index list.
     */
    private List<Integer> computeIndex(ByteBuffer item) {
        int binNum = getBinNum(cuckooHashBinType, n);
        Prf[] hashes = Arrays.stream(hashKeys)
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        List<Integer> indexList = new ArrayList<>();
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            HashBinEntry<ByteBuffer> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, item);
            indexList.add(hashes[hashIndex].getInteger(hashBinEntry.getItemByteArray(), binNum));
        }
        return indexList;
    }

    /**
     * handle server response.
     *
     * @param retrievalItems retrieval items.
     * @param itemDigest     item digest.
     * @return retrieval result.
     */
    private ByteBuffer handleResponse(byte[][] retrievalItems, byte[] itemDigest) {
        ByteBuffer itemDigestByteBuffer = ByteBuffer.wrap(itemDigest);
        for (int i = 0; i < hashNum; i++) {
            byte[] retrievalDigest = BytesUtils.clone(retrievalItems[i], 0, digestByteLength);
            if (ByteBuffer.wrap(retrievalDigest).equals(itemDigestByteBuffer)) {
                return ByteBuffer.wrap(BytesUtils.clone(retrievalItems[i], digestByteLength, byteL));
            }
        }
        return null;
    }
}
