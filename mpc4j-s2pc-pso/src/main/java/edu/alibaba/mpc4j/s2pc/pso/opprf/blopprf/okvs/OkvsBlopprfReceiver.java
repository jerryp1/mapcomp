package edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.okvs;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.Okvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.AbstractBlopprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.okvs.OkvsBlopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfReceiverOutput;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * OKVS Batched l-bit-input OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsBlopprfReceiver extends AbstractBlopprfReceiver {
    /**
     * the OPRF receiver
     */
    private final OprfReceiver oprfReceiver;
    /**
     * the OKVS type
     */
    private final OkvsFactory.OkvsType okvsType;

    public OkvsBlopprfReceiver(Rpc receiverRpc, Party senderParty, OkvsBlopprfConfig config) {
        super(OkvsBlopprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(receiverRpc, senderParty, config.getOprfConfig());
        addSubPtos(oprfReceiver);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(int maxBatchSize, int maxPointNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfReceiver.init(maxBatchSize, maxPointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] opprf(int l, byte[][] inputArray, int targetNum) throws MpcAbortException {
        setPtoInput(l, inputArray, targetNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(inputArray);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Receiver runs OPRF");

        // receive keys
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        // receive OKVS
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();

        stopWatch.start();
        byte[][] outputArray = handleOkvsPayload(oprfReceiverOutput, keysPayload, okvsPayload);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, okvsTime, "Receiver handles OKVS");

        logPhaseInfo(PtoState.PTO_END);
        return outputArray;
    }

    private byte[][] handleOkvsPayload(OprfReceiverOutput oprfReceiverOutput,
                                       List<byte[]> keysPayload, List<byte[]> okvsPayload) throws MpcAbortException {
        // parse keys
        MpcAbortPreconditions.checkArgument(keysPayload.size() == OkvsFactory.getHashNum(okvsType) + 1);
        byte[] prfKey = keysPayload.remove(0);
        byte[][] okvsKeys = keysPayload.toArray(new byte[0][]);
        // parse OKVS storage
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == OkvsFactory.getM(okvsType, pointNum));
        byte[][] okvsStorage = okvsPayload.toArray(new byte[0][]);
        // compute PRF output
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(envType, okvsType, pointNum, l, okvsKeys);
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(prfKey);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(batchIndex -> {
                byte[] input = inputArray[batchIndex];
                byte[] programOutput = oprfReceiverOutput.getPrf(batchIndex);
                programOutput = prf.getBytes(programOutput);
                byte[] okvsOutput = okvs.decode(okvsStorage, ByteBuffer.wrap(input));
                BytesUtils.xori(programOutput, okvsOutput);
                return programOutput;
            })
            .toArray(byte[][]::new);
    }
}
