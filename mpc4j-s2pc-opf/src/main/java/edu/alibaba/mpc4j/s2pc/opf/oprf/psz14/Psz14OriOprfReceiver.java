package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Psz14OriOprfReceiver extends AbstractOprfReceiver {
    /**
     * 核COT协议发送方
     */
    private final LcotReceiver lcotReceiver;
    /**
     * COT发送方输出
     */
    private LcotReceiverOutput lcotReceiverOutput;
    /**
     * H_1: {0,1}^* → {0,1}^{l}
     */
    private Hash h1;
    /**
     * Input Hash Length
     */
    private int l;


    public Psz14OriOprfReceiver(Rpc receiverRpc, Party senderParty, Psz14OriOprfConfig config) {
        super(Psz14OriOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lcotReceiver = LcotFactory.createReceiver(receiverRpc, senderParty, config.getLcotConfig());
        addSubPtos(lcotReceiver);
    }

    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        this.l = CommonConstants.STATS_BIT_LENGTH + Byte.SIZE * (int) Math.ceil(2.0 * Math.log(maxBatchSize) / Byte.SIZE);
        h1 = HashFactory.createInstance(envType, l / Byte.SIZE);
        lcotReceiver.init(Byte.SIZE, maxBatchSize * l / Byte.SIZE);
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
        List<byte[]> hashedInputList = inputStream.map(input -> h1.digestToBytes(input)).collect(Collectors.toList());
        List<Byte> choiceList = new ArrayList<>();
        for(int i = 0; i < inputs.length; i++)
            choiceList.addAll(Bytes.asList(hashedInputList.get(i)));
        List<byte[]> choices = new ArrayList<>();
        for(int i = 0; i < choiceList.size(); i++)
            choices.add(new byte[] {choiceList.get(i)});
        this.lcotReceiverOutput = lcotReceiver.receive(choices.toArray(new byte[choices.size()][]));
        OprfReceiverOutput receiverOutput = generateOprfOutput();

        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, cotTime);
        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private OprfReceiverOutput generateOprfOutput() {
        List<byte[]> prfList = new ArrayList<>();
        for (int i = 0; i < inputs.length; i++){
            prfList.add(lcotReceiverOutput.getRb(i * l / Byte.SIZE));
            for(int j = 1; j < l / Byte.SIZE; j++)
                BytesUtils.xori(prfList.get(i), lcotReceiverOutput.getRb(i * l / Byte.SIZE + j));
        }
        return new OprfReceiverOutput(lcotReceiverOutput.getOutputByteLength(), inputs, prfList.toArray(new byte[inputs.length][]));
    }
}