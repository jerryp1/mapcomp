package edu.alibaba.mpc4j.s2pc.opf.oprf.prty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.hashbin.object.TwoChoiceHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Prty19FastMpOprfSender extends AbstractMpOprfSender {
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * 选择比特
     */
    private boolean[] s;
    /**
     * 2choiceHash keys
     */
    private byte[][] hashBinKeys;
    /**
     * BinNum
     */
    private int binNum;
    /**
     * BinSize
     */
    private int binSize;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;

    public Prty19FastMpOprfSender(Rpc senderRpc, Party receiverParty, Prty19FastMpOprfConfig config) {
        super(Prty19FastMpOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPtos(coreCotReceiver);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 计算l，初始化COT协议
        int maxL = Prty19MpOprfUtils.getL(maxBatchSize);
        coreCotReceiver.init(maxL);
        DataPacketHeader keyHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty19FastMpOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keyPayload = rpc.receive(keyHeader).getPayload();
        byte[][] keys = keyPayload.toArray(new byte[0][]);
        this.hashBinKeys = Arrays.copyOfRange(keys, 0 ,2);
        this.okvsKeys = Arrays.copyOfRange(keys, 2, keys.length);
        this.binNum = TwoChoiceHashBin.expectedBinNum(maxBatchSize);
        this.binSize = TwoChoiceHashBin.expectedMaxBinSize(maxBatchSize);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int l = Prty19MpOprfUtils.getL(batchSize);
        int lByteLength = CommonUtils.getByteLength(l);
        s = new boolean[lByteLength * Byte.SIZE];
        IntStream.range(0, l).forEach(index -> s[index] = secureRandom.nextBoolean());
        // 执行COT协议
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(Arrays.copyOf(s, l));
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        stopWatch.start();
        DataPacketHeader storageHeader = new DataPacketHeader(
                this.encodeTaskId, ptoDesc.getPtoId(), Prty19FastMpOprfPtoDesc.PtoStep.RECEIVER_SEND_STORAGE.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> storagePayload = rpc.receive(storageHeader).getPayload();
        // 读取矩阵
        Prty19FastMpOprfSenderOutput senderOutput = new Prty19FastMpOprfSenderOutput(envType, batchSize, hashBinKeys, binNum, binSize,
            cotReceiverOutput.getRbArray(), BinaryUtils.binaryToByteArray(s), storagePayload.toArray(new byte[0][]), okvsType, okvsKeys, parallel);
        okvsKeys = null;
        s = null;
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, encodeTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
