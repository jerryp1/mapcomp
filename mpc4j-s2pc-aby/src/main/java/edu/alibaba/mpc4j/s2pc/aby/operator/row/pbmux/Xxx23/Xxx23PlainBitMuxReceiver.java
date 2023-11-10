package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.Xxx23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.AbstractPlainBitMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.Xxx23.Xxx23PlainBitMuxPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Plain bit mux receiver.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public class Xxx23PlainBitMuxReceiver extends AbstractPlainBitMuxParty {
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * COT sender
     */
    private final CotSender cotSender;

    public Xxx23PlainBitMuxReceiver(Rpc receiverRpc, Party senderParty, Xxx23PlainBitMuxConfig config) {
        super(Xxx23PlainBitMuxPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPtos(cotReceiver);
        cotSender = CotFactory.createSender(receiverRpc, senderParty, config.getCotConfig());
        addSubPtos(cotSender);
        zl = config.getZl();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(maxNum);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector mux(BitVector x1, SquareZlVector y1) throws MpcAbortException {
        assert x1 != null;
        setPtoInput(x1, y1);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // cot
        stopWatch.start();
        byte[] x0Bytes = x1.getBytes();
        boolean[] x0Binary = BinaryUtils.byteArrayToBinary(x0Bytes, num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x0Binary);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        // receive payload and decrypt
        stopWatch.start();
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOADS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> t0t1Payload = rpc.receive(t0t1Header).getPayload();
        SquareZlVector result = t0t1(cotReceiverOutput, t0t1Payload);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    @Override
    public SquareZ2Vector[] mux(BitVector xi, SquareZ2Vector[] yi) throws MpcAbortException {
        assert xi != null;
        setPtoInput(xi, yi);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // cot
        stopWatch.start();
        byte[] x0Bytes = xi.getBytes();
        boolean[] x0Binary = BinaryUtils.byteArrayToBinary(x0Bytes, num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x0Binary);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        // receive payload and decrypt
        stopWatch.start();
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOADS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> t0t1Payload = rpc.receive(t0t1Header).getPayload();
        SquareZ2Vector[] result = t0t1ForZ2(cotReceiverOutput, t0t1Payload);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    private SquareZlVector t0t1(CotReceiverOutput cotReceiverOutput, List<byte[]> t0t1Payload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == num * 2);
        byte[][] t0s = t0t1Payload.subList(0, num).toArray(new byte[0][]);
        byte[][] t1s = t0t1Payload.subList(num, num * 2).toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // Let P0's output be a0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        BigInteger[] a0s = t0IntStream
            .mapToObj(index -> {
                boolean x0 = cotReceiverOutput.getChoice(index);
                byte[] a0 = prg.extendToBytes(cotReceiverOutput.getRb(index));
                if (!x0) {
                    BytesUtils.xori(a0, t0s[index]);
                    return BigIntegerUtils.byteArrayToNonNegBigInteger(a0);
                } else {
                    BytesUtils.xori(a0, t1s[index]);
                    return zl.add(BigIntegerUtils.byteArrayToNonNegBigInteger(a0), inputZlValues.getZlVector().getElement(index));
                }
            })
            .toArray(BigInteger[]::new);
        ZlVector z0ZlVector = ZlVector.create(zl, a0s);
        return SquareZlVector.create(z0ZlVector, false);
    }

    private SquareZ2Vector[] t0t1ForZ2(CotReceiverOutput cotReceiverOutput, List<byte[]> t0t1Payload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == num * 2);
        byte[][] t0s = t0t1Payload.subList(0, num).toArray(new byte[0][]);
        byte[][] t1s = t0t1Payload.subList(num, num * 2).toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // Let P0's output be a0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        byte[][] a0s = t0IntStream
            .mapToObj(index -> {
                boolean x0 = cotReceiverOutput.getChoice(index);
                byte[] a0 = prg.extendToBytes(cotReceiverOutput.getRb(index));
                if (!x0) {
                    BytesUtils.xori(a0, t0s[index]);
                } else {
                    BytesUtils.xori(a0, t1s[index]);
                    BytesUtils.xori(a0, inputZ2Values[index].getBitVector().getBytes());
                }
                return a0;
            })
            .toArray(byte[][]::new);
        BitVector[] bitVectors = ZlDatabase.create(bitL, a0s).bitPartition(envType, parallel);
        return Arrays.stream(bitVectors).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
    }
}
