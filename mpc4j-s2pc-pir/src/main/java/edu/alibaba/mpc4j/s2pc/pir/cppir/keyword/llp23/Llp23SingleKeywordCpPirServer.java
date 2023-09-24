package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.llp23;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleKeywordCpPirServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.llp23.Llp23SingleKeywordCpPirDesc.*;

/**
 * LLP23 client-specific preprocessing PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class Llp23SingleKeywordCpPirServer extends AbstractSingleKeywordCpPirServer {

    /**
     * ecc
     */
    private final ByteFullEcc ecc;
    /**
     * PRF map
     */
    private Map<ByteBuffer, byte[]> prfMap;
    /**
     * hash
     */
    private final Hash hash;

    public Llp23SingleKeywordCpPirServer(Rpc serverRpc, Party clientParty, Llp23SingleKeywordCpPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(Map<ByteBuffer, ByteBuffer> keyValueMap, int labelBitLength) throws MpcAbortException {
        setInitInput(keyValueMap, labelBitLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int[] dimensionSize = PirUtils.computeDimensionLength(n, 2);
        int rowNum = dimensionSize[0];
        int colNum = dimensionSize[1];
        assert rowNum * colNum >= n;
        while (keyValueMap.size() < rowNum * colNum) {
            byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(key);
            if (!keyValueMap.containsKey(ByteBuffer.wrap(key))) {
                byte[] value = new byte[byteL];
                secureRandom.nextBytes(value);
                keyValueMap.put(ByteBuffer.wrap(key), ByteBuffer.wrap(value));
            }
        }
        stopWatch.stop();
        long paddingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.PTO_STEP, 0, 1, paddingTime,
            String.format(
                "Server sets params: n = %d, rowNum = %d, colNum = %d, n (pad) = %d",
                n, rowNum, colNum, rowNum * colNum
            )
        );

        // row preprocessing
        List<byte[]> rowProcessList = rowPreprocessing(keyValueMap, rowNum, colNum);
        // column preprocessing
        columnPreprocessing(rowProcessList, rowNum, colNum);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == 1);

        stopWatch.start();
        byte[] response = generateResponse(clientQueryPayload.get(0));
        DataPacketHeader responsePayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responsePayloadHeader, Collections.singletonList(response)));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * server generates response.
     *
     * @param query client query.
     * @return response.
     */
    private byte[] generateResponse(byte[] query) {
        ByteBuffer item = ByteBuffer.wrap(query);
        return prfMap.getOrDefault(item, botElementByteBuffer.array());
    }

    /**
     * client preprocess row elements.
     *
     * @param rowNum row num.
     * @param colNum col num.
     * @return row preprocess map.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> rowPreprocessing(Map<ByteBuffer, ByteBuffer> keyValueMap, int rowNum, int colNum)
        throws MpcAbortException {
        stopWatch.start();
        List<byte[]> rowPreprocessList = new ArrayList<>();
        ByteBuffer[] keywordList = keyValueMap.keySet().toArray(new ByteBuffer[0]);
        for (int i = 0; i < rowNum; i++) {
            IntStream intStream = IntStream.range(0, colNum);
            intStream = parallel ? intStream.parallel() : intStream;
            int finalI = i;
            List<byte[]> blindPayload = intStream
                .mapToObj(j -> {
                    // hash
                    byte[] digest = hash.digestToBytes(keywordList[j + finalI * colNum].array());
                    // concat value
                    return Bytes.concat(digest, keyValueMap.get(keywordList[j + finalI * colNum]).array());
                })
                .collect(Collectors.toList());
            DataPacketHeader blindPayloadHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(blindPayloadHeader, blindPayload));
            // receive shuffled element prf
            DataPacketHeader blindPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND_PRF.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
            MpcAbortPreconditions.checkArgument(blindPrfPayload.size() == colNum);
            rowPreprocessList.addAll(blindPrfPayload);
            extraInfo++;
        }
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, streamTime);

        return rowPreprocessList;
    }

    /**
     * client preprocess column elements.
     *
     * @param rowNum row num.
     * @param colNum col num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void columnPreprocessing(List<byte[]> rowPreprocessList, int rowNum, int colNum)
        throws MpcAbortException {
        stopWatch.start();
        prfMap = new ConcurrentHashMap<>(rowNum * colNum);
        byte[][] rowPreprocessArray = rowPreprocessList.toArray(new byte[0][]);
        for (int i = 0; i < colNum; i++) {
            IntStream intStream = IntStream.range(0, rowNum);
            intStream = parallel ? intStream.parallel() : intStream;
            int finalI = i;
            List<byte[]> blindPayload = intStream
                .mapToObj(j -> rowPreprocessArray[finalI + j * colNum])
                .collect(Collectors.toList());
            DataPacketHeader blindPayloadHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(blindPayloadHeader, blindPayload));
            // receive shuffled element prf
            DataPacketHeader blindPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND_PRF.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
            MpcAbortPreconditions.checkArgument(blindPrfPayload.size() == rowNum);
            byte[][] blindPrfArray = blindPrfPayload.toArray(new byte[0][]);
            intStream = IntStream.range(0, rowNum);
            intStream = parallel ? intStream.parallel() : intStream;
            prfMap.putAll(intStream.boxed()
                .collect(Collectors.toMap(
                        index -> {
                            byte[] keyBlindPrf = BytesUtils.clone(blindPrfArray[index], 0, ecc.pointByteLength());
                            return ByteBuffer.wrap(keyBlindPrf);
                        },
                        index -> BytesUtils.clone(blindPrfArray[index], ecc.pointByteLength(), byteL)
                    )
                )
            );
            extraInfo++;
        }
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, streamTime);
    }
}