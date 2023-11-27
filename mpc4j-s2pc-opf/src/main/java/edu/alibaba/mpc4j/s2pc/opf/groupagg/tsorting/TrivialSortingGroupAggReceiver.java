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
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
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
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Trivial sorting-based group aggregation receiver.
 *
 * @author Li Peng
 * @date 2023/11/19
 */
public class TrivialSortingGroupAggReceiver extends AbstractGroupAggParty {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Zl mux receiver.
     */
    private final ZlMuxParty zlMuxReceiver;
    /**
     * Prefix aggregation receiver.
     */
    private final PrefixAggParty prefixAggReceiver;
    /**
     * Z2 circuit party.
     */
    private final Z2cParty z2cReceiver;
    /**
     * Zl circuit party.
     */
    private final ZlcParty zlcReceiver;
    /**
     * B2a receiver.
     */
    private final B2aParty b2aReceiver;
    /**
     * Permutation sender.
     */
    private final PermutationSender permutationSender;
    /**
     * Shared permutation receiver.
     */
    private final SharedPermutationParty sharedPermutationReceiver;
    /**
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit z2IntegerCircuit;
    /**
     * Prefix aggregation type.
     */
    private final PrefixAggTypes prefixAggType;
    /**
     * total group shares.
     */
    private Vector<byte[]> mergedGroups;
    /**
     * permutation
     */
    private Vector<byte[]> perms;

    public TrivialSortingGroupAggReceiver(Rpc receiverRpc, Party senderParty, TrivialSortingGroupAggConfig config) {
        super(TrivialSortingGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnSender = OsnFactory.createSender(receiverRpc, senderParty, config.getOsnConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        b2aReceiver = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
        permutationSender = PermutationFactory.createSender(receiverRpc, senderParty, config.getPermutationConfig());
        sharedPermutationReceiver = SharedPermutationFactory.createReceiver(receiverRpc, senderParty, config.getSharedPermutationConfig());
//        addSubPtos(osnSender);
//        addSubPtos(zlMuxReceiver);
//        addSubPtos(sharedPermutationReceiver);
//        addSubPtos(prefixAggReceiver);
//        addSubPtos(z2cReceiver);
//        addSubPtos(zlcReceiver);
//        addSubPtos(b2aReceiver);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cReceiver);
        prefixAggType = config.getPrefixAggConfig().getPrefixType();
        secureRandom = new SecureRandom();
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnSender.init(maxNum);
        zlMuxReceiver.init(maxNum);
        prefixAggReceiver.init(maxL, maxNum);
        z2cReceiver.init(maxL * maxNum);
        zlcReceiver.init(1);
        b2aReceiver.init(maxL, maxNum);
        permutationSender.init(maxL, maxNum);
        sharedPermutationReceiver.init(maxNum);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupAttr, final long[] aggAttr, final SquareZ2Vector interFlagE) throws MpcAbortException {
        assert aggAttr != null;
        // set input
        setPtoInput(groupAttr, aggAttr, interFlagE);
        // share and merge groups
        share(groupAttr);
        // sort
        sort();
        // apply permutation to agg
        apply(aggAttr);
        // b2a
        SquareZlVector receiverAggAs = b2a();
        // ### test
        String[] groupResult = revealBothGroup(mergedGroups);
        ZlVector zlVector = zlcReceiver.revealOwn(receiverAggAs);
        // aggregation
        return aggregation(mergedGroups, receiverAggAs, e);
    }

    private void share(String[] groups) {
        Vector<byte[]> groupBytes = GroupAggUtils.binaryStringToBytes(groups);
        Vector<byte[]> senderGroupShare = shareOther();
        Vector<byte[]> receiverGroupShare = shareOwn(groupBytes);
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
        // sort
        SquareZ2Vector[] permsVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{transposedGroup},
            null, PlainZ2Vector.createOnes(1), true, false))
            .map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
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

    private void apply(long[] agg) throws MpcAbortException {
        // apply permutation to plain agg
        aggShare = IntStream.range(0, num).mapToObj(i ->
            ByteBuffer.allocate(Long.BYTES)
                .put(LongUtils.longToByteArray(agg[i])).array())
            .collect(Collectors.toCollection(Vector::new));
        aggShare = permutationSender.permute(perms, aggShare);
    }


    private Vector<byte[]> mergeGroup(Vector<byte[]> senderGroupShare, Vector<byte[]> receiverGroupShare) {
        return IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(senderGroupShare.get(i)).put(receiverGroupShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
    }

    private SquareZlVector b2a() throws MpcAbortException {
        // b2a, transfer agg to arithmetic share
        SquareZ2Vector[] transposed = Arrays.stream(TransposeUtils.transposeSplit(aggShare, Long.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        return b2aReceiver.b2a(transposed);
    }

    private GroupAggOut aggregation(Vector<byte[]> groupField, SquareZlVector aggField, SquareZ2Vector flag) throws MpcAbortException {
        // agg
        switch (prefixAggType) {
            case SUM:
                return sumAgg(groupField, aggField, flag);
            case MAX:
                return maxAgg(groupField, aggField, flag);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixAggTypes.class.getSimpleName() + ": " + prefixAggType.name());
        }
    }

    private GroupAggOut sumAgg(Vector<byte[]> groupField, SquareZlVector aggField, SquareZ2Vector flag) throws MpcAbortException {
        Zl zl = aggField.getZl();
        // agg
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField, flag);
        // reveal
        ZlVector aggResult = zlcReceiver.revealOwn(agg.getAggs());
        String[] tureGroup = revealBothGroup(agg.getGroupings());
        BitVector indicator = z2cReceiver.revealOwn(agg.getIndicator());
        // subtraction
        int[] indexes = obtainIndexes(indicator);
        BigInteger[] result = aggResult.getElements();
        for (int i = 0; i < indexes.length - 1; i++) {
            result[indexes[i]] = zl.sub(result[indexes[i]], result[indexes[i + 1]]);
        }
        return new GroupAggOut(tureGroup, result);
    }

    private GroupAggOut maxAgg(Vector<byte[]> groupField, SquareZlVector aggField, SquareZ2Vector flag) throws MpcAbortException {
        // agg
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField, flag);
        // reveal
        Preconditions.checkArgument(agg.getNum() == num, "size of output not correct");
        ZlVector aggResult = zlcReceiver.revealOwn(agg.getAggs());
        String[] tureGroup = revealBothGroup(agg.getGroupings());
        BitVector groupIndicator = z2cReceiver.revealOwn(agg.getIndicator());
        // filter
        int[] indexes = obtainIndexes(groupIndicator);
        BigInteger[] filteredAgg = new BigInteger[indexes.length];
        String[] filteredGroup = new String[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            filteredAgg[i] = aggResult.getElement(indexes[i]);
            filteredGroup[i] = tureGroup[indexes[i]];
        }
        return new GroupAggOut(filteredGroup, filteredAgg);
    }

    private int[] obtainIndexes(BitVector input) {
        return IntStream.range(0, num).filter(input::get).toArray();
    }

    private String[] revealGroup(Vector<byte[]> ownGroup, int bitLength) {
        DataPacketHeader groupHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_OUTPUT.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderDataSizePayload = rpc.receive(groupHeader).getPayload();
        extraInfo++;
        Preconditions.checkArgument(senderDataSizePayload.size() == num, "group num not match");
        Vector<byte[]> plainBytes = IntStream.range(0, num).mapToObj(i ->
            BytesUtils.xor(senderDataSizePayload.get(i), ownGroup.get(i))).collect(Collectors.toCollection(Vector::new));
        return GroupAggUtils.bytesToBinaryString(plainBytes, bitLength);
    }

    private String[] revealBothGroup(Vector<byte[]> ownGroup) {
        DataPacketHeader groupHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_OUTPUT.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderDataSizePayload = rpc.receive(groupHeader).getPayload();
        extraInfo++;
        Preconditions.checkArgument(senderDataSizePayload.size() == num, "group num not match");
        Vector<byte[]> plainBytes = IntStream.range(0, num).mapToObj(i ->
            BytesUtils.xor(senderDataSizePayload.get(i), ownGroup.get(i))).collect(Collectors.toCollection(Vector::new));
        return GroupAggUtils.bytesToBinaryString(plainBytes, senderGroupBitLength, receiverGroupBitLength);
    }

    private List<byte[]> revealOwnBit(Vector<byte[]> ownGroup) {
        DataPacketHeader groupHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_BIT.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderDataSizePayload = rpc.receive(groupHeader).getPayload();
        extraInfo++;
        Preconditions.checkArgument(senderDataSizePayload.size() == num, "group num not match");
        return IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(senderDataSizePayload.get(i), ownGroup.get(i))).collect(Collectors.toList());
    }

