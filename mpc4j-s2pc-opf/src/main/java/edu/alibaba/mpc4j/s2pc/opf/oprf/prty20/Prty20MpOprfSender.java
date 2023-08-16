package edu.alibaba.mpc4j.s2pc.opf.oprf.prty20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.BinaryGf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Prty20MpOprfSender extends AbstractMpOprfSender {
    /**
     * 核LOT协议接收方
     */
    private final LcotSender lcotSender;
    /**
     * output length
     */
    private int l;
    /**
     * OKVS length
     */
    private int m;
    /**
     * 批处理数量字节长度
     */
    private int nByteLength;
    /**
     * COT接收方输出
     */
    private LcotSenderOutput lcotSenderOutput;
    /**
     * OKVS type
     */
    private Gf2eDokvsFactory.Gf2eDokvsType okvsType;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;

    public Prty20MpOprfSender(Rpc senderRpc, Party receiverParty, Prty20MpOprfConfig config) {
        super(Prty20MpOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        lcotSender = LcotFactory.createSender(senderRpc, receiverParty, config.getLcotConfig());
        addSubPtos(lcotSender);
        this.okvsType = config.getBinaryOkvsType();
    }

    public void setOkvsType(Gf2eDokvsFactory.Gf2eDokvsType type){
        this.okvsType = type;
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty20MpOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsKeyPayload = rpc.receive(okvsKeyHeader).getPayload();
        this.okvsKeys = okvsKeyPayload.toArray(new byte[0][]);
        //SemiHonest Setting
        this.l = CommonConstants.STATS_BIT_LENGTH + Byte.SIZE * (int) Math.ceil(2.0 * Math.log(maxBatchSize) / Byte.SIZE);
        BinaryGf2eDokvs<byte[]> okvs = Gf2eDokvsFactory.createBinaryInstance(envType, okvsType, maxBatchSize, l, okvsKeys);
        this.m = okvs.getM();
        // 初始化LOT协议
        lcotSender.init(l,m);
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
        // 执行LOT协议
        LcotSenderOutput lcotSenderOutput = lcotSender.send(m);
        Prty20MpOprfSenderOutput prty20MpOprfSenderOutput = new Prty20MpOprfSenderOutput(envType,batchSize,lcotSenderOutput,okvsType,batchSize, l, okvsKeys);
        okvsKeys = null;
        this.lcotSenderOutput = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, cotTime);
        logPhaseInfo(PtoState.PTO_END);
        return prty20MpOprfSenderOutput;
    }


}
