package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Psz14OptOprfReceiver extends AbstractOprfReceiver {
    /**
     * 核COT协议发送方
     */
    private final LcotReceiver lcotReceiver;
    /**
     * H_1: {0,1}^* → {0,1}^{l}
     */
    private Hash h1;


    public Psz14OptOprfReceiver(Rpc receiverRpc, Party senderParty, Psz14OptOprfConfig config) {
        super(Psz14OptOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lcotReceiver = LcotFactory.createReceiver(receiverRpc, senderParty, config.getLcotConfig());
        addSubPtos(lcotReceiver);
    }

    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int l = CommonConstants.STATS_BIT_LENGTH + Byte.SIZE * (int) Math.ceil(2.0 * Math.log(maxBatchSize) / Byte.SIZE);
        h1 = HashFactory.createInstance(envType, l /Byte.SIZE);
        lcotReceiver.init(l, maxBatchSize);
        stopWatch.stop();
        long initLotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initLotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public OprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();

        // 执行COT协议
        Stream<byte[]> inputStream = Arrays.stream(inputs);
        inputStream = parallel ? inputStream.parallel() : inputStream;
        LcotReceiverOutput lotReceiverOutput = lcotReceiver.receive(inputStream.map(input -> h1.digestToBytes(input)).toArray(byte[][]::new));
        OprfReceiverOutput receiverOutput = new OprfReceiverOutput(lotReceiverOutput.getOutputByteLength(), inputs, lotReceiverOutput.getRbArray());
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, cotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
