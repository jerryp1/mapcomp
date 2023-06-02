package edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.crypto.matrix.matrix.Zl64Matrix;
import edu.alibaba.mpc4j.crypto.matrix.vector.Zl64Vector;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SingleIndexPirPtoDesc.*;

/**
 * Simple PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/5/30
 */
public class Hhcm23SingleIndexPirClient extends AbstractSingleIndexPirClient {

    /**
     * Simple PIR params
     */
    private Hhcm23SingleIndexPirParams params;
    /**
     * dimension length
     */
    private int dimensionLength;
    /**
     * hint
     */
    private Zl64Matrix[] hint;
    /**
     * secret key
     */
    private Zl64Vector secretKey;
    /**
     * random matrix A
     */
    private Zl64Matrix a;
    /**
     * row index
     */
    private int rowIndex;

    public Hhcm23SingleIndexPirClient(Rpc clientRpc, Party serverParty, Hhcm23SingleIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
    }


    @Override
    public void init(SingleIndexPirParams indexPirParams, int serverElementSize, int elementBitLength)
        throws MpcAbortException {
        assert indexPirParams instanceof Hhcm23SingleIndexPirParams;
        params = (Hhcm23SingleIndexPirParams) indexPirParams;
        assert (1L << params.expectElementLogSize) > serverElementSize;
        logPhaseInfo(PtoState.INIT_BEGIN);

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
        clientSetup(serverElementSize, elementBitLength);
        handledSeedPayload(seedPayload);
        handledHintPayload(hintPayload);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength) throws MpcAbortException {
        params = Hhcm23SingleIndexPirParams.SERVER_ELEMENT_LOG_SIZE_30;
        assert (1L << params.expectElementLogSize) > serverElementSize;
        logPhaseInfo(PtoState.INIT_BEGIN);

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
        clientSetup(serverElementSize, elementBitLength);
        handledSeedPayload(seedPayload);
        handledHintPayload(hintPayload);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // client generates query
        stopWatch.start();
        List<byte[]> clientQueryPayload = generateQuery(index);
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        // receive response
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        stopWatch.start();
        byte[] element = decodeResponse(serverResponsePayload, rowIndex);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return element;
    }

    @Override
    public List<byte[]> clientSetup(int serverElementSize, int elementBitLength) {
        if (params == null) {
            params = Hhcm23SingleIndexPirParams.SERVER_ELEMENT_LOG_SIZE_30;
        }
        int maxPartitionBitLength = PirUtils.getBitLength(params.p) - 1;
        setInitInput(serverElementSize, elementBitLength, maxPartitionBitLength);
        dimensionLength = (int) Math.max(2, Math.ceil(Math.pow(num, 1.0 / 2)));
        // secret key
        secretKey = Zl64Vector.createRandom(params.zl64, params.n, secureRandom);
        return null;
    }

    @Override
    public List<byte[]> generateQuery(int index) {
        // row index
        rowIndex = index / dimensionLength;
        // column index
        int colIndex = index % dimensionLength;
        // q / p
        long floor = params.q / params.p;
        // error term, allow correctness error 𝛿 = 2^{−40}.
        Zl64Vector error = Zl64Vector.createGaussianSample(params.zl64, dimensionLength, 0, params.stdDev);
        // query = A * s + e + q/p * u_i_col
        Zl64Vector query = a.matrixMulVector(secretKey).add(error);
        // Add q/p * 1 only to the index corresponding to the desired column
        long[] elements = query.getElements();
        elements[colIndex] = params.zl64.add(elements[colIndex], floor);
        return Collections.singletonList(LongUtils.longArrayToByteArray(elements));
    }

    @Override
    public byte[] decodeResponse(List<byte[]> serverResponse, int rowIndex) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() == partitionSize);
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        for (int i = 0; i < partitionSize; i++) {
            Zl64Vector vector = hint[i].matrixMulVector(secretKey);
            long[] responseElements = LongUtils.byteArrayToLongArray(serverResponse.get(i));
            Zl64Vector response = Zl64Vector.create(params.zl64, responseElements);
            response.setParallel(parallel);
            response.subi(vector);
            int element = (int) (Math.round(response.getElement(rowIndex) * params.p * 1.0 / params.q) % params.p);
            byte[] bytes = IntUtils.nonNegIntToFixedByteArray(element, partitionByteLength);
            databases[i] = ZlDatabase.create(partitionBitLength, new byte[][]{bytes});
        }
        return NaiveDatabase.createFromZl(elementBitLength, databases).getBytesData(0);
    }

    /**
     * client handles hint payload.
     *
     * @param hintPayload hint payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void handledHintPayload(List<byte[]> hintPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hintPayload.size() == partitionSize);
        hint = new Zl64Matrix[partitionSize];
        IntStream.range(0, partitionSize).forEach(i -> {
            long[] elements = LongUtils.byteArrayToLongArray(hintPayload.get(i));
            hint[i] = Zl64Matrix.create(params.zl64, elements, dimensionLength, params.n);
            hint[i].setParallel(parallel);
        });
    }

    /**
     * client handles seed payload.
     *
     * @param seedPayload seed payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void handledSeedPayload(List<byte[]> seedPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        a = Zl64Matrix.createRandom(params.zl64, dimensionLength, params.n, seed);
        a.setParallel(parallel);
    }
}
