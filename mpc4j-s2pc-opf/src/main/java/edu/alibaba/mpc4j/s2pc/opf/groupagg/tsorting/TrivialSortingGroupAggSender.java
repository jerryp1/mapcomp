package edu.alibaba.mpc4j.s2pc.opf.groupagg.tsorting;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.tsorting.TrivialSortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;

/**
 * Trivial sorting-based group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/19
 */
public class TrivialSortingGroupAggSender extends AbstractGroupAggParty {
    /**
     * Prefix aggregation sender.
     */
    private final PrefixAggParty prefixAggSender;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * Zl circuit sender
     */
    private final ZlcParty zlcSender;
    /**
     * Permutation receiver.
     */
    private final PermutationReceiver permutationReceiver;
    /**
     * Shared permutation sender.
     */
    private final SharedPermutationParty sharedPermutationSender;
    /**
     * Permutation sender.
     */
    private final PermutationSender permutationSender;
    /**
     * Plain payload mux receiver.
     */
    private final PlainPayloadMuxParty plainPayloadMuxReceiver;
    /**
     * A2b sender.
     */
    private final A2bParty a2bSender;
    /**
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit z2IntegerCircuit;
    /**
     * total group shares.
     */
    private Vector<byte[]> mergedGroups;
    /**
     * permutation
     */
    private Vector<byte[]> perms;
    /**
     * summation in z2.
     */
    private SquareZ2Vector[] sumZ2;

