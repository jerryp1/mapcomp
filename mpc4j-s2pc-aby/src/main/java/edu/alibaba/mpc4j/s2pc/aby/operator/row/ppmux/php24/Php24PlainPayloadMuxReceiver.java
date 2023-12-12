package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.php24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.AbstractPlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.php24.Php24PlainPayloadMuxPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Plain mux receiver.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public class Php24PlainPayloadMuxReceiver extends AbstractPlainPayloadMuxParty {
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * COT sender
     */
    private final CotSender cotSender;

    public Php24PlainPayloadMuxReceiver(Rpc receiverRpc, Party senderParty, Php24PlainPayloadMuxConfig config) {
        super(Php24PlainPayloadMuxPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        cotSender = CotFactory.createSender(receiverRpc, senderParty, config.getCotConfig());
        addMultipleSubPtos(cotReceiver, cotSender);
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
    public SquareZlVector mux(SquareZ2Vector x1, long[] y1, int validBitLen) throws MpcAbortException {
        assert y1 == null;
        setPtoInput(x1, y1, validBitLen);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // cot
        stopWatch.start();
        byte[] x0Bytes = x1.getBitVector().getBytes();
        boolean[] x0Binary = BinaryUtils.byteArrayToBinary(x0Bytes, num);
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

    @Override
    public SquareZ2Vector[] muxB(SquareZ2Vector xi, BitVector[] yi, int validBitLen) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        setPtoInput(xi, yi, validBitLen);
        SquareZ2Vector[] result;

        if (yi != null) {
            // sender
            // cot
            stopWatch.start();
            CotSenderOutput cotSenderOutput = cotSender.send(num);
            logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

            // send payload
            stopWatch.start();
            result = t0t1BinarySender(cotSenderOutput, yi);
            logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());
        } else {
            // receiver
            stopWatch.start();
            // cot
            byte[] x0Bytes = xi.getBitVector().getBytes();
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
            result = t0t1BinaryReceiver(cotReceiverOutput, t0t1Payload);

            logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());
        }
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
                } else {
                    BytesUtils.xori(a0, t1s[index]);
                }
                return a0;
            })
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        ZlVector z0ZlVector = ZlVector.create(zl, a0s);
        return SquareZlVector.create(z0ZlVector, false);
    }

    private SquareZ2Vector[] t0t1BinaryReceiver(CotReceiverOutput cotReceiverOutput, List<byte[]> t0t1Payload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == num * 2);
        byte[][] t0s = t0t1Payload.subList(0, num).toArray(new byte[0][]);
        byte[][] t1s = t0t1Payload.subList(num, num * 2).toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // Let P0's output be a0
        byte andNum = (byte) (zl.getL() == 8 ? 255 : (1 << (zl.getL() & 7)) - 1);
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        byte[][] a0s = t0IntStream
            .mapToObj(index -> {
                boolean x0 = cotReceiverOutput.getChoice(index);
                byte[] a0 = prg.extendToBytes(cotReceiverOutput.getRb(index));
                a0[0] &= andNum;
                if (!x0) {
                    BytesUtils.xori(a0, t0s[index]);
                } else {
                    BytesUtils.xori(a0, t1s[index]);
                }
                return a0;
            })
            .toArray(byte[][]::new);
        BitVector[] tmp = ZlDatabase.create(zl.getL(), a0s).bitPartition(envType, parallel);
        return Arrays.stream(tmp).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
    }

    private SquareZ2Vector[] t0t1BinarySender(CotSenderOutput cotSenderOutput, BitVector[] data) {
        byte[][] inputPayloads = ZlDatabase.create(envType, parallel, data).getBytesData();
        Prg prg = PrgFactory.createInstance(envType, byteL);
        BitVector inputBits = this.inputBits.getBitVector();

        // generate random rs
        byte andNum = (byte) (data.length == 8 ? 255 : (1 << (data.length & 7)) - 1);
        BitVector[] resBits = IntStream.range(0, data.length).mapToObj(i -> BitVectorFactory.createRandom(num, secureRandom)).toArray(BitVector[]::new);
        byte[][] rs = ZlDatabase.create(envType, parallel, resBits).getBytesData();
        byte[][] r0s = IntStream.range(0, num)
            .mapToObj(i -> inputBits.get(i) ? BytesUtils.xor(rs[i], inputPayloads[i]) : rs[i])
            .toArray(byte[][]::new);
        byte[][] r1s = IntStream.range(0, num)
            .mapToObj(i -> inputBits.get(i) ? rs[i] : BytesUtils.xor(rs[i], inputPayloads[i]))
            .toArray(byte[][]::new);
        // P1 creates t0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        List<byte[]> t0t1Payload = t0IntStream
            .mapToObj(index -> {
                // key0
                byte[] t0 = prg.extendToBytes(cotSenderOutput.getR0(index));
                t0[0] &= andNum;
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
                t1[0] &= andNum;
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
        return Arrays.stream(resBits).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
    }

}
