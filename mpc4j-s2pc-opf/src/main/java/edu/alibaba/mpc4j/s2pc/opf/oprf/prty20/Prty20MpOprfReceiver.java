package edu.alibaba.mpc4j.s2pc.opf.oprf.prty20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.BinaryGf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Prty20MpOprfReceiver extends AbstractMpOprfReceiver {
    /**
     * 核COT协议发送方
     */
    private final LcotReceiver lcotReceiver;
    /**
     * 规约批处理数量
     */
    private int n;
    /**
     * output length
     */
    private int l;
    /**
     * H_1: {0,1}^* → {0,1}^{l1}
     */
    private Hash h1;
    /**
     * COT发送方输出
     */
    private LcotReceiverOutput lcotReceiverOutput;
    /**
     * OKVS type
     */
    private Gf2eDokvsType okvsType;
    /**
     * D-OKVS
     */
    private BinaryGf2eDokvs<byte[]> dArrayOkvs;
    /**
     * R-OKVS
     */
    private BinaryGf2eDokvs<byte[]> rArrayOkvs;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;

    public Prty20MpOprfReceiver(Rpc receiverRpc, Party senderParty, Prty20MpOprfConfig config) {
        super(Prty20MpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lcotReceiver = LcotFactory.createReceiver(receiverRpc, senderParty, config.getLcotConfig());
        addSubPtos(lcotReceiver);
        okvsType = config.getBinaryOkvsType();
    }

    public void setOkvsType(Gf2eDokvsType type){
        this.okvsType = type;
    }

    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 计算maxM，初始化LOT协议
        //SemiHonest Setting
        this.l = CommonConstants.STATS_BIT_LENGTH + Byte.SIZE * (int) Math.ceil(2.0 * Math.log(maxBatchSize) / Byte.SIZE);
        this.n = maxBatchSize;
        this.okvsKeys = CommonUtils.generateRandomKeys(Gf2eDokvsFactory.getHashKeyNum(okvsType), secureRandom);
        dArrayOkvs = Gf2eDokvsFactory.createBinaryInstance(envType,okvsType,maxBatchSize, l, okvsKeys);
        int m = dArrayOkvs.getM();
        h1 = HashFactory.createInstance(envType,l/Byte.SIZE);
        List<byte[]> okvsKeyPayload = Arrays.stream(okvsKeys).collect(Collectors.toList());
        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty20MpOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsKeyHeader, okvsKeyPayload));
        lcotReceiver.init(l, m);
        stopWatch.stop();
        long initLotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initLotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Map<byte[], byte[]> keyValueMap = new HashMap<>();
        IntStream.range(0, inputs.length).forEach(index -> {
            byte[] valueBytes = h1.digestToBytes(inputs[index]);
            keyValueMap.put(inputs[index], valueBytes);
        });
        // 方案所使用的OKVS是PaXoS，参数置为false
        byte[][] dArray = dArrayOkvs.encode(keyValueMap, false);

        // 执行LOT协议
        lcotReceiverOutput = lcotReceiver.receive(dArray);
        rArrayOkvs = Gf2eDokvsFactory.createBinaryInstance(envType,okvsType, n , lcotReceiverOutput.getOutputByteLength() * Byte.SIZE, okvsKeys);

        MpOprfReceiverOutput receiverOutput = generateOprfOutput();
        rArrayOkvs = null;
        dArrayOkvs = null;
        okvsKeys = null;
        h1 = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, cotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private MpOprfReceiverOutput generateOprfOutput() {
        IntStream inputIndexStream = IntStream.range(0, batchSize);
        inputIndexStream = parallel ? inputIndexStream.parallel() : inputIndexStream;
        byte[][] prfs = inputIndexStream
                .mapToObj(index -> rArrayOkvs.decode(lcotReceiverOutput.getRbArray(),inputs[index]))
                .toArray(byte[][]::new);
        int oprfLength = lcotReceiverOutput.getOutputByteLength();
        lcotReceiverOutput = null;
        return new MpOprfReceiverOutput(oprfLength, inputs, prfs);
    }

}
