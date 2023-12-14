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
 * Bea91 Zl circuit receiver.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
public class Bea91ZlcReceiver extends AbstractZlcParty {
    /**
     * multiplication triple generation receiver
     */
    private final ZlMtgParty mtgReceiver;

    public Bea91ZlcReceiver(Rpc receiverRpc, Party senderParty, Bea91ZlcConfig config) {
        super(Bea91ZlcPtoDesc.getInstance(), receiverRpc, senderParty, config);
        mtgReceiver = ZlMtgFactory.createReceiver(receiverRpc, senderParty, config.getMtgConfig());
        addSubPtos(mtgReceiver);
    }

    public Bea91ZlcReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, Bea91ZlcConfig config) {
        super(Bea91ZlcPtoDesc.getInstance(), receiverRpc, senderParty, config);
        mtgReceiver = ZlMtgFactory.createReceiver(receiverRpc, senderParty, aiderParty, config.getMtgConfig());
        addSubPtos(mtgReceiver);
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
        mtgReceiver.init(updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector setPublicValue(ZlVector xi){
        return SquareZlVector.create(xi, false);
    }

    @Override
    public ZlVector revealOwn(MpcZlVector x1) throws MpcAbortException {
        SquareZlVector x1SquareVector = (SquareZlVector) x1;
        setRevealOwnInput(x1SquareVector);
        if (x1.isPlain()) {
            return x1.getZlVector();
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN, "receive share");

            stopWatch.start();
            DataPacketHeader x0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> x0Payload = rpc.receive(x0Header).getPayload();
            extraInfo++;
            MpcAbortPreconditions.checkArgument(x0Payload.size() == num);
            BigInteger[] x0Array = x0Payload.stream()
                .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
                .toArray(BigInteger[]::new);
            ZlVector x0Vector = ZlVector.create(zl, x0Array);
            ZlVector x1Vector = x1.getZlVector();
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "receive share");

            logPhaseInfo(PtoState.PTO_END, "receive share");
            return x0Vector.add(x1Vector);
        }
    }

    @Override
    public void revealOther(MpcZlVector x1) {
        SquareZlVector x1SquareVector = (SquareZlVector) x1;
        setRevealOtherInput(x1SquareVector);
        if (!x1.isPlain()) {
            logPhaseInfo(PtoState.PTO_BEGIN, "send share");

            stopWatch.start();
            List<byte[]> x1Payload = Arrays.stream(x1.getZlVector().getElements())
                .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
                .collect(Collectors.toList());
            DataPacketHeader x1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_OUTPUT_SHARE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(x1Header, x1Payload));
            extraInfo++;
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, revealTime, "send share");

            logPhaseInfo(PtoState.PTO_END, "send share");
        }
    }

    @Override
    public SquareZlVector add(MpcZlVector x1, MpcZlVector y1) {
        SquareZlVector x1SquareVector = (SquareZlVector) x1;
        SquareZlVector y1SquareVector = (SquareZlVector) y1;
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain vector, using plain add.
            ZlVector z1Vector = x1.getZlVector().add(y1.getZlVector());
            return SquareZlVector.create(z1Vector, true);
        } else if (x1.isPlain()) {
            // x1 is plain vector, y1 is secret vector, the receiver copies y1
            return y1SquareVector.copy();
        } else if (y1.isPlain()) {
            // x1 is secret vector, y1 is plain vector, the receiver copies x1
            return x1SquareVector.copy();
        } else {
            // x1 and y1 are secret vectors, using secret add.
            ZlVector z1Vector = x1.getZlVector().add(y1.getZlVector());
            return SquareZlVector.create(x1.getZlVector().add(y1.getZlVector()), false);
        }
    }

    @Override
    public SquareZlVector sub(MpcZlVector x1, MpcZlVector y1) {
        SquareZlVector x1SquareVector = (SquareZlVector) x1;
        SquareZlVector y1SquareVector = (SquareZlVector) y1;
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain vector, using plain sub.
            ZlVector z1Vector = x1.getZlVector().sub(y1.getZlVector());
            return SquareZlVector.create(z1Vector, true);
        } else if (x1.isPlain()) {
            // x1 is plain vector, y1 is secret vector, the receiver computes 0 - y1
            ZlVector z1Vector = ZlVector.createZeros(zl, num).sub(y1.getZlVector());
            return SquareZlVector.create(z1Vector, false);
        } else if (y1.isPlain()) {
            // x1 is secret vector, y1 is plain vector, the receiver copies x1
            return x1SquareVector.copy();
        } else {
            // x1 and y1 are secret vectors, using secret sub.
            return SquareZlVector.create(x1.getZlVector().sub(y1.getZlVector()), false);
        }
    }

    @Override
    public SquareZlVector mul(MpcZlVector x1, MpcZlVector y1) throws MpcAbortException {
        SquareZlVector x1SquareVector = (SquareZlVector) x1;
        SquareZlVector y1SquareVector = (SquareZlVector) y1;
        setDyadicOperatorInput(x1SquareVector, y1SquareVector);

        if (x1.isPlain() && y1.isPlain()) {
            // x1 and y1 are plain vectors, using plain mul.
            ZlVector z1Vector = x1.getZlVector().mul(y1.getZlVector());
            return SquareZlVector.create(z1Vector, true);
        } else if (x1.isPlain() || y1.isPlain()) {
            // x1 or y1 is plain vector, using plain mul.
            ZlVector z1Vector = x1.getZlVector().mul(y1.getZlVector());
            return SquareZlVector.create(z1Vector, false);
        } else {
            // x1 and y1 are secret vectors, using secret mul.
            logPhaseInfo(PtoState.PTO_BEGIN, "mul");

            stopWatch.start();
            ZlTriple triple = mtgReceiver.generate(num);
            stopWatch.stop();
            long mtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 3, mtgTime, "and (gen. triples)");

            stopWatch.start();
            ZlVector a1 = ZlVector.create(zl, triple.getA());
            ZlVector b1 = ZlVector.create(zl, triple.getB());
            ZlVector c1 = ZlVector.create(zl, triple.getC());
            // e1 = x1 - a1
            ZlVector e1 = x1.getZlVector().sub(a1);
            // f1 = y1 - b1
            ZlVector f1 = y1.getZlVector().sub(b1);
            List<byte[]> e1f1Payload = Arrays.stream(e1.getElements())
                .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
                .collect(Collectors.toList());
            List<byte[]> f1Payload = Arrays.stream(f1.getElements())
                .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
                .collect(Collectors.toList());
            e1f1Payload.addAll(f1Payload);
            DataPacketHeader e1f1Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_E1_F1.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(e1f1Header, e1f1Payload));
            stopWatch.stop();
            long e1f1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, e1f1Time, "and (open e/f)");

            stopWatch.start();
            DataPacketHeader e0f0Header = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_E0_F0.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> e0f0Payload = rpc.receive(e0f0Header).getPayload();
            MpcAbortPreconditions.checkArgument(e0f0Payload.size() == 2 * num);
            BigInteger[] e0f0 = e0f0Payload.stream()
                .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
                .toArray(BigInteger[]::new);
            BigInteger[] e0 = new BigInteger[num];
            System.arraycopy(e0f0, 0, e0, 0, num);
            BigInteger[] f0 = new BigInteger[num];
            System.arraycopy(e0f0, num, f0, 0, num);
            // e = (e0 + e1)
            ZlVector z1 = ZlVector.create(zl, e0).add(e1);
            // f = (f0 + f1)
            ZlVector f = ZlVector.create(zl, f0).add(f1);
            // z1 = (e * b1) + (f * a1) + c1 + (e * f)
            ZlVector ef = z1.mul(f);
            z1.muli(b1);
            f.muli(a1);
            z1.addi(f);
            z1.addi(c1);
            z1.addi(ef);
            SquareZlVector z1SquareVector = SquareZlVector.create(z1, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, z1Time, "mul (gen. z)");

            logPhaseInfo(PtoState.PTO_END, "mul");
            return z1SquareVector;
        }
    }

    @Override
    public SquareZlVector rowAdderWithPrefix(SquareZlVector x, SquareZlVector prefix){
        assert !x.isPlain();
        MathPreconditions.checkEqual("data of prefixValue", "1", prefix.getNum(), 1);
        Zl zl = x.getZl();
        BigInteger[] xValues = x.getZlVector().getElements();
        BigInteger[] res = new BigInteger[xValues.length];
        res[0] = prefix.isPlain() ? xValues[0] : zl.add(xValues[0], prefix.getZlVector().getElement(0));
        for(int i = 1; i < xValues.length; i++){
            res[i] = zl.add(res[i - 1], xValues[i]);
        }
        return SquareZlVector.create(zl, res, x.isPlain());
    }
}
