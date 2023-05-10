package edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.bea91;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.AbstractSquareZlParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.bea91.Bea91SquareZlPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Bea91 Zl circuit sender.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class Bea91SquareZlSender extends AbstractSquareZlParty {
    /**
     * multiplication triple generator
     */
    private final ZlMtgParty mtgSender;

    public Bea91SquareZlSender(Rpc senderRpc, Party receiverParty, Bea91SquareZlConfig config) {
        super(Bea91SquareZlPtoDesc.getInstance(), senderRpc, receiverParty, config);
        mtgSender = ZlMtgFactory.createSender(senderRpc, receiverParty, config.getMtgConfig());
        addSubPtos(mtgSender);
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mtgSender.init(maxRoundNum, updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector shareOwn(ZlVector x0) {
        setShareOwnInput(x0);
        logPhaseInfo(PtoState.PTO_BEGIN, "send share");

        stopWatch.start();
        ZlVector x0Vector = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector x1Vector = x0.sub(x0Vector);
        List<byte[]> x1Payload = Arrays.stream(x1Vector.getElements())
            .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
            .collect(Collectors.toList());
        DataPacketHeader x1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(x1Header, x1Payload));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "send share");

        logPhaseInfo(PtoState.PTO_END, "send share");
        return SquareZlVector.create(x0Vector, false);
    }

    @Override
    public SquareZlVector shareOther(int num) throws MpcAbortException {
        setShareOtherInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

        stopWatch.start();
        DataPacketHeader x0Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> x0Payload = rpc.receive(x0Header).getPayload();
        MpcAbortPreconditions.checkArgument(x0Payload.size() == num);
        BigInteger[] x0Array = x0Payload.stream()
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        ZlVector x0Vector = ZlVector.create(zl, x0Array);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, shareTime, "receive share");

        logPhaseInfo(PtoState.PTO_END, "receive share");
        return SquareZlVector.create(x0Vector, false);
    }

    @Override
    public ZlVector revealOwn(MpcZlVector x0) throws MpcAbortException {
        SquareZlVector squareX0 = (SquareZlVector) x0;
        setRevealOwnInput(squareX0);
        if (x0.isPlain()) {
            return x0.getZlVector();
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

            stopWatch.start();
            DataPacketHeader x1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_OUTPUT_SHARE.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> x1Payload = rpc.receive(x1Header).getPayload();
            MpcAbortPreconditions.checkArgument(x1Payload.size() == num);
            BigInteger[] x1Array = x1Payload.stream()
                .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
                .toArray(BigInteger[]::new);
            ZlVector x0Vector = x0.getZlVector();
            ZlVector x1Vector = ZlVector.create(zl, x1Array);
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "receive share");

            logPhaseInfo(PtoState.PTO_END, "receive share");
            return x0Vector.add(x1Vector);
        }
    }

    @Override
    public void revealOther(MpcZlVector x0) {
        SquareZlVector squareX0 = (SquareZlVector) x0;
        setRevealOtherInput(squareX0);
        if (!x0.isPlain()) {
            logPhaseInfo(PtoState.PTO_BEGIN, "send share");

            stopWatch.start();
            List<byte[]> x0Payload = Arrays.stream(x0.getZlVector().getElements())
                .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
                .collect(Collectors.toList());
            DataPacketHeader x0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(x0Header, x0Payload));
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "send share");

            logPhaseInfo(PtoState.PTO_END, "send share");
        }
    }

    @Override
    public SquareZlVector add(MpcZlVector x0, MpcZlVector y0) {
        SquareZlVector squareX0 = (SquareZlVector) x0;
        SquareZlVector squareY0 = (SquareZlVector) y0;
        setDyadicOperatorInput(squareX0, squareY0);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain vector, using plain add.
            ZlVector z0Vector = x0.getZlVector().add(y0.getZlVector());
            return SquareZlVector.create(z0Vector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain vector, the sender does plain add.
            ZlVector z0Vector = x0.getZlVector().add(y0.getZlVector());
            return SquareZlVector.create(z0Vector, false);
        } else {
            // x0 and y0 are secret vector, using secret add.
            logPhaseInfo(PtoState.PTO_BEGIN, "add");

            stopWatch.start();
            ZlVector z0Vector = x0.getZlVector().add(y0.getZlVector());
            SquareZlVector squareZ0 = SquareZlVector.create(z0Vector, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z0Time, "add (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "add");
            return squareZ0;
        }
    }

    @Override
    public SquareZlVector sub(MpcZlVector x0, MpcZlVector y0) {
        SquareZlVector squareX0 = (SquareZlVector) x0;
        SquareZlVector squareY0 = (SquareZlVector) y0;
        setDyadicOperatorInput(squareX0, squareY0);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain vector, using plain sub.
            ZlVector z0Vector = x0.getZlVector().sub(y0.getZlVector());
            return SquareZlVector.create(z0Vector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain vector, the sender does plain sub.
            ZlVector z0Vector = x0.getZlVector().sub(y0.getZlVector());
            return SquareZlVector.create(z0Vector, false);
        } else {
            // x0 and y0 are secret vector, using secret sub.
            logPhaseInfo(PtoState.PTO_BEGIN, "sub");

            stopWatch.start();
            ZlVector z0Vector = x0.getZlVector().sub(y0.getZlVector());
            SquareZlVector squareZ0 = SquareZlVector.create(z0Vector, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, z0Time, "sub (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "sub");
            return squareZ0;
        }
    }

    @Override
    public SquareZlVector mul(MpcZlVector x0, MpcZlVector y0) throws MpcAbortException {
        SquareZlVector squareX0 = (SquareZlVector) x0;
        SquareZlVector squareY0 = (SquareZlVector) y0;
        setDyadicOperatorInput(squareX0, squareY0);
        if (x0.isPlain() && y0.isPlain()) {
            // x0 and y0 are plain vector, using plain mul.
            ZlVector z0Vector = x0.getZlVector().mul(y0.getZlVector());
            return SquareZlVector.create(z0Vector, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0 or y0 is plain vector, using plain mul.
            ZlVector z0Vector = x0.getZlVector().mul(y0.getZlVector());
            return SquareZlVector.create(z0Vector, false);
        } else {
            // x0 and y0 are secret vector, using secret mul.
            logPhaseInfo(PtoState.PTO_BEGIN, "mul");

            stopWatch.start();
            ZlTriple triple = mtgSender.generate(num);
            stopWatch.stop();
            long mtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, mtgTime, "mul (gen. triples)");

            // compute e0 and f0
            stopWatch.start();
            ZlVector a0 = ZlVector.create(zl, triple.getA());
            ZlVector b0 = ZlVector.create(zl, triple.getB());
            ZlVector c0 = ZlVector.create(zl, triple.getC());
            // e0 = x0 + a0
            ZlVector e0 = x0.getZlVector().add(a0);
            // f0 = y0 ⊕ b0
            ZlVector f0 = y0.getZlVector().add(b0);
            List<byte[]> e0f0Payload = Arrays.stream(e0.getElements())
                .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
                .collect(Collectors.toList());
            List<byte[]> f0Payload = Arrays.stream(f0.getElements())
                .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
                .collect(Collectors.toList());
            e0f0Payload.addAll(f0Payload);
            DataPacketHeader e0f0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_E0_F0.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(e0f0Header, e0f0Payload));
            stopWatch.stop();
            long e0f0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, e0f0Time, "mul (open e/f)");

            stopWatch.start();
            DataPacketHeader e1f1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_E1_F1.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> e1f1Payload = rpc.receive(e1f1Header).getPayload();
            MpcAbortPreconditions.checkArgument(e1f1Payload.size() == 2 * num);
            BigInteger[] e1f1 = e1f1Payload.stream()
                .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
                .toArray(BigInteger[]::new);
            BigInteger[] e1 = new BigInteger[num];
            System.arraycopy(e1f1, 0, e1, 0, num);
            BigInteger[] f1 = new BigInteger[num];
            System.arraycopy(e1f1, num, f1, 0, num);
            // e = (e0 + e1)
            ZlVector z0 = e0.add(ZlVector.create(zl, e1));
            // f = (f0 ⊕ f1)
            ZlVector f = f0.add(ZlVector.create(zl, f1));
            // z0 = (e * b0) + (f * a0) + c0
            z0 = z0.mul(b0);
            f = f.mul(a0);
            z0 = z0.add(f);
            z0 = z0.add(c0);
            SquareZlVector squareZ0 = SquareZlVector.create(zl, z0.getElements(), false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z0Time, "mul (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "mul");
            return squareZ0;
        }
    }
}
