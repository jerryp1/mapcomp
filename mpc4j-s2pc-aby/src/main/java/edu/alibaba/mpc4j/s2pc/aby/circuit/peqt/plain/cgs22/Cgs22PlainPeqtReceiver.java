package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGS22 plain private equality test receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class Cgs22PlainPeqtReceiver extends AbstractPlainPeqtParty {
    /**
     * Boolean circuit receiver
     */
    private final BcParty bcReceiver;
    /**
     * LNOT receiver
     */
    private final LnotReceiver lnotReceiver;

    public Cgs22PlainPeqtReceiver(Rpc senderRpc, Party receiverParty, Cgs22PlainPeqtConfig config) {
        super(Cgs22PlainPeqtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bcReceiver = BcFactory.createReceiver(senderRpc, receiverParty, config.getBcConfig());
        addSubPtos(bcReceiver);
        lnotReceiver = LnotFactory.createReceiver(senderRpc, receiverParty, config.getLnotConfig());
        addSubPtos(lnotReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // q = l / m, where m = 4
        int maxByteL = CommonUtils.getByteLength(maxL);
        int maxQ = maxByteL * 2;
        bcReceiver.init(maxNum * (maxQ - 1), maxNum * (maxQ - 1));
        lnotReceiver.init(4, maxNum, maxNum * maxQ);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareShareZ2Vector peqt(int l, byte[][] ys) throws MpcAbortException {
        setPtoInput(l, ys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // q = l/4. P0 parses each of its input element as x_{q-1}||...||x_{0}, where x_j ∈ {0,1}^4 for all j ∈ [0,q).
        int q = byteL * 2;
        int[][] partitionYs = new int[q][num];
        IntStream.range(0, num).forEach(index -> {
            byte[] y = ys[index];
            for (int lIndex = 0; lIndex < byteL; lIndex++) {
                byte lIndexByte = y[lIndex];
                // the left part
                partitionYs[lIndex * 2][index] = ((lIndexByte & 0xFF) >> 4);
                // the right part
                partitionYs[lIndex * 2 + 1][index] = (lIndexByte & 0x0F);
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
            // P0 & P1 invoke 1-out-of-2^4 OT with P1 as receiver.
            LnotReceiverOutput lnotReceiverOutput = lnotReceiver.receive(partitionYs[j]);
            DataPacketHeader evsHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_EVS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            // for v ∈ [2^4], P0 sets e_{j,v} ← <eq_{0,j}>_0 ⊕ 1{x_{1,j} = v}
            List<byte[]> evsPayload = rpc.receive(evsHeader).getPayload();
            extraInfo++;
            MpcAbortPreconditions.checkArgument(evsPayload.size() == 1 << 4);
            BitVector[] evs = evsPayload.stream()
                .map(ev -> BitVectorFactory.create(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num, ev))
                .toArray(BitVector[]::new);
            for (int index = 0; index < num; index++) {
                int v = lnotReceiverOutput.getChoice(index);
                byte[] rv = lnotReceiverOutput.getRb(index);
                eqs[j].set(index, evs[v].get(index) ^ ((rv[0] % 2) != 0));
            }
        }
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        stopWatch.start();
        SquareShareZ2Vector[] eqs1 = IntStream.range(0, q)
            .mapToObj(j -> SquareShareZ2Vector.create(eqs[j], false))
            .toArray(SquareShareZ2Vector[]::new);
        // tree-based AND
        int logQ = LongUtils.ceilLog2(q);
        for (int h = 1; h <= logQ; h++) {
            int nodeNum = eqs1.length / 2;
            SquareShareZ2Vector[] eqsx0 = new SquareShareZ2Vector[nodeNum];
            SquareShareZ2Vector[] eqsy0 = new SquareShareZ2Vector[nodeNum];
            for (int i = 0; i < nodeNum; i++) {
                eqsx0[i] = eqs1[i * 2];
                eqsy0[i] = eqs1[i * 2 + 1];
            }
            SquareShareZ2Vector[] eqsz1 = bcReceiver.and(eqsx0, eqsy0);
            if (eqs1.length % 2 == 1) {
                eqsz1 = Arrays.copyOf(eqsz1, nodeNum + 1);
                eqsz1[nodeNum] = eqs1[eqs1.length - 1];
            }
            eqs1 = eqsz1;
        }
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return eqs1[0];
    }
}