    public TrivialSortingGroupAggSender(Rpc senderRpc, Party receiverParty, TrivialSortingGroupAggConfig config) {
        super(TrivialSortingGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        permutationReceiver = PermutationFactory.createReceiver(senderRpc, receiverParty, config.getPermutationConfig());
        sharedPermutationSender = SharedPermutationFactory.createSender(senderRpc, receiverParty, config.getSharedPermutationConfig());
        permutationSender = PermutationFactory.createSender(senderRpc, receiverParty, config.getPermutationConfig());
        a2bSender = A2bFactory.createSender(senderRpc, receiverParty, config.getA2bConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        z2IntegerCircuit = new Z2IntegerCircuit(z2cSender);
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        prefixAggSender.init(maxL, maxNum);
        z2cSender.init(maxL * maxNum);
        zlcSender.init(1);
        permutationReceiver.init(maxL, maxNum);
        sharedPermutationSender.init(maxNum);
        permutationSender.init(maxL, maxNum);
        plainPayloadMuxReceiver.init(maxNum);
        a2bSender.init(maxL, maxNum);


        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector interFlagE) throws MpcAbortException {
        // set input
        setPtoInput(groupField, aggField, interFlagE);
        // having state
        if (havingState) {
            getSum();
        }
        // share and merge group shares
        share();
        // sort
        stopWatch.start();
        groupTripleNum = TRIPLE_NUM;
        sort();
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // apply permutation to agg
        stopWatch.start();
        if (aggField == null) {
            apply();
        } else {
            applyWithSenderAgg();
        }
        stopWatch.stop();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        SquareZ2Vector[] receiverAggAs = getAggAttr();
        // ### test
        // zlcSender.revealOther(otherAggB2a);
        // revealOtherGroup(mergedGroups);
        // agg
        stopWatch.start();
        aggTripleNum = TRIPLE_NUM;
        aggregation(mergedGroups, receiverAggAs, e);
        stopWatch.stop();
        aggTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        aggTripleNum = TRIPLE_NUM - aggTripleNum;
        stopWatch.reset();
        return null;
    }

    private void getSum() throws MpcAbortException {
        SquareZlVector mul = plainPayloadMuxReceiver.mux(e, null, Long.SIZE);
        BigInteger sum = Arrays.stream(mul.getZlVector().getElements()).reduce(BigInteger.ZERO, (a, b) -> zl.add(a, b));
        SquareZlVector sumZl = SquareZlVector.create(zl, IntStream.range(0, num).mapToObj(i -> sum).toArray(BigInteger[]::new), false);
        sumZ2 = a2bSender.a2b(sumZl);
    }

    private void share() {
        if (senderGroupBitLength == 0 && receiverGroupBitLength == 0) {
            throw new IllegalArgumentException("group should be set");
        }
        // sender == 0
        if (senderGroupBitLength != 0) {
            Vector<byte[]> groupBytes = GroupAggUtils.binaryStringToBytes(groupAttr);
            mergedGroups = shareOwn(groupBytes);
            if (receiverGroupBitLength != 0) {
                Vector<byte[]> receiverGroupShare = shareOther();
                mergedGroups = mergeGroup(mergedGroups, receiverGroupShare);
            }
            // receiver == 0
        } else {
            mergedGroups = shareOther();
        }
    }

    private void sort() throws MpcAbortException {
        // merge input
        Vector<byte[]> eByte = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(1)
            .put(e.getBitVector().get(i) ? (byte) 1 : (byte) 0).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> mergedInput = merge(Arrays.asList(eByte, mergedGroups));
        // transpose
        SquareZ2Vector[] transposedGroup = Arrays.stream(TransposeUtils.transposeSplit(mergedInput, (totalGroupByteLength + 1) * Byte.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        SquareZ2Vector[] sortInput = new SquareZ2Vector[totalGroupBitLength + 1];
        // e
        sortInput[0] = transposedGroup[7];
        // group
        System.arraycopy(transposedGroup, 8, sortInput, 1, senderGroupBitLength);
        System.arraycopy(transposedGroup, 8 + (senderGroupByteLength << 3), sortInput, 1 + senderGroupBitLength, receiverGroupBitLength);
        // sort
        SquareZ2Vector[] permsVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{sortInput},
            null, PlainZ2Vector.createOnes(1), true, false))
            .map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        transposedGroup[7] = sortInput[0];
        System.arraycopy(sortInput, 1, transposedGroup, 8, senderGroupBitLength);
        System.arraycopy(sortInput, 1 + senderGroupBitLength, transposedGroup, 8 + (senderGroupByteLength << 3), receiverGroupBitLength);

        // transpose
        perms = TransposeUtils.transposeMergeToVector(Arrays.stream(permsVector).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        // get e and sorted groups from psorter's input.
        Vector<byte[]> mergedOutput = TransposeUtils.transposeMergeToVector(Arrays.stream(transposedGroup)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        List<Vector<byte[]>> split = GroupAggUtils.split(mergedOutput, new int[]{1, totalGroupByteLength});
        e = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> e.getBitVector().set(i, (split.get(0).get(i)[0] & 1) == 1));
        mergedGroups = split.get(1);
    }

    private void apply() throws MpcAbortException {
        // apply permutation to plain agg
        aggShare = permutationReceiver.permute(perms, Long.BYTES);
    }

    private void applyWithSenderAgg() throws MpcAbortException {
        // apply permutation to plain agg
        int byteLen = dummyPayload ? 2 * Long.BYTES : Long.BYTES;
        aggShare = IntStream.range(0, num).mapToObj(i ->
            ByteBuffer.allocate(byteLen)
                .put(LongUtils.longToByteArray(aggAttr[i])).array())
            .collect(Collectors.toCollection(Vector::new));
        aggShare = permutationSender.permute(perms, aggShare);
        if (dummyPayload) {
            aggShare = aggShare.stream().map(v -> {
                byte[] bytes = new byte[Long.BYTES];
                System.arraycopy(v, 0, bytes, 0, Long.BYTES);
                return bytes;
            }).collect(Collectors.toCollection(Vector::new));
        }
    }

    private Vector<byte[]> mergeGroup(Vector<byte[]> senderGroupShare, Vector<byte[]> receiverGroupShare) {
        return IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(senderGroupShare.get(i)).put(receiverGroupShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
    }

    private void aggregation(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException {
        PrefixAggOutput outputs = prefixAggSender.agg(groupField, aggField, flag);
        if (dummyPayload) {
            prefixAggSender.agg(groupField, aggField, flag);
        }
        z2cSender.revealOther(outputs.getIndicator());
        // reveal
        // zlcSender.revealOther(outputs.getAggs());
        // if q11 then compare
        SquareZ2Vector[] aggTemp = outputs.getAggsBinary();
        if (havingState) {
            SquareZ2Vector compare = (SquareZ2Vector) z2IntegerCircuit.sub(aggTemp, sumZ2)[0];
            z2cSender.revealOther(compare);
        }
        // reveal
        z2cSender.revealOther(aggTemp);
        revealOtherGroup(outputs.getGroupings());

        Preconditions.checkArgument(outputs.getNum() == num, "size of outputs not correct");
    }

    protected Vector<byte[]> shareOwn(Vector<byte[]> input) {
        Vector<byte[]> ownShares = IntStream.range(0, input.size()).mapToObj(i -> {
            byte[] bytes = new byte[input.get(0).length];
            secureRandom.nextBytes(bytes);
            return bytes;
        }).collect(Collectors.toCollection(Vector::new));

        List<byte[]> otherShares = IntStream.range(0, input.size()).mapToObj(i -> BytesUtils.xor(input.get(i), ownShares.get(i))).collect(Collectors.toList());

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_SHARES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;

        return ownShares;
    }

    protected Vector<byte[]> shareOther() {
        DataPacketHeader receiveSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_SHARES.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiveSharesPayload = rpc.receive(receiveSharesHeader).getPayload();
        extraInfo++;
        return new Vector<>(receiveSharesPayload);
    }

    protected void revealOtherGroup(Vector<byte[]> input) {
        List<byte[]> otherShares = new ArrayList<>(input);

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_OUTPUT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;
    }

    protected void revealOtherBit(Vector<byte[]> input) {

        List<byte[]> otherShares = new ArrayList<>(input);

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_BIT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;
    }

    protected void revealOtherLong(Vector<byte[]> input) {
        List<byte[]> otherShares = new ArrayList<>(input);
        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.TEST.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;
    }
}
