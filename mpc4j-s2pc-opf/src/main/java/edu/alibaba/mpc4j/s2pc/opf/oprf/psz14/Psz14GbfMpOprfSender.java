package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Psz14GbfMpOprfSender extends AbstractMpOprfSender {
    /**
     * 核LOT协议接收方
     */
    private final CoreCotSender coreCotSender;
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
    private CotSenderOutput cotSenderOutput;
    /**
     * OKVS type
     */
    private Gf2eDokvsFactory.Gf2eDokvsType okvsType;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;

    public Psz14GbfMpOprfSender(Rpc senderRpc, Party receiverParty, Psz14GbfMpOprfConfig config) {
        super(Psz14GbfMpOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPtos(coreCotSender);
        this.okvsType = config.getBinaryOkvsType();
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
            this.encodeTaskId, getPtoDesc().getPtoId(), Psz14GbfMpOprfPtoDesc.PtoStep.RECEIVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsKeyPayload = rpc.receive(okvsKeyHeader).getPayload();
        this.okvsKeys = okvsKeyPayload.toArray(new byte[0][]);
        //SemiHonest Setting
        Gf2eDokvs<byte[]> okvs = Gf2eDokvsFactory.createBinaryInstance(envType, okvsType, maxBatchSize, CommonConstants.BLOCK_BIT_LENGTH, okvsKeys);
        this.m = okvs.getM();
        // 初始化COT协议
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, m);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    public MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 执行COT协议
        CotSenderOutput cotSenderOutput = coreCotSender.send(m);

        Psz14GbfMpOprfSenderOutput psz14GbfMpOprfSenderOutput = new Psz14GbfMpOprfSenderOutput(envType,
            batchSize, cotSenderOutput.getR1Array(), okvsKeys, okvsType);
        okvsKeys = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, cotTime);

        logPhaseInfo(PtoState.PTO_END);
        return psz14GbfMpOprfSenderOutput;
    }
}
