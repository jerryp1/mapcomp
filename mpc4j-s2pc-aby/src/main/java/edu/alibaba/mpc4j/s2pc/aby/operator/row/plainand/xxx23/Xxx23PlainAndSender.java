package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.xxx23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.AbstractPlainAndParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.xxx23.Xxx23PlainAndPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Plain and sender.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public class Xxx23PlainAndSender extends AbstractPlainAndParty {
    /**
     * COT sender
     */
    private final CotSender cotSender;
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;

    public Xxx23PlainAndSender(Rpc senderRpc, Party receiverParty, Xxx23PlainAndConfig config) {
        super(Xxx23PlainAndPtoDesc.getInstance(), senderRpc, receiverParty, config);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPtos(cotSender);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, config.getCotConfig());
        addSubPtos(cotReceiver);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum);
        cotReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector and(BitVector x) throws MpcAbortException {
        setPtoInput(x);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // cot
        stopWatch.start();
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        // send payload
        stopWatch.start();
        SquareZ2Vector r = t0t1(cotSenderOutput);
        stopWatch.stop();
        long delta0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, delta0Time);
        logPhaseInfo(PtoState.PTO_END);

        return r;
    }

    private SquareZ2Vector t0t1(CotSenderOutput cotSenderOutput) {
        // generate random x0 and x1
        BitVector x0 = BitVectorFactory.createRandom(num, secureRandom);
        BitVector x1 = input.xor(x0);
        // t0 t1
        BitVector t0 = BitVectorFactory.createZeros(num);
        BitVector t1 = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> t0.set(i, x0.get(i) ^ BinaryUtils.getBoolean(cotSenderOutput.getR0(i), 0)));
        IntStream.range(0, num).forEach(i -> t1.set(i, x1.get(i) ^ BinaryUtils.getBoolean(cotSenderOutput.getR1(i), 0)));

        List<byte[]> t0t1Payload = Arrays.asList(t0.getBytes(), t1.getBytes());
        // sends t0 and t1
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOADS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(t0t1Header, t0t1Payload));
        // x0 as output
        return SquareZ2Vector.create(x0, false);
    }
}