    private long[] revealOwnLong(Vector<byte[]> input) {
        DataPacketHeader groupHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.TEST.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderDataSizePayload = rpc.receive(groupHeader).getPayload();
        extraInfo++;
        Preconditions.checkArgument(senderDataSizePayload.size() == num, "group num not match");
        return IntStream.range(0, num).mapToLong(i -> LongUtils.byteArrayToLong(BytesUtils.xor(senderDataSizePayload.get(i), input.get(i)))).toArray();
    }

    protected Vector<byte[]> shareOwn(Vector<byte[]> input) {
        Vector<byte[]> ownShares = IntStream.range(0, input.size()).mapToObj(i -> {
            byte[] bytes = new byte[input.get(0).length];
            secureRandom.nextBytes(bytes);
            return bytes;
        }).collect(Collectors.toCollection(Vector::new));

        List<byte[]> otherShares = IntStream.range(0, input.size()).mapToObj(i -> BytesUtils.xor(input.get(i), ownShares.get(i))).collect(Collectors.toList());

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_SHARES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;

        return ownShares;
    }

    protected Vector<byte[]> shareOther() {
        DataPacketHeader receiveSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_SHARES.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiveSharesPayload = rpc.receive(receiveSharesHeader).getPayload();
        extraInfo++;
        return new Vector<>(receiveSharesPayload);
    }

    private int[] getGroupIndexes(BitVector indicator) {
        return IntStream.range(0, num).filter(indicator::get).toArray();
    }
}
