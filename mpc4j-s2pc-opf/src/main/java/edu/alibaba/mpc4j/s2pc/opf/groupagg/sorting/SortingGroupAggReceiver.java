package edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting;

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
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;

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
 * @author Li Peng
 * @date 2023/11/3
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

    public SortingGroupAggReceiver(Rpc receiverRpc, Party senderParty, SortingGroupAggConfig config) {
        super(SortingGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnSender = OsnFactory.createSender(receiverRpc, senderParty, config.getOsnConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        sharedPermutationReceiver = SharedPermutationFactory.createReceiver(receiverRpc, senderParty, config.getSharedPermutationConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        b2aReceiver = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());

//        addSubPtos(osnSender);
//        addSubPtos(zlMuxReceiver);
//        addSubPtos(sharedPermutationReceiver);
//        addSubPtos(prefixAggReceiver);
//        addSubPtos(z2cReceiver);
//        addSubPtos(zlcReceiver);
//        addSubPtos(b2aReceiver);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cReceiver);
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
        SquareZlVector receiverAggAs = b2a();
        // ### test
        String[] groupResult = revealOwnGroup(mergedTwoGroup);
        ZlVector zlVector = zlcReceiver.revealOwn(receiverAggAs);
        // aggregation
        return aggregation(mergedTwoGroup, receiverAggAs, e);
    }

    private void osn1(String[] groupAttr, long[] aggAttr) throws MpcAbortException {
        // merge group // TODO 这里可以考虑写一个 string to bytes的encode和decode，减少通信量
        Vector<byte[]> groupBytes = Arrays.stream(groupAttr).map(String::getBytes).collect(Collectors.toCollection(Vector::new));
        // osn1
        Vector<byte[]> osnInput1 = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(receiverGroupBitLength + Long.BYTES + 1)
            .put(groupBytes.get(i)).put(LongUtils.longToByteArray(aggAttr[i]))
            .put(e.getBitVector().get(i) ? (byte) 1 : (byte) 0).array()).collect(Collectors.toCollection(Vector::new));

        Vector<byte[]> osnOutput1 = osnSender.osn(osnInput1, receiverGroupBitLength + Long.BYTES + 1).getShare();
        // split
        List<Vector<byte[]>> splitOsn1 = GroupAggUtils.split(osnOutput1, new int[]{receiverGroupBitLength, Long.BYTES, 1});
        receiverGroupShare = splitOsn1.get(0);
        aggShare = splitOsn1.get(1);
        eByte = splitOsn1.get(2);

        // ### test ownBit
        List<byte[]> ownBit = revealOwnBit(eByte);

        // share
        senderGroupShare = shareOther();
        // ### test
        String[] senderGroup = revealOwnGroup(senderGroupShare);
    }

    private void pSorter() throws MpcAbortException {
        // generate input of psorter
        Vector<byte[]> mergedPsorterInput = merge(Arrays.asList(eByte, receiverGroupShare));
        // prepare psorter input, with shared e and group of receiver
        SquareZ2Vector[] psorterInput = Arrays.stream(TransposeUtils.transposeSplit(mergedPsorterInput, (receiverGroupBitLength + 1) * 8))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        // psorter
        SquareZ2Vector[] piGiVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{psorterInput},
            null, PlainZ2Vector.createOnes(1), true)).map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);

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
        long[] perms2 = revealOwnLong(piGi.stream().map(v -> BytesUtils.fixedByteArrayLength(v, 8)).collect(Collectors.toCollection(Vector::new)));

        // get receiver's shared agg and e from psorter's input, which have been sorted.
        Vector<byte[]> trans = TransposeUtils.transposeMergeToVector(Arrays.stream(psorterInput)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        List<Vector<byte[]>> splitOwn = GroupAggUtils.split(trans, new int[]{1, receiverGroupBitLength});
        receiverGroupShare = splitOwn.get(1);

        // ### test
        String[] doubSortedReceiverGroup = revealOwnGroup(receiverGroupShare);
        e = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> e.getBitVector().set(i, (splitOwn.get(0).get(i)[0] & 1) == 1));

        // ### test
        BitVector bit3 = z2cReceiver.revealOwn(e);
    }

    private void applyPiGi() throws MpcAbortException {
        // now apply piGi to receiver's shared agg. the group and e of receiver have already been permuted
        aggShare = sharedPermutationReceiver.permute(piGi, aggShare);
        // ### test
        long[] test = revealOwnLong(aggShare);

        // now apply piGi to sender's shared group, after which will be double sorted
        senderGroupShare = sharedPermutationReceiver.permute(piGi, senderGroupShare);

        // ### test
        String[] doubSortedSenderGroup = revealOwnGroup(senderGroupShare);
        System.out.println(123);
    }

    private Vector<byte[]> mergeGroup() {
        return IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupBitLength)
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
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField, flag);
        // reveal
        Preconditions.checkArgument(agg.getNum() == num, "size of output not correct");
        ZlVector aggResult = zlcReceiver.revealOwn(agg.getAggs());
        String[] truetureGroup = revealOwnGroup(agg.getGroupings());

        return new GroupAggOut(truetureGroup, aggResult.getElements());
    }

    private String[] revealOwnGroup(Vector<byte[]> ownGroup) {
        DataPacketHeader groupHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_OUTPUT.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderDataSizePayload = rpc.receive(groupHeader).getPayload();
        extraInfo++;
        Preconditions.checkArgument(senderDataSizePayload.size() == num, "group num not match");
        return IntStream.range(0, num).mapToObj(i -> new String(BytesUtils.xor(senderDataSizePayload.get(i), ownGroup.get(i)))).toArray(String[]::new);
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
