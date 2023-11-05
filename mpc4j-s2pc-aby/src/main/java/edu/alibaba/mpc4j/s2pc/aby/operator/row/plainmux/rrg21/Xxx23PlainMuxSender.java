package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux.rrg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux.AbstractPlainMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux.rrg21.Xxx23PlainMuxPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Plain mux sender.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public class Xxx23PlainMuxSender extends AbstractPlainMuxParty {
    /**
     * COT sender
     */
    private final CotSender cotSender;
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * -s0 vector
     */
    private ZlVector negS0ZlVector;
    /**
     * t0 vector
     */
    private ZlVector t0ZlVector;

    public Xxx23PlainMuxSender(Rpc senderRpc, Party receiverParty, Xxx23PlainMuxConfig config) {
        super(Xxx23PlainMuxPtoDesc.getInstance(), senderRpc, receiverParty, config);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPtos(cotSender);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, config.getCotConfig());
        addSubPtos(cotReceiver);
        zl = config.getZl();
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
    public SquareZlVector mux(SquareZ2Vector x0, long[] y0) throws MpcAbortException {
        assert y0 != null;
        setPtoInput(x0, y0);
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
        SquareZlVector r = t0t1(cotSenderOutput);
        stopWatch.stop();
        long delta0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, delta0Time);
        logPhaseInfo(PtoState.PTO_END);

        return r;
    }

    private SquareZlVector t0t1(CotSenderOutput cotSenderOutput) {
        Prg prg = PrgFactory.createInstance(envType, byteL);
        BitVector inputBits = this.inputBits.getBitVector();
        // generate random rs
        BigInteger[] rs = ZlVector.createRandom(zl, num, secureRandom).getElements();
        byte[][] r0s = IntStream.range(0, num)
            .mapToObj(i -> zl.add(rs[i], inputBits.get(i) ? BigInteger.valueOf(inputs[i]) : BigInteger.ZERO))
            .map(r -> BigIntegerUtils.nonNegBigIntegerToByteArray(r, byteL))
            .toArray(byte[][]::new);
        byte[][] r1s = IntStream.range(0, num)
            .mapToObj(i -> zl.add(rs[i], inputBits.get(i) ? BigInteger.ZERO : BigInteger.valueOf(inputs[i])))
            .map(r -> BigIntegerUtils.nonNegBigIntegerToByteArray(r, byteL))
            .toArray(byte[][]::new);
        // P1 creates t0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        List<byte[]> t0t1Payload = t0IntStream
            .mapToObj(index -> {
                // key0
                byte[] t0 = prg.extendToBytes(cotSenderOutput.getR0(index));
                BytesUtils.xori(t0, r0s[index]);
                return t0;
            })
            .collect(Collectors.toList());
        // P1 creates t1
        IntStream t1IntStream = IntStream.range(0, num);
        t1IntStream = parallel ? t1IntStream.parallel() : t1IntStream;
        List<byte[]> t1Payload = t1IntStream
            .mapToObj(index -> {
                // key1
                byte[] t1 = prg.extendToBytes(cotSenderOutput.getR1(index));
                BytesUtils.xori(t1, r1s[index]);
                return t1;
            })
            .collect(Collectors.toList());
        // merge t0 and t1
        t0t1Payload.addAll(t1Payload);
        // sends s0 and s1
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOADS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(t0t1Header, t0t1Payload));
        // -r as output
        return SquareZlVector.create(zl, Arrays.stream(rs).map(r -> zl.neg(r)).toArray(BigInteger[]::new), false);
    }
}
