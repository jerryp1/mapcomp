package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.cheetah;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.AbstractMillionaireParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Cheetah Millionaire Protocol Receiver.
 *
 * @author Li Peng
 * @date 2023/4/25
 */
public class CheetahMillionaireReceiver extends AbstractMillionaireParty {
    /**
     * 1-out-of-n (with n = 2^l) ot receiver.
     */
    private final LnotReceiver lnotReceiver;
    /**
     * z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;

    public CheetahMillionaireReceiver(Rpc receiverRpc, Party senderParty, CheetahMillionaireConfig config) {
        super(CheetahMillionairePtoDesc.getInstance(), receiverRpc, senderParty, config);
        lnotReceiver = LnotFactory.createReceiver(receiverRpc, senderParty, config.getLnotConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getBcConfig());
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
        z2cReceiver.init(maxNum * (maxQ - 1), maxNum * (maxQ - 1));
        lnotReceiver.init(4, maxNum, maxNum * maxQ);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector lt(int l, byte[][] ys) throws MpcAbortException {
        setPtoInput(l, ys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int[][] partitionInputArray = partitionInputArray();
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        SquareZ2Vector[][] shares = iterateSubstrings(partitionInputArray);
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        stopWatch.start();
        SquareZ2Vector z0 = combine(shares);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);

        return z0;
    }

    private SquareZ2Vector[][] iterateSubstrings(int[][] partitionInputArray) throws MpcAbortException {
        // P1 creates random lt_{0,j},eq_{0,j} for all j ∈ [0,q)
        BitVector[] lts = new BitVector[q];
        BitVector[] eqs = new BitVector[q];
        for (int j = 0; j < q; j++) {
            lts[j] = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num);
            eqs[j] = BitVectorFactory.createZeros(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num);
        }
        // for j ∈ [0, q) do
        for (int j = 0; j < q; j++) {

            // P0 & P1 invoke 1-out-of-2^m OT with P1 as receiver.
            LnotReceiverOutput lnotReceiverOutputLt = lnotReceiver.receive(partitionInputArray[j]);
            // for v ∈ [2^m], P1 receives lt_{0,j}_1
            DataPacketHeader ltsHeader = new DataPacketHeader(
                    encodeTaskId, getPtoDesc().getPtoId(), CheetahMillionairePtoDesc.PtoStep.SENDER_SENDS_S.ordinal(), extraInfo,
                    otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> ltsPayload = rpc.receive(ltsHeader).getPayload();
            extraInfo++;
            MpcAbortPreconditions.checkArgument(ltsPayload.size() == 1 << 4);
            BitVector[] evsLt = ltsPayload.stream()
                    .map(lt -> BitVectorFactory.create(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num, lt))
                    .toArray(BitVector[]::new);
            for (int index = 0; index < num; index++) {
                // payload
                int v = lnotReceiverOutputLt.getChoice(index);
                // ot key
                byte[] rv = lnotReceiverOutputLt.getRb(index);
                // decrypt
                lts[j].set(index, evsLt[v].get(index) ^ ((rv[0] % 2) != 0));
            }
            // for v ∈ [2^4], P1 receives eq_{0,j}_1
            DataPacketHeader eqsHeader = new DataPacketHeader(
                    encodeTaskId, getPtoDesc().getPtoId(), CheetahMillionairePtoDesc.PtoStep.SENDER_SENDS_T.ordinal(), extraInfo,
                    otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> eqsPayload = rpc.receive(eqsHeader).getPayload();
            extraInfo++;
            MpcAbortPreconditions.checkArgument(eqsPayload.size() == 1 << 4);
            BitVector[] evsEq = eqsPayload.stream()
                    .map(eq -> BitVectorFactory.create(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num, eq))
                    .toArray(BitVector[]::new);
            for (int index = 0; index < num; index++) {
                // payload
                int v = lnotReceiverOutputLt.getChoice(index);
                // ot key
                byte[] rv = lnotReceiverOutputLt.getRb(index);
                // decrypt
                eqs[j].set(index, evsEq[v].get(index) ^ ((rv[0] % 2) != 0));
            }
        }
        SquareZ2Vector[] ltShare = Arrays.stream(lts).map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] eqShare = Arrays.stream(eqs).map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        return new SquareZ2Vector[][]{ltShare, eqShare};
    }

    private SquareZ2Vector combine(SquareZ2Vector[][] shares) throws MpcAbortException {
        SquareZ2Vector[] lts = shares[0];
        SquareZ2Vector[] eqs = shares[1];
        // tree-based AND
        int logQ = LongUtils.ceilLog2(q);
        for (int i = 1; i <= logQ; i++) {
            for (int j = 0; j < q / (1 << i); j++) {
                lts[j] = z2cReceiver.xor(z2cReceiver.and(lts[j * 2 + 1], eqs[j * 2]), lts[j * 2]);
                // equalities computed on lowest significant bits are never used, thus omit computing
                if (j < q / (1 << i) - 1) {
                    eqs[j] = z2cReceiver.and(eqs[j * 2], eqs[j * 2 + 1]);
                }
            }
        }
        return lts[0];
    }

    private int[][] partitionInputArray() {
        // P1 parses each of its input element as y_{q-1} || ... || y_{0}, where y_j ∈ {0,1}^4 for all j ∈ [0,q).
        int[][] partitionInputArray = new int[q][num];
        IntStream.range(0, num).forEach(index -> {
            byte[] y = inputs[index];
            for (int lIndex = 0; lIndex < byteL; lIndex++) {
                byte lIndexByte = y[lIndex];
                // the left part
                partitionInputArray[lIndex * 2][index] = ((lIndexByte & 0xFF) >> 4);
                // the right part
                partitionInputArray[lIndex * 2 + 1][index] = (lIndexByte & 0x0F);
            }
        });
        return partitionInputArray;
    }
}
