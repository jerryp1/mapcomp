package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;

import java.util.concurrent.TimeUnit;

public class Psz14OriOprfSender extends AbstractOprfSender {
    /**
     * 核LOT协议接收方
     */
    private final LcotSender lcotSender;
    /**
     * InputHash Length
     */
    private int l;

    public Psz14OriOprfSender(Rpc senderRpc, Party receiverParty, Psz14OriOprfConfig config) {
        super(Psz14OriOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        lcotSender = LcotFactory.createSender(senderRpc, receiverParty, config.getLcotConfig());
        addSubPtos(lcotSender);
    }
    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        this.l = CommonConstants.STATS_BIT_LENGTH + Byte.SIZE * (int) Math.ceil(2.0 * Math.log(maxBatchSize) / Byte.SIZE);
        // 初始化LOT协议
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        lcotSender.init(Byte.SIZE, maxBatchSize * l / Byte.SIZE);
        stopWatch.stop();
        long initLotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initLotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    public OprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 执行COT协议
        LcotSenderOutput lcotSenderOutput = lcotSender.send(batchSize * l / Byte.SIZE);

        Psz14OriOprfSenderOutput psz14OriOprfSenderOutput = new Psz14OriOprfSenderOutput(envType, batchSize, l, lcotSenderOutput);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, cotTime);
        logPhaseInfo(PtoState.PTO_END);
        return psz14OriOprfSenderOutput;
    }
}
