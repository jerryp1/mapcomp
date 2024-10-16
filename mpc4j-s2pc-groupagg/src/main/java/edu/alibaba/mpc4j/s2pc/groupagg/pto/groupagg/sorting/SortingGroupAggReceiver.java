package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.sorting;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.sorting.SortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;

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
 * Sorting-based group aggregation receiver.
 *
 */
public class SortingGroupAggReceiver extends AbstractGroupAggParty {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Zl mux receiver.
     */
    private final ZlMuxParty zlMuxReceiver;
    /**
     * Shared permutation receiver.
     */
    private final SharedPermutationParty sharedPermutationReceiver;
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
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit z2IntegerCircuit;
    /**
     * Own bit split.
     */
    private Vector<byte[]> eByte;

    private Vector<byte[]> piGi;
    /**
     * Prefix aggregation type.
     */
    private PrefixAggTypes prefixAggType;

    public SortingGroupAggReceiver(Rpc receiverRpc, Party senderParty, SortingGroupAggConfig config) {
        super(SortingGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnSender = OsnFactory.createSender(receiverRpc, senderParty, config.getOsnConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        sharedPermutationReceiver = SharedPermutationFactory.createReceiver(receiverRpc, senderParty, config.getSharedPermutationConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        b2aReceiver = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
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
        sharedPermutationReceiver.init(maxNum);
        prefixAggReceiver.init(maxL, maxNum);
        z2cReceiver.init(maxL * maxNum);
        zlcReceiver.init(1);
        b2aReceiver.init(maxL, maxNum);

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
        // osn1
        osn1(groupAttr, aggAttr);
        // pSorter using e and receiver's group
        pSorter();
        // apply piGi to receiver's agg and sender's group
        applyPiGi();
        // merge group
        Vector<byte[]> mergedTwoGroup = mergeGroup();
        // b2a
        // SquareZlVector receiverAggAs = b2a();
        SquareZ2Vector[] receiverAggAs = getAggAttr();
        // ### test
        // String[] groupResult = revealBothGroup(mergedTwoGroup);
        // ZlVector zlVector = zlcReceiver.revealOwn(receiverAggAs);
        // aggregation
        return aggregation(mergedTwoGroup, receiverAggAs, e);
    }

    private void osn1(String[] groupAttr, long[] aggAttr) throws MpcAbortException {

        // merge group
        Vector<byte[]> groupBytes = GroupAggUtils.binaryStringToBytes(groupAttr);
        // osn1
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        Vector<byte[]> osnInput1 = intStream.mapToObj(i -> ByteBuffer.allocate(receiverGroupByteLength + Long.BYTES + 1)
            .put(groupBytes.get(i)).put(LongUtils.longToByteArray(aggAttr[i]))
            .put(e.getBitVector().get(i) ? (byte) 1 : (byte) 0).array()).collect(Collectors.toCollection(Vector::new));

        Vector<byte[]> osnOutput1 = osnSender.osn(osnInput1, receiverGroupByteLength + Long.BYTES + 1).getVector();
        // split
        List<Vector<byte[]>> splitOsn1 = GroupAggUtils.split(osnOutput1, new int[]{receiverGroupByteLength, Long.BYTES, 1});
        receiverGroupShare = splitOsn1.get(0);
        aggShare = splitOsn1.get(1);
        eByte = splitOsn1.get(2);

        // ### test ownBit
        // List<byte[]> ownBit = revealOwnBit(eByte);

        // share
        senderGroupShare = shareOther();
        // ### test
        // String[] senderGroup = revealGroup(senderGroupShare, senderGroupBitLength);
    }

    private void pSorter() throws MpcAbortException {
        // generate input of psorter
        Vector<byte[]> mergedPsorterInput = merge(Arrays.asList(eByte, receiverGroupShare));
        // prepare psorter input, with shared e and group of receiver
        SquareZ2Vector[] psorterInput = Arrays.stream(TransposeUtils.transposeSplit(mergedPsorterInput, (receiverGroupByteLength + 1) * 8))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        SquareZ2Vector[] sortInput = new SquareZ2Vector[receiverGroupBitLength + 1];
        sortInput[0] = psorterInput[7];
        System.arraycopy(psorterInput, 8, sortInput, 1, receiverGroupBitLength);
        // sort
        SquareZ2Vector[] piGiVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{sortInput},
            null, PlainZ2Vector.createOnes(1), true, false))
            .map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        psorterInput[7] = sortInput[0];
        System.arraycopy(sortInput, 1, psorterInput, 8, receiverGroupBitLength);

//        // psorter
        // SquareZ2Vector[] piGiVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{psorterInput},
        // null, PlainZ2Vector.createOnes(1), true, true)).map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);

        // ### test
        BitVector[] permVector = new BitVector[piGiVector.length];
        for (int i = 0; i < piGiVector.length; i++) {
            permVector[i] = z2cReceiver.revealOwn(piGiVector[i]);
        }
        Vector<byte[]> permBytes = TransposeUtils.transposeMergeToVector(permVector);
        long[] perm = permBytes.stream().map(v -> BytesUtils.fixedByteArrayLength(v, 8)).mapToLong(LongUtils::byteArrayToLong).toArray();

        // get vector form of piGi
        piGi = TransposeUtils.transposeMergeToVector(Arrays.stream(piGiVector).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        // ### test
        // long[] perms2 = revealOwnLong(piGi.stream().map(v -> BytesUtils.fixedByteArrayLength(v, 8)).collect(Collectors.toCollection(Vector::new)));

        // get receiver's shared agg and e from psorter's input, which have been sorted.
        Vector<byte[]> trans = TransposeUtils.transposeMergeToVector(Arrays.stream(psorterInput)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        List<Vector<byte[]>> splitOwn = GroupAggUtils.split(trans, new int[]{1, receiverGroupByteLength});
        receiverGroupShare = splitOwn.get(1);

        // ### test
        // String[] doubSortedReceiverGroup = revealGroup(receiverGroupShare, receiverGroupBitLength);
        e = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> e.getBitVector().set(i, (splitOwn.get(0).get(i)[0] & 1) == 1));

        // ### test
        // BitVector bit3 = z2cReceiver.revealOwn(e);
    }

    private void applyPiGi() throws MpcAbortException {
        // now apply piGi to receiver's shared agg. the group and e of receiver have already been permuted
        aggShare = sharedPermutationReceiver.permute(piGi, aggShare);
        // ### test
        // long[] test = revealOwnLong(aggShare);

        // now apply piGi to sender's shared group, after which will be double sorted
        senderGroupShare = sharedPermutationReceiver.permute(piGi, senderGroupShare);

        // ### test
        // String[] doubSortedSenderGroup = revealGroup(senderGroupShare, senderGroupBitLength);
    }

    private Vector<byte[]> mergeGroup() {
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        return intStream.mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(senderGroupShare.get(i)).put(receiverGroupShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
    }

    private GroupAggOut aggregation(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException {
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

    private GroupAggOut sumAgg(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException {
        // agg
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField, flag);
        // reveal
        BitVector[] tmpAgg = z2cReceiver.revealOwn(agg.getAggsBinary());
        ZlVector aggResult = ZlVector.create(zl, ZlDatabase.create(envType, parallel, tmpAgg).getBigIntegerData());
        // ZlVector aggResult = zlcReceiver.revealOwn(agg.getAggs());
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

    private GroupAggOut maxAgg(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException {
        // agg
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField, flag);
        // reveal
        Preconditions.checkArgument(agg.getNum() == num, "size of output not correct");
        BitVector[] tmpAgg = z2cReceiver.revealOwn(agg.getAggsBinary());
        ZlVector aggResult = ZlVector.create(zl, ZlDatabase.create(envType, parallel, tmpAgg).getBigIntegerData());
        // ZlVector aggResult = zlcReceiver.revealOwn(agg.getAggs());
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

    protected Vector<byte[]> shareOther() {
        DataPacketHeader receiveSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_SHARES.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiveSharesPayload = rpc.receive(receiveSharesHeader).getPayload();
        return new Vector<>(receiveSharesPayload);
    }
}
