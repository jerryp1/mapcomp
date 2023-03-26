package edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.okvs;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.Okvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.AbstractBlopprfSender;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.okvs.OkvsBlopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSenderOutput;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OKVS Batched l-bit-input OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsBlopprfSender extends AbstractBlopprfSender {
    /**
     * the OPRF sender
     */
    private final OprfSender oprfSender;
    /**
     * the OKVS type
     */
    private final OkvsFactory.OkvsType okvsType;

    public OkvsBlopprfSender(Rpc senderRpc, Party receiverParty, OkvsBlopprfConfig config) {
        super(OkvsBlopprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
       oprfSender = OprfFactory.createOprfSender(senderRpc, receiverParty, config.getOprfConfig());
       addSubPtos(oprfSender);
       okvsType = config.getOkvsType();
    }

    @Override
    public void init(int maxBatchSize, int maxPointNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfSender.init(maxBatchSize, maxPointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void opprf(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException {
        setPtoInput(l, inputArrays, targetArrays);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        OprfSenderOutput oprfSenderOutput = oprfSender.oprf(batchSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Sender runs OPRF");

        stopWatch.start();
        List<byte[]> keysPayload = new LinkedList<>();
        // generate PRF key
        byte[] prfKey = CommonUtils.generateRandomKey(secureRandom);
        keysPayload.add(prfKey);
        // generate OKVS keys
        byte[][] okvsKeys = CommonUtils.generateRandomKeys(OkvsFactory.getHashNum(okvsType), secureRandom);
        keysPayload.addAll(Arrays.asList(okvsKeys));
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keysHeader, keysPayload));
        stopWatch.stop();
        long keysTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, keysTime, "Sender sends keys");

        stopWatch.start();
        List<byte[]> okvsPayload = generateOkvsPayload(oprfSenderOutput, prfKey, okvsKeys);
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, okvsTime, "Sender sends OKVS");

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateOkvsPayload(OprfSenderOutput oprfSenderOutput, byte[] prfKey, byte[][] okvsKeys) {
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(envType, okvsType, pointNum, l, okvsKeys);
        okvs.setParallelEncode(parallel);
        // construct key-value map
        Map<ByteBuffer, byte[]> keyValueMap = new ConcurrentHashMap<>(pointNum);
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(prfKey);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        batchIntStream.forEach(batchIndex -> {
            byte[][] inputArray = inputArrays[batchIndex];
            byte[][] targetArray = targetArrays[batchIndex];
            assert inputArray.length == targetArray.length;
            int num = inputArray.length;
            for (int index = 0; index < num; index++) {
                byte[] input = inputArray[index];
                byte[] target = targetArray[index];
                byte[] programOutput = oprfSenderOutput.getPrf(batchIndex, input);
                programOutput = prf.getBytes(programOutput);
                BytesUtils.xori(programOutput, target);
                keyValueMap.put(ByteBuffer.wrap(input), programOutput);
            }
        });
        byte[][] okvsStorage = okvs.encode(keyValueMap);
        return Arrays.stream(okvsStorage).collect(Collectors.toList());
    }
}
