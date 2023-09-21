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
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleKeywordCpPirClient;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.llp23.Llp23SingleKeywordCpPirDesc.*;


/**
 * LLP23 client-specific preprocessing PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class Llp23SingleKeywordCpPirClient extends AbstractSingleKeywordCpPirClient {
    /**
     * ecc
     */
    private final ByteFullEcc ecc;
    /**
     * PRF key
     */
    private BigInteger alpha;
    /**
     * mask
     */
    private byte[] mask;

    public Llp23SingleKeywordCpPirClient(Rpc clientRpc, Party serverParty, Llp23SingleKeywordCpPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int[] dimensionSize = PirUtils.computeDimensionLength(n, 2);
        int rowNum = dimensionSize[0];
        int colNum = dimensionSize[1];
        assert rowNum * colNum >= n;
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.PTO_STEP, 0, 1, paramTime,
            String.format(
                "Client sets params: n = %d, rowNum = %d, colNum = %d, n (pad) = %d",
                n, rowNum, colNum, rowNum * colNum
            )
        );

        // preprocessing
        preprocessing(rowNum, colNum);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ByteBuffer pir(ByteBuffer item) throws MpcAbortException {
        setPtoInput(item);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[] clientQuery = generateQuery(item);
        DataPacketHeader queryPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryPayloadHeader, Collections.singletonList(clientQuery)));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(serverResponseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(responsePayload.size() == 1);

        stopWatch.start();
        ByteBuffer value = handleResponse(responsePayload.get(0));
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, decodeResponseTime, "Client decodes response");

        logPhaseInfo(PtoState.PTO_END);
        return value;
    }

    /**
     * client preprocess.
     *
     * @param rowNum row num.
     * @param colNum col num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void preprocessing(int rowNum, int colNum) throws MpcAbortException {
        stopWatch.start();
        // create PRG
        Prg prg = PrgFactory.createInstance(envType, byteL);
        byte[] prgSeed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prgSeed);
        mask = prg.extendToBytes(prgSeed);
        // prf key
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        // point byte length
        int pointByteLength = ecc.pointByteLength();
        // preprocess
        for (int i = 0; i < rowNum; i++) {
            // receive blind element
            DataPacketHeader blindPayloadHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> blindPayload = rpc.receive(blindPayloadHeader).getPayload();
            MpcAbortPreconditions.checkArgument(blindPayload.size() == colNum);
            // shuffle blind element
            Collections.shuffle(blindPayload, secureRandom);
            byte[][] permutedBlindElementsArray = blindPayload.toArray(new byte[0][]);
            IntStream intStream = IntStream.range(0, colNum);
            intStream = parallel ? intStream.parallel() : intStream;
            List<byte[]> blindPrfPayload = intStream
                .mapToObj(index -> {
                    byte[] keyBlind = BytesUtils.clone(permutedBlindElementsArray[index], 0, pointByteLength);
                    byte[] valueBlind = BytesUtils.clone(permutedBlindElementsArray[index], pointByteLength, byteL);
                    // blind key prf
                    byte[] keyBlindPrf = ecc.mul(keyBlind, alpha);
                    // blind value prf
                    byte[] valueBlindPrf = BytesUtils.xor(mask, valueBlind);
                    return Bytes.concat(keyBlindPrf, valueBlindPrf);
                })
                .collect(Collectors.toList());
            // send blind element prf
            DataPacketHeader blindPrfPayloadHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND_PRF.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(blindPrfPayloadHeader, blindPrfPayload));
            extraInfo++;
        }
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime);
    }

    /**
     * client generates query.
     *
     * @param key key.
     * @return query.
     */
    private byte[] generateQuery(ByteBuffer key) {
        byte[] element = ecc.hashToCurve(key.array());
        return ecc.mul(element, alpha);
    }

    /**
     * client handle server response.
     *
     * @param response response.
     * @return value.
     */
    private ByteBuffer handleResponse(byte[] response) {
        ByteBuffer item = ByteBuffer.wrap(response);
        return Objects.equals(item, botElementByteBuffer) ? null : ByteBuffer.wrap(BytesUtils.xor(mask, response));
    }
}