package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.llp23;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleKeywordCpPirServer;

import java.math.BigInteger;
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
     * β^{-1}
     */
    private BigInteger inverseBeta;
    /**
     * ecc
     */
    private final ByteFullEcc ecc;
    /**
     * prf map
     */
    private Map<ByteBuffer, byte[]> prfMap;

    public Llp23SingleKeywordCpPirServer(Rpc serverRpc, Party clientParty, Llp23SingleKeywordCpPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
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

        // preprocessing
        rowPreprocessing(keyValueMap, rowNum, colNum);

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
     * client preprocess.
     *
     * @param rowNum row num.
     * @param colNum col num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void rowPreprocessing(Map<ByteBuffer, ByteBuffer> keyValueMap, int rowNum, int colNum)
        throws MpcAbortException {
        stopWatch.start();
        ByteBuffer[] keywordList = keyValueMap.keySet().toArray(new ByteBuffer[0]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        byte[] prgSeed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prgSeed);
        byte[] mask = prg.extendToBytes(prgSeed);
        int pointByteLength = ecc.pointByteLength();
        BigInteger beta = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        inverseBeta = beta.modInverse(ecc.getN());
        prfMap = new ConcurrentHashMap<>(rowNum * colNum);
        for (int i = 0; i < rowNum; i++) {
            // generate blind element
            IntStream intStream = IntStream.range(0, colNum);
            intStream = parallel ? intStream.parallel() : intStream;
            int finalI = i;
            List<byte[]> blindPayload = intStream
                .mapToObj(j -> {
                    // hash to point
                    byte[] element = ecc.hashToCurve(keywordList[j + finalI * colNum].array());
                    // blinding
                    byte[] keyBlind = ecc.mul(element, beta);
                    byte[] valueBlind = BytesUtils.xor(mask, keyValueMap.get(keywordList[j + finalI * colNum]).array());
                    return (Bytes.concat(keyBlind, valueBlind));
                })
                .collect(Collectors.toList());
            DataPacketHeader blindPayloadHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(blindPayloadHeader, blindPayload));
            // receive shuffled blind element prf
            DataPacketHeader blindPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND_PRF.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
            MpcAbortPreconditions.checkArgument(blindPrfPayload.size() == colNum);
            byte[][] blindPrfArray = blindPrfPayload.toArray(new byte[0][]);
            // handle blind prf
            intStream = IntStream.range(0, colNum);
            intStream = parallel ? intStream.parallel() : intStream;
            prfMap.putAll(intStream.boxed()
                .collect(Collectors.toMap(
                        index -> {
                            byte[] keyBlindPrf = BytesUtils.clone(blindPrfArray[index], 0, pointByteLength);
                            return ByteBuffer.wrap(ecc.mul(keyBlindPrf, inverseBeta));
                        },
                        index -> {
                            byte[] valueBlindPrf = BytesUtils.clone(blindPrfArray[index], pointByteLength, byteL);
                            return BytesUtils.xor(mask, valueBlindPrf);
                        }
                    )
                )
            );
            extraInfo++;
        }
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime);
    }
}