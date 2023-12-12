package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.rrk20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.AbstractZ2MuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.rrk20.Rrk20Z2MuxPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Rrk20Z2MuxSender extends AbstractZ2MuxParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rrk20Z2MuxSender.class);

    /**
     * COT sender
     */
    private final CotSender cotSender;
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * R0 vector
     */
    private BitVector[] r0Z2Vectors;
    /**
     * s0
     */
    private byte[][] s0s;
    /**
     * s1
     */
    private byte[][] s1s;

    public Rrk20Z2MuxSender(Rpc senderRpc, Party receiverParty, Rrk20Z2MuxConfig config) {
        super(Rrk20Z2MuxPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
    public SquareZ2Vector[] mux(SquareZ2Vector x0, SquareZ2Vector[] y0) throws MpcAbortException {
        setPtoInput(x0, y0);
        logPhaseInfo(PtoState.PTO_BEGIN);
//        LOGGER.info("mux step0");

        stopWatch.start();
        prepare(x0, y0);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, prepareTime);
//        LOGGER.info("mux step1");
        stopWatch.start();
        // P0 invokes an instance of COT, where P0 is the sender with inputs (s0, s1).
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        // P0 invokes an instance of COT, where P0 is the receiver with inputs x0.
//        LOGGER.info("mux step1.5");
        byte[] x0Bytes = x0.getBitVector().getBytes();
        boolean[] x0Binary = BinaryUtils.byteArrayToBinary(x0Bytes, num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x0Binary);
        stopWatch.stop();
//        LOGGER.info("mux step2");

        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, cotTime);

        stopWatch.start();
        s0s1(cotSenderOutput);
        s0s = null;
        s1s = null;
        stopWatch.stop();
        long s0s1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, s0s1Time);
//        LOGGER.info("mux step3");

        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_T0_T1.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> t0t1Payload = rpc.receive(t0t1Header).getPayload();
//        LOGGER.info("mux step4");

        stopWatch.start();
        SquareZ2Vector[] z0 = t0t1(cotReceiverOutput, t0t1Payload);
        r0Z2Vectors = null;
        stopWatch.stop();
        long t0t1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, t0t1Time);
//        LOGGER.info("mux step5");

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }

    private void prepare(SquareZ2Vector f, SquareZ2Vector[] xi) {
        // P0 picks r0 ∈ Zn
        r0Z2Vectors = IntStream.range(0, xi.length).mapToObj(i ->
            BitVectorFactory.createRandom(num, secureRandom)).toArray(BitVector[]::new);
        byte[][] valueByte = ZlDatabase.create(envType, parallel, r0Z2Vectors).getBytesData();
        // if x0 = 0, P0 sets (s0, s1) = (-r0, -r0 + y0), else, P0 sets (s0, s1) = (-r0 + y0, -r0).
//        LOGGER.info("mux sub step0");

        BitVector x0BitVector = f.getBitVector();
        byte[][] originData = ZlDatabase.create(envType, parallel, Arrays.stream(xi)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new)).getBytesData();
        s0s = new byte[num][];
        s1s = new byte[num][];
//        LOGGER.info("mux sub step1");
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> {
            boolean x = x0BitVector.get(index);
            if (!x) {
                s0s[index] = valueByte[index];
                s1s[index] = BytesUtils.xor(valueByte[index], originData[index]);
            } else {
                s0s[index] = BytesUtils.xor(valueByte[index], originData[index]);
                s1s[index] = valueByte[index];
            }
        });
    }

    private void s0s1(CotSenderOutput cotSenderOutput) {
        Prg prg = PrgFactory.createInstance(envType, byteL);
        byte andNum = (byte) (bitLen % 8 == 0 ? 255 : (1 << (bitLen & 7)) - 1);
        // P0 creates s0
        IntStream s0IntStream = IntStream.range(0, num);
        s0IntStream = parallel ? s0IntStream.parallel() : s0IntStream;
        List<byte[]> s0s1Payload = s0IntStream
            .mapToObj(index -> {
                byte[] s0 = prg.extendToBytes(cotSenderOutput.getR0(index));
                s0[0] &= andNum;
                BytesUtils.xori(s0, s0s[index]);
                return s0;
            })
            .collect(Collectors.toList());
        // P0 creates s1
//        LOGGER.info("mux sub step2");

        IntStream s1IntStream = IntStream.range(0, num);
        s1IntStream = parallel ? s1IntStream.parallel() : s1IntStream;
        List<byte[]> s1Payload = s1IntStream
            .mapToObj(index -> {
                byte[] s1 = prg.extendToBytes(cotSenderOutput.getR1(index));
                s1[0] &= andNum;
                BytesUtils.xori(s1, s1s[index]);
                return s1;
            })
            .collect(Collectors.toList());
        // merge s0 and s1
        s0s1Payload.addAll(s1Payload);
        // sends s0 and s1
        DataPacketHeader s0s1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_S0_S1.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
//        LOGGER.info("mux sub step3");

        rpc.send(DataPacket.fromByteArrayList(s0s1Header, s0s1Payload));
    }

    private SquareZ2Vector[] t0t1(CotReceiverOutput cotReceiverOutput, List<byte[]> t0t1Payload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == num * 2);
        byte andNum = (byte) (bitLen % 8 == 0 ? 255 : (1 << (bitLen & 7)) - 1);
        byte[][] t0s = t0t1Payload.subList(0, num).toArray(new byte[0][]);
        byte[][] t1s = t0t1Payload.subList(num, num * 2).toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // Let P0's output be a0
        IntStream t0IntStream = IntStream.range(0, num);
//        LOGGER.info("mux sub step4");

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
        BitVector[] tmp = ZlDatabase.create(bitLen, a0s).bitPartition(envType, parallel);
//        LOGGER.info("mux sub step5");

        for(int i = 0; i < tmp.length; i++){
            tmp[i].xori(r0Z2Vectors[i]);
        }
        return Arrays.stream(tmp).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
    }
}
