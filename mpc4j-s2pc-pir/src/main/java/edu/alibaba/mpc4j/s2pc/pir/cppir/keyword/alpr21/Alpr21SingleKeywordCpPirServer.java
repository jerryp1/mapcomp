package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleKeywordCpPirServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;
import static edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleKeywordCpPirDesc.*;

/**
 * ALPR21 client-specific preprocessing PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class Alpr21SingleKeywordCpPirServer extends AbstractSingleKeywordCpPirServer {
    /**
     * single index client-specific preprocessing PIR server
     */
    private final SingleIndexCpPirServer singleIndexCpPirServer;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
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
     * keyword list
     */
    private List<ByteBuffer> keywordList;

    public Alpr21SingleKeywordCpPirServer(Rpc serverRpc, Party clientParty, Alpr21SingleKeywordCpPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        singleIndexCpPirServer = SingleIndexCpPirFactory.createServer(serverRpc, clientParty, config.getIndexCpPirConfig());
        addSubPtos(singleIndexCpPirServer);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = getHashNum(cuckooHashBinType);
        digestByteLength = config.getDigestByteLength();
    }

    @Override
    public void init(Map<ByteBuffer, ByteBuffer> keyValueMap, int labelBitLength) throws MpcAbortException {
        setInitInput(keyValueMap, labelBitLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        MathPreconditions.checkGreaterOrEqual("statistical security",
            digestByteLength * Byte.SIZE, PirUtils.getBitLength(n) + CommonConstants.STATS_BIT_LENGTH);

        // compute keyword prf
        stopWatch.start();
        keywordList = new ArrayList<>(keyValueMap.keySet());
        List<ByteBuffer> keywordPrf = computeKeywordPrf();
        Map<ByteBuffer, ByteBuffer> prfLabelMap = IntStream.range(0, n)
            .boxed()
            .collect(
                Collectors.toMap(keywordPrf::get, i -> keyValueMap.get(keywordList.get(i)), (a, b) -> b)
            );
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, prfTime);

        // init index pir
        stopWatch.start();
        ZlDatabase database = generateCuckooHashBin(keywordPrf, prfLabelMap);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        singleIndexCpPirServer.init(database);
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

        stopWatch.start();
        for (int i = 0; i < hashNum; i++) {
            singleIndexCpPirServer.pir();
        }
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
        Hash hash = HashFactory.createInstance(envType, digestByteLength);
        Stream<ByteBuffer> keywordStream = keywordList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(byteBuffer -> hash.digestToBytes(byteBuffer.array()))
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
    private ZlDatabase generateCuckooHashBin(List<ByteBuffer> keywordPrf, Map<ByteBuffer, ByteBuffer> prfLabelMap) {
        CuckooHashBin<ByteBuffer> cuckooHashBin = createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, n, keywordPrf, secureRandom
        );
        hashKeys = cuckooHashBin.getHashKeys();
        byte[] botElementByteArray = new byte[digestByteLength];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        ByteBuffer botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        int binNum = cuckooHashBin.binNum();
        byte[][] cuckooHashBinItems = new byte[binNum][];
        for (int i = 0; i < binNum; i++) {
            ByteBuffer item = cuckooHashBin.getHashBinEntry(i).getItem();
            byte[] value = new byte[byteL];
            if (prfLabelMap.get(item) != null) {
                value = prfLabelMap.get(item).array();
            } else {
                secureRandom.nextBytes(value);
            }
            cuckooHashBinItems[i] = Bytes.concat(item.array(), value);
        }
        return ZlDatabase.create((digestByteLength + byteL) * Byte.SIZE, cuckooHashBinItems);
    }
}
