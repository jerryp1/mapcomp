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
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
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
    private BigInteger[] alpha;
    /**
     * hash
     */
    private final Hash hash;
    /**
     * PRF
     */
    private Prf[] prf;

    public Llp23SingleKeywordCpPirClient(Rpc clientRpc, Party serverParty, Llp23SingleKeywordCpPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
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

        prf = new Prf[2];
        alpha = new BigInteger[2];
        // preprocessing
        rowPreprocessing(rowNum, colNum);
        columnPreprocessing(rowNum, colNum);

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
        ByteBuffer value = handleResponse(responsePayload.get(0), item.array());
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, decodeResponseTime, "Client decodes response");

        logPhaseInfo(PtoState.PTO_END);
        return value;
    }

    /**
     * client preprocess row elements.
     *
     * @param rowNum row num.
     * @param colNum col num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void rowPreprocessing(int rowNum, int colNum) throws MpcAbortException {
        stopWatch.start();
        prf[0] = PrfFactory.createInstance(envType, byteL);
        byte[] prfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prfKey);
        prf[0].setKey(prfKey);
        alpha[0] = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
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
                    byte[] digest = BytesUtils.clone(permutedBlindElementsArray[index], 0, hash.getOutputByteLength());
                    byte[] value = BytesUtils.clone(permutedBlindElementsArray[index], hash.getOutputByteLength(), byteL);
                    // blind key prf
                    byte[] point = ecc.hashToCurve(digest);
                    byte[] keyBlindPrf = ecc.mul(point, alpha[0]);
                    byte[] keyPrf = prf[0].getBytes(keyBlindPrf);
                    byte[] valueBlindPrf = BytesUtils.xor(keyPrf, value);
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
     * client preprocess column elements.
     *
     * @param rowNum row num.
     * @param colNum col num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void columnPreprocessing(int rowNum, int colNum) throws MpcAbortException {
        stopWatch.start();
        prf[1] = PrfFactory.createInstance(envType, byteL);
        byte[] prfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prfKey);
        prf[1].setKey(prfKey);
        alpha[1] = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        // preprocess
        for (int i = 0; i < colNum; i++) {
            // receive blind element
            DataPacketHeader blindPayloadHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> blindPayload = rpc.receive(blindPayloadHeader).getPayload();
            MpcAbortPreconditions.checkArgument(blindPayload.size() == rowNum);
            // shuffle blind element
            Collections.shuffle(blindPayload, secureRandom);
            byte[][] permutedBlindElementsArray = blindPayload.toArray(new byte[0][]);
            IntStream intStream = IntStream.range(0, rowNum);
            intStream = parallel ? intStream.parallel() : intStream;
            List<byte[]> blindPrfPayload = intStream
                .mapToObj(index -> {
                    byte[] point = BytesUtils.clone(permutedBlindElementsArray[index], 0, ecc.pointByteLength());
                    byte[] value = BytesUtils.clone(permutedBlindElementsArray[index], ecc.pointByteLength(), byteL);
                    // blind key prf
                    byte[] keyBlindPrf = ecc.mul(point, alpha[1]);
                    byte[] keyPrf = prf[1].getBytes(keyBlindPrf);
                    byte[] valueBlindPrf = BytesUtils.xor(keyPrf, value);
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
        byte[] digest = hash.digestToBytes(key.array());
        byte[] eccPoint = ecc.hashToCurve(digest);
        return ecc.mul(ecc.mul(eccPoint, alpha[0]), alpha[1]);
    }

    /**
     * client handle server response.
     *
     * @param response response.
     * @param item     item.
     * @return value.
     */
    private ByteBuffer handleResponse(byte[] response, byte[] item) {
        if (Objects.equals(ByteBuffer.wrap(response), botElementByteBuffer)) {
            return null;
        } else {
            byte[] digest = hash.digestToBytes(item);
            byte[] point = ecc.hashToCurve(digest);
            byte[] h1 = ecc.mul(point, alpha[0]);
            byte[] h2 = ecc.mul(h1, alpha[1]);
            byte[] f1 = prf[0].getBytes(h1);
            byte[] f2 = prf[1].getBytes(h2);
            return ByteBuffer.wrap(BytesUtils.xor(BytesUtils.xor(response, f1), f2));
        }
    }
}