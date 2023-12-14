package edu.alibaba.mpc4j.s2pc.aby.basics.zl.bea91;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.AbstractZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.bea91.Bea91ZlcPtoDesc.PtoStep;
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
public class Bea91ZlcSender extends AbstractZlcParty {
    /**
     * multiplication triple generator
     */
    private final ZlMtgParty mtgSender;

    public Bea91ZlcSender(Rpc senderRpc, Party receiverParty, Bea91ZlcConfig config) {
        super(Bea91ZlcPtoDesc.getInstance(), senderRpc, receiverParty, config);
        mtgSender = ZlMtgFactory.createSender(senderRpc, receiverParty, config.getMtgConfig());
        addSubPtos(mtgSender);
    }

    public Bea91ZlcSender(Rpc senderRpc, Party receiverParty, Party aiderParty, Bea91ZlcConfig config) {
        super(Bea91ZlcPtoDesc.getInstance(), senderRpc, receiverParty, config);
        mtgSender = ZlMtgFactory.createSender(senderRpc, receiverParty, aiderParty, config.getMtgConfig());
        addSubPtos(mtgSender);
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public void init(int updateNum) throws MpcAbortException {
        setInitInput(updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mtgSender.init(updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector setPublicValue(ZlVector xi){
        ZlVector zlVector = ZlVector.createZeros(xi.getZl(), xi.getNum());
        return SquareZlVector.create(zlVector, false);
    }

    @Override
    public ZlVector revealOwn(MpcZlVector x0) throws MpcAbortException {
        SquareZlVector x0SquareVector = (SquareZlVector) x0;
        setRevealOwnInput(x0SquareVector);
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
            extraInfo++;
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
        SquareZlVector x0SquareVector = (SquareZlVector) x0;
        setRevealOtherInput(x0SquareVector);
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
            extraInfo++;
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "send share");

            logPhaseInfo(PtoState.PTO_END, "send share");
        }
    }

    @Override
    public SquareZlVector add(MpcZlVector x0, MpcZlVector y0) {
        SquareZlVector x0SquareVector = (SquareZlVector) x0;
        SquareZlVector y0SquareVector = (SquareZlVector) y0;
        setDyadicOperatorInput(x0SquareVector, y0SquareVector);
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
            return SquareZlVector.create(x0.getZlVector().add(y0.getZlVector()), false);
        }
    }

    @Override
    public SquareZlVector sub(MpcZlVector x0, MpcZlVector y0) {
        SquareZlVector x0SquareVector = (SquareZlVector) x0;
        SquareZlVector y0SquareVector = (SquareZlVector) y0;
        setDyadicOperatorInput(x0SquareVector, y0SquareVector);
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
            return SquareZlVector.create(x0.getZlVector().sub(y0.getZlVector()), false);
        }
    }

    @Override
    public SquareZlVector mul(MpcZlVector x0, MpcZlVector y0) throws MpcAbortException {
        SquareZlVector x0SquareVector = (SquareZlVector) x0;
        SquareZlVector y0SquareVector = (SquareZlVector) y0;
        setDyadicOperatorInput(x0SquareVector, y0SquareVector);
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
            // e0 = x0 - a0
            ZlVector e0 = x0.getZlVector().sub(a0);
            // f0 = y0 - b0
            ZlVector f0 = y0.getZlVector().sub(b0);
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
            // f = (f0 + f1)
            ZlVector f = f0.add(ZlVector.create(zl, f1));
            // z0 = (e * b0) + (f * a0) + c0
            z0.muli(b0);
            f.muli(a0);
            z0.addi(f);
            z0.addi(c0);
            SquareZlVector z0SquareVector = SquareZlVector.create(zl, z0.getElements(), false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z0Time, "mul (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "mul");
            return z0SquareVector;
        }
    }

    @Override
    public SquareZlVector rowAdderWithPrefix(SquareZlVector x, SquareZlVector prefix){
        assert !x.isPlain();
        MathPreconditions.checkEqual("data of prefixValue", "1", prefix.getNum(), 1);
        Zl zl = x.getZl();
        BigInteger[] xValues = x.getZlVector().getElements();
        BigInteger[] res = new BigInteger[xValues.length];
        res[0] = zl.add(xValues[0], prefix.getZlVector().getElement(0));
        for(int i = 1; i < xValues.length; i++){
            res[i] = zl.add(res[i - 1], xValues[i]);
        }
        return SquareZlVector.create(zl, res, x.isPlain());
    }
}
