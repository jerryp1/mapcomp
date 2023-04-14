package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.AbstractPlainPeqtParty;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22.Cgs22PlainPeqtPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CGS22 plain private equality test sender.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class Cgs22PlainPeqtSender extends AbstractPlainPeqtParty {
    /**
     * Boolean circuit sender
     */
    private final BcParty bcSender;
    /**
     * LNOT sender
     */
    private final LnotSender lnotSender;

    public Cgs22PlainPeqtSender(Rpc senderRpc, Party receiverParty, Cgs22PlainPeqtConfig config) {
        super(Cgs22PlainPeqtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bcSender = BcFactory.createSender(senderRpc, receiverParty, config.getBcConfig());
        addSubPtos(bcSender);
        lnotSender = LnotFactory.createSender(senderRpc, receiverParty, config.getLnotConfig());
        addSubPtos(lnotSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // q = l / m, where m = 4
        int maxByteL = CommonUtils.getByteLength(maxL);
        int maxQ = maxByteL * 2;
        bcSender.init(maxNum * (maxQ - 1), maxNum * (maxQ - 1));
        lnotSender.init(4, maxNum, maxNum * maxQ);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareShareZ2Vector peqt(int l, byte[][] xs) throws MpcAbortException {
        setPtoInput(l, xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // q = l/4. P0 parses each of its input element as x_{q-1}||...||x_{0}, where x_j ∈ {0,1}^4 for all j ∈ [0,q).
        int q = byteL * 2;
        int[][] partitionXs = new int[num][q];
        IntStream.range(0, num).forEach(index -> {
            byte[] x = xs[index];
            for (int lIndex = 0; lIndex < byteL; lIndex++) {
                byte lIndexByte = x[lIndex];
                // the left part
                partitionXs[index][lIndex * 2] = ((lIndexByte & 0xFF) >> 4);
                // the right part
                partitionXs[index][lIndex * 2 + 1] = (lIndexByte & 0x0F);
            }
        });
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P0 samples eq_{0, j} for all j ∈ [0,q)
        BitVector[] eqs = IntStream.range(0, q)
            .mapToObj(j -> BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num))
            .toArray(BitVector[]::new);
        // for j ∈ [0, q) do
        for (int j = 0; j < q; j++) {
            final int jFinal = j;
            // P0 & P1 invoke 1-out-of-2^4 OT with P0 as sender.
            LnotSenderOutput lnotSenderOutput = lnotSender.send(num);
            // for v ∈ [2^4], P0 sets e_{j,v} ← <eq_{0,j}>_0 ⊕ 1{x_{1,j} = v}
            IntStream vIntStream = IntStream.range(0, 1 << 4);
            vIntStream = parallel ? vIntStream.parallel() : vIntStream;
            List<byte[]> evsPayload = vIntStream
                .mapToObj(v -> {
                    BitVector ev = BitVectorFactory.createRandom(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num, secureRandom);
                    for (int index = 0; index < num; index++) {
                        byte[] ri = lnotSenderOutput.getRb(index, v);
                        if (v == partitionXs[index][jFinal]) {
                            // x_j == v, e_{j,v} = Rb ⊕ 1
                            ev.set(index, (ri[0] % 2) == 0);
                        } else {
                            // x_j != v, e_{j,v} = Rb
                            ev.set(index, (ri[0] % 2) != 0);
                        }
                    }
                    // e_{j,v} ⊕ eqs_j
                    ev.xori(eqs[jFinal]);
                    return ev.getBytes();
                })
                .collect(Collectors.toList());
            DataPacketHeader evsHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_EVS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            extraInfo++;
            rpc.send(DataPacket.fromByteArrayList(evsHeader, evsPayload));
        }
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        stopWatch.start();
        // tree-based AND
        SquareShareZ2Vector[] eqs0 = IntStream.range(0, q)
            .mapToObj(j -> SquareShareZ2Vector.create(eqs[j], false))
            .toArray(SquareShareZ2Vector[]::new);
        int logQ = LongUtils.ceilLog2(q);
        for (int h = 1; h <= logQ; h++) {
            int nodeNum = eqs0.length / 2;
            SquareShareZ2Vector[] eqsx0 = new SquareShareZ2Vector[nodeNum];
            SquareShareZ2Vector[] eqsy0 = new SquareShareZ2Vector[nodeNum];
            for (int i = 0; i < nodeNum; i++) {
                eqsx0[i] = eqs0[i * 2];
                eqsy0[i] = eqs0[i * 2 + 1];
            }
            SquareShareZ2Vector[] eqsz0 = bcSender.and(eqsx0, eqsy0);
            if (eqs0.length % 2 == 1) {
                eqsz0 = Arrays.copyOf(eqsz0, nodeNum + 1);
                eqsz0[nodeNum] = eqs0[eqs0.length - 1];
            }
            eqs0 = eqsz0;
        }
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return eqs0[0];
    }
}
