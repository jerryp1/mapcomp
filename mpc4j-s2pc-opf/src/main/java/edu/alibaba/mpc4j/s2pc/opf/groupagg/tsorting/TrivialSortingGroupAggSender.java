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
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.tsorting.TrivialSortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Trivial sorting-based group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/19
 */
public class TrivialSortingGroupAggSender extends AbstractGroupAggParty {
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Zl mux party.
     */
    private final ZlMuxParty zlMuxSender;
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
     * B2a sender.
     */
    private final B2aParty b2aSender;
    /**
     * Permutation receiver.
     */
    private final PermutationReceiver permutationReceiver;
    /**
     * Shared permutation sender.
     */
    private final SharedPermutationParty sharedPermutationSender;
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

    public TrivialSortingGroupAggSender(Rpc senderRpc, Party receiverParty, TrivialSortingGroupAggConfig config) {
        super(TrivialSortingGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnReceiver = OsnFactory.createReceiver(senderRpc, receiverParty, config.getOsnConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
        permutationReceiver = PermutationFactory.createReceiver(senderRpc, receiverParty, config.getPermutationConfig());
        sharedPermutationSender = SharedPermutationFactory.createSender(senderRpc, receiverParty, config.getSharedPermutationConfig());
//        addSubPtos(osnReceiver);
//        addSubPtos(zlMuxSender);
//        addSubPtos(sharedPermutationSender);
//        addSubPtos(prefixAggSender);
//        addSubPtos(z2cSender);
//        addSubPtos(zlcSender);
//        addSubPtos(b2aSender);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cSender);
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnReceiver.init(maxNum);
        zlMuxSender.init(maxNum);
        prefixAggSender.init(maxL, maxNum);
        z2cSender.init(maxL * maxNum);
        zlcSender.init(1);
        b2aSender.init(maxL, maxNum);
        permutationReceiver.init(maxL, maxNum);
        sharedPermutationSender.init(maxNum);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector interFlagE) throws MpcAbortException {
        assert aggField == null;
        // set input
        setPtoInput(groupField, aggField, interFlagE);
        // share and merge group shares
        share(groupField);
        // sort
        sort();
        // apply permutation to agg
        apply();
        // b2a
//        SquareZlVector receiverAggAs = b2a();
        SquareZ2Vector[] receiverAggAs = getAggAttr();
//        // ### test
//        zlcSender.revealOther(otherAggB2a);
//        revealOtherGroup(mergedGroups);
        // agg
        aggregation(mergedGroups, receiverAggAs, e);
        return null;
    }

    private void share(String[] groups) {
        Vector<byte[]> groupBytes = GroupAggUtils.binaryStringToBytes(groups);
        Vector<byte[]> senderGroupShare = shareOwn(groupBytes);
        Vector<byte[]> receiverGroupShare = shareOther();
        mergedGroups = mergeGroup(senderGroupShare, receiverGroupShare);
    }

    private void sort() throws MpcAbortException {
        // merge input
        Vector<byte[]> eByte = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(1)
            .put(e.getBitVector().get(i) ? (byte) 1 : (byte) 0).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> mergedInput = merge(Arrays.asList(eByte, mergedGroups));
        // transpose
        SquareZ2Vector[] transposedGroup = Arrays.stream(TransposeUtils.transposeSplit(mergedInput, (totalGroupByteLength + 1) * Byte.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        SquareZ2Vector[] sortInput = new SquareZ2Vector[receiverGroupBitLength + senderGroupBitLength + 1];
        sortInput[0] = transposedGroup[7];
        System.arraycopy(transposedGroup, 8, sortInput, 1, senderGroupBitLength);
        System.arraycopy(transposedGroup, 8 + (senderGroupByteLength<<3), sortInput, 1 + senderGroupBitLength, receiverGroupBitLength);
        // sort
        SquareZ2Vector[] permsVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{sortInput},
                null, PlainZ2Vector.createOnes(1), true, false))
            .map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        transposedGroup[7] = sortInput[0];
        System.arraycopy(sortInput, 1, transposedGroup, 8, senderGroupBitLength);
        System.arraycopy(sortInput, 1 + senderGroupBitLength, transposedGroup, 8 + (senderGroupByteLength<<3), receiverGroupBitLength);

//        // sort
//        SquareZ2Vector[] permsVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{transposedGroup},
//            null, PlainZ2Vector.createOnes(1), true, false))
//            .map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);


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

    private Vector<byte[]> mergeGroup(Vector<byte[]> senderGroupShare, Vector<byte[]> receiverGroupShare) {
        return IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(senderGroupShare.get(i)).put(receiverGroupShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
    }

    private SquareZlVector b2a() throws MpcAbortException {
        // b2a
        SquareZ2Vector[] transposed = Arrays.stream(TransposeUtils.transposeSplit(aggShare, Long.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        return b2aSender.b2a(transposed);
    }

    private void aggregation(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException {
        PrefixAggOutput agg = prefixAggSender.agg(groupField, aggField, flag);
        // reveal
//        zlcSender.revealOther(agg.getAggs());
        z2cSender.revealOther(agg.getAggsBinary());
        revealOtherGroup(agg.getGroupings());
        z2cSender.revealOther(agg.getIndicator());

        Preconditions.checkArgument(agg.getNum() == num, "size of output not correct");
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
