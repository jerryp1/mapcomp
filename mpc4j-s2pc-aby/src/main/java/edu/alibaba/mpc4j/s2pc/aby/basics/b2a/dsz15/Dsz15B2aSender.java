package edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15;

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
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.AbstractB2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15.Dsz15B2aPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DSZ15 B2a Sender.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public class Dsz15B2aSender extends AbstractB2aParty {
    /**
     * COT sender.
     */
    private final CotSender cotSender;

    public Dsz15B2aSender(Rpc rpc, Party otherParty, Dsz15B2aConfig config) {
        super(Dsz15B2aPtoDesc.getInstance(), rpc, otherParty, config);
        cotSender = CotFactory.createSender(rpc, otherParty, config.getCotConfig());
        addSubPtos(cotSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum * maxL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector b2a(SquareZ2Vector[] xi) throws MpcAbortException {
        setPtoInput(xi);

        logPhaseInfo(PtoState.PTO_BEGIN);

        // cot
        stopWatch.start();
        CotSenderOutput cotSenderOutput = cotSender.send(num * l);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);

        // ot payload
        stopWatch.start();
        SquareZlVector r = t0t1(cotSenderOutput);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return r;
    }


    private SquareZlVector t0t1(CotSenderOutput cotSenderOutput) {
        Prg prg = PrgFactory.createInstance(envType, byteL);
        BitVector[] inputBits = Arrays.stream(input).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new);
        // generate random rs
        BigInteger[] rs = ZlVector.createRandom(zl, num * l, secureRandom).getElements();
        // vertical
        byte[][] r0s = IntStream.range(0, num * l)
            .mapToObj(i -> zl.sub(inputBits[i / num].get(i % num) ? BigInteger.ONE.shiftLeft(l - i / num - 1) : BigInteger.ZERO, rs[i]))
            .map(r -> BigIntegerUtils.nonNegBigIntegerToByteArray(r, byteL))
            .toArray(byte[][]::new);
        byte[][] r1s = IntStream.range(0, num * l)
            .mapToObj(i -> zl.sub(inputBits[i / num].get(i % num) ? BigInteger.ZERO : BigInteger.ONE.shiftLeft(l - i / num - 1), rs[i]))
            .map(r -> BigIntegerUtils.nonNegBigIntegerToByteArray(r, byteL))
            .toArray(byte[][]::new);
        // P1 creates t0
        IntStream t0IntStream = IntStream.range(0, num * l);
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
        IntStream t1IntStream = IntStream.range(0, num * l);
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
        // sum(r) as output
        BigInteger[] output = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            int finalI = i;
            output[i] = IntStream.range(0, l).mapToObj(j -> rs[j * num + finalI]).reduce(BigInteger.ZERO, (a, b) -> zl.add(a, b));
        }
        return SquareZlVector.create(zl, output, false);
    }
}
