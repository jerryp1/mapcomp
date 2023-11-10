package edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.AbstractB2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15.Dsz15B2aPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * DSZ15 B2a Receiver.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public class Dsz15B2aReceiver extends AbstractB2aParty {
    /**
     * COT receiver.
     */
    private final CotReceiver cotReceiver;

    public Dsz15B2aReceiver(Rpc rpc, Party otherParty, Dsz15B2aConfig config) {
        super(Dsz15B2aPtoDesc.getInstance(), rpc, otherParty, config);
        cotReceiver = CotFactory.createReceiver(rpc, otherParty, config.getCotConfig());
        addSubPtos(cotReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(maxNum * maxL);
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
        BitVector mergeBitVector = BitVectorFactory.merge(Arrays.stream(input).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        byte[] x0Bytes = mergeBitVector.getBytes();
        boolean[] x0Binary = BinaryUtils.byteArrayToBinary(x0Bytes, num * l);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x0Binary);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);

        // receive payload and decrypt
        stopWatch.start();
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOADS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> t0t1Payload = rpc.receive(t0t1Header).getPayload();
        SquareZlVector result = t0t1(cotReceiverOutput, t0t1Payload);

        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    private SquareZlVector t0t1(CotReceiverOutput cotReceiverOutput, List<byte[]> t0t1Payload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == num * l * 2);
        byte[][] t0s = t0t1Payload.subList(0, num * l).toArray(new byte[0][]);
        byte[][] t1s = t0t1Payload.subList(num * l, num * l * 2).toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // Let P0's output be a0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        BigInteger[] a0s = t0IntStream
            .mapToObj(i ->
                IntStream.range(0, l).mapToObj(j -> {
                    boolean x0 = cotReceiverOutput.getChoice(j * num + i);
                    byte[] a0 = prg.extendToBytes(cotReceiverOutput.getRb(j * num + i));
                    if (!x0) {
                        BytesUtils.xori(a0, t0s[j * num + i]);
                    } else {
                        BytesUtils.xori(a0, t1s[j * num + i]);
                    }
                    return a0;
                }).map(BigIntegerUtils::byteArrayToNonNegBigInteger)
                    .reduce(BigInteger.ZERO, (a, b) -> zl.add(a, b))
            ).toArray(BigInteger[]::new);
        ZlVector z0ZlVector = ZlVector.create(zl, a0s);
        return SquareZlVector.create(z0ZlVector, false);
    }
}
