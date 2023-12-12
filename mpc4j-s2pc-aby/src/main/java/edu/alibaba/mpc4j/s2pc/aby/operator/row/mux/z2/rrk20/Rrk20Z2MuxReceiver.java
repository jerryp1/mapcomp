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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RRK+20 Z2 mux receiver.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
public class Rrk20Z2MuxReceiver extends AbstractZ2MuxParty {
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * COT sender
     */
    private final CotSender cotSender;
    private BitVector[] r1z2Vectors;
    /**
     * t0
     */
    private byte[][] t0s;
    /**
     * t1
     */
    private byte[][] t1s;

    public Rrk20Z2MuxReceiver(Rpc receiverRpc, Party senderParty, Rrk20Z2MuxConfig config) {
        super(Rrk20Z2MuxPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPtos(cotReceiver);
        cotSender = CotFactory.createSender(receiverRpc, senderParty, config.getCotConfig());
        addSubPtos(cotSender);
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
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector[] mux(SquareZ2Vector x1, SquareZ2Vector[] y1) throws MpcAbortException {
        setPtoInput(x1, y1);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        prepare(x1, y1);
        logStepInfo(PtoState.PTO_STEP, 1, 4, resetAndGetTime());

        stopWatch.start();
        // P1 invokes an instance of COT, where P1 is the receiver with inputs x1.
        byte[] x1Bytes = x1.getBitVector().getBytes();
        boolean[] x1Binary = BinaryUtils.byteArrayToBinary(x1Bytes, num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(x1Binary);
        // P1 invokes an instance of COT, where P1 is the sender with inputs (t0, t1).
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        logStepInfo(PtoState.PTO_STEP, 2, 4, resetAndGetTime());

        stopWatch.start();
        t0t1(cotSenderOutput);
        t0s = null;
        t1s = null;
        logStepInfo(PtoState.PTO_STEP, 3, 4, resetAndGetTime());

        stopWatch.start();
        DataPacketHeader s0s1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_S0_S1.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> s0s1Payload = rpc.receive(s0s1Header).getPayload();
        SquareZ2Vector[] z1 = s0s1(cotReceiverOutput, s0s1Payload);
        r1z2Vectors = null;
        logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }

    private void prepare(SquareZ2Vector f, SquareZ2Vector[] xi) {
        // P1 picks r1 ∈ Zn
        r1z2Vectors = IntStream.range(0, xi.length).mapToObj(i ->
            BitVectorFactory.createRandom(num, secureRandom)).toArray(BitVector[]::new);
        byte[][] valueByte = ZlDatabase.create(envType, parallel, r1z2Vectors).getBytesData();
        // if x1 = 0, P1 sets (t0, t1) = (-r1, -r1 + y1), else, P1 sets (t0, t1) = (-r1 + y1, -r1).
        byte[][] originData = ZlDatabase.create(envType, parallel, Arrays.stream(xi)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new)).getBytesData();
        BitVector x1BitVector = f.getBitVector();
        t0s = new byte[num][];
        t1s = new byte[num][];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> {
            boolean x = x1BitVector.get(index);
            if (!x) {
                t0s[index] = valueByte[index];
                t1s[index] = BytesUtils.xor(valueByte[index], originData[index]);
            } else {
                t0s[index] = BytesUtils.xor(valueByte[index], originData[index]);
                t1s[index] = valueByte[index];
            }
        });
    }

    private void t0t1(CotSenderOutput cotSenderOutput) {
        Prg prg = PrgFactory.createInstance(envType, byteL);
        byte andNum = (byte) (bitLen % 8 == 0 ? 255 : (1 << (bitLen & 7)) - 1);
        // P1 creates t0
        IntStream t0IntStream = IntStream.range(0, num);
        t0IntStream = parallel ? t0IntStream.parallel() : t0IntStream;
        List<byte[]> t0t1Payload = t0IntStream
            .mapToObj(index -> {
                byte[] t0 = prg.extendToBytes(cotSenderOutput.getR0(index));
                t0[0] &= andNum;
                BytesUtils.xori(t0, t0s[index]);
                return t0;
            })
            .collect(Collectors.toList());
        // P1 creates t1
        IntStream t1IntStream = IntStream.range(0, num);
        t1IntStream = parallel ? t1IntStream.parallel() : t1IntStream;
        List<byte[]> t1Payload = t1IntStream
            .mapToObj(index -> {
                byte[] t1 = prg.extendToBytes(cotSenderOutput.getR1(index));
                t1[0] &= andNum;
                BytesUtils.xori(t1, t1s[index]);
                return t1;
            })
            .collect(Collectors.toList());
        // merge t0 and t1
        t0t1Payload.addAll(t1Payload);
        // sends s0 and s1
        DataPacketHeader t0t1Header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_T0_T1.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(t0t1Header, t0t1Payload));
    }

    private SquareZ2Vector[] s0s1(CotReceiverOutput cotReceiverOutput, List<byte[]> s0s1Payload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(s0s1Payload.size() == num * 2);
        byte andNum = (byte) (bitLen % 8 == 0 ? 255 : (1 << (bitLen & 7)) - 1);
        byte[][] s0s = s0s1Payload.subList(0, num).toArray(new byte[0][]);
        byte[][] s1s = s0s1Payload.subList(num, num * 2).toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        // Let P1's output be a1
        IntStream s0IntStream = IntStream.range(0, num);
        s0IntStream = parallel ? s0IntStream.parallel() : s0IntStream;
        byte[][] a1s = s0IntStream
            .mapToObj(index -> {
                boolean x1 = cotReceiverOutput.getChoice(index);
                byte[] a1 = prg.extendToBytes(cotReceiverOutput.getRb(index));
                a1[0] &= andNum;
                if (!x1) {
                    BytesUtils.xori(a1, s0s[index]);
                } else {
                    BytesUtils.xori(a1, s1s[index]);
                }
                return a1;
            })
            .toArray(byte[][]::new);
        BitVector[] tmp = ZlDatabase.create(bitLen, a1s).bitPartition(envType, parallel);
        for(int i = 0; i < tmp.length; i++){
            tmp[i].xori(r1z2Vectors[i]);
        }
        return Arrays.stream(tmp).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
    }
}
