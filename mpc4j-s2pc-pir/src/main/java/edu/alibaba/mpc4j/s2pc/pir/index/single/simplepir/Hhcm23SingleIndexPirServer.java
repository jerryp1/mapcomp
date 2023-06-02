package edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.crypto.matrix.matrix.Zl64Matrix;
import edu.alibaba.mpc4j.crypto.matrix.vector.Zl64Vector;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SingleIndexPirPtoDesc.*;

/**
 * Simple PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/5/30
 */
public class Hhcm23SingleIndexPirServer extends AbstractSingleIndexPirServer {

    /**
     * Simple PIR params
     */
    private Hhcm23SingleIndexPirParams params;
    /**
     * hint
     */
    private Zl64Matrix[] hint;
    /**
     * database
     */
    private Zl64Matrix[] db;
    /**
     * random seed
     */
    private byte[] seed;

    public Hhcm23SingleIndexPirServer(Rpc serverRpc, Party clientParty, Hhcm23SingleIndexPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        assert indexPirParams instanceof Hhcm23SingleIndexPirParams;
        params = (Hhcm23SingleIndexPirParams) indexPirParams;
        assert (1L << params.expectElementLogSize) > database.rows();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        serverSetup(database);
        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedPayloadHeader, Collections.singletonList(seed)));
        List<byte[]> hintPayload = IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(hint[i].elements))
            .collect(Collectors.toList());
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hintPayloadHeader, hintPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(NaiveDatabase database) throws MpcAbortException {
        params = Hhcm23SingleIndexPirParams.SERVER_ELEMENT_LOG_SIZE_30;
        assert (1L << params.expectElementLogSize) > database.rows();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        serverSetup(database);
        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedPayloadHeader, Collections.singletonList(seed)));
        List<byte[]> hintPayload = IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(hint[i].elements))
            .collect(Collectors.toList());
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hintPayloadHeader, hintPayload));
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

        // receive query
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        // generate response
        stopWatch.start();
        List<byte[]> serverResponsePayload = generateResponse(clientQueryPayload, new ArrayList<>());
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Client generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    public void setPublicKey(List<byte[]> clientPublicKeysPayload) {
        // empty
    }

    @Override
    public List<byte[][]> serverSetup(NaiveDatabase database) {
        int maxPartitionBitLength = PirUtils.getBitLength(params.p) - 1;
        setInitInput(database, database.getL(), maxPartitionBitLength);
        int dimensionLength = (int) Math.max(2, Math.ceil(Math.pow(num, 1.0 / 2)));
        // public matrix A
        seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        Zl64Matrix a = Zl64Matrix.createRandom(params.zl64, dimensionLength, params.n, seed);
        // generate the client's hint, which is the database multiplied by A. Also known as the setup.
        db = new Zl64Matrix[partitionSize];
        hint = new Zl64Matrix[partitionSize];
        for (int i = 0; i < partitionSize; i++) {
            long[] elements = new long[dimensionLength * dimensionLength];
            for (int j = 0; j < num; j++) {
                elements[j] = IntUtils.fixedByteArrayToNonNegInt(databases[i].getBytesData(j));
            }
            // padding elements
            for (int j = num; j < dimensionLength * dimensionLength; j++) {
                elements[j] = 1L;
            }
            // values mod the plaintext modulus p
            db[i] = Zl64Matrix.create(params.zl64, elements, dimensionLength, dimensionLength);
            db[i].setParallel(parallel);
            hint[i] = db[i].matrixMul(a);
        }
        return null;
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery, List<byte[][]> empty) throws MpcAbortException {
        long[] queryElements = LongUtils.byteArrayToLongArray(clientQuery.get(0));
        Zl64Vector query = Zl64Vector.create(params.zl64, queryElements);
        return IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(db[i].matrixMulVector(query).getElements()))
            .collect(Collectors.toList());
    }
}
