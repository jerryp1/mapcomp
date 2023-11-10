package edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
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
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
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
 * Mix group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class SortingGroupAggSender extends AbstractGroupAggParty {
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Zl mux party.
     */
    private final ZlMuxParty zlMuxSender;
    /**
     * Shared permutation sender.
     */
    private final SharedPermutationParty sharedPermutationSender;
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
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit z2IntegerCircuit;

    public SortingGroupAggSender(Rpc senderRpc, Party receiverParty, SortingGroupAggConfig config) {
        super(SortingGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnReceiver = OsnFactory.createReceiver(senderRpc, receiverParty, config.getOsnConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        sharedPermutationSender = SharedPermutationFactory.createSender(senderRpc, receiverParty, config.getSharedPermutationConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
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
        sharedPermutationSender.init(maxNum);
        prefixAggSender.init(maxL, maxNum);
        z2cSender.init(maxL * maxNum);
        zlcSender.init(1);
        b2aSender.init(maxL, maxNum);


        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        assert aggField == null;
        // set input
        setPtoInput(groupField, aggField, e);
        // sigma_s
        int[] sigmaSPerm = obtainPerms(groupField);
        // osn1, sender permute receiver's group, agg and e
        Vector<byte[]> osnOutput1 = osnReceiver.osn(sigmaSPerm, receiverGroupBitLength + Long.BYTES + 1).getShare();

        // pi 这里也要输入e进行排序
        // split
        List<Vector<byte[]>> splits = GroupAggUtils.split(osnOutput1, new int[]{receiverGroupBitLength, Long.BYTES, 1});
        Vector<byte[]> splitGroup = splits.get(0);
        Vector<byte[]> splitLong = splits.get(1);

        Vector<byte[]> splitBit = splits.get(2);

        Vector<byte[]> senderE = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(1).put((e.getBitVector().get(i) ? (byte) 1 : (byte) 0)).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> tempee = BenesNetworkUtils.permutation(sigmaSPerm, senderE);
        senderE = IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(splitBit.get(i), tempee.get(i))).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> senderGroup = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(senderGroupBitLength).put(groupField[i].getBytes())
            .array()).collect(Collectors.toCollection(Vector::new));
        senderGroup = BenesNetworkUtils.permutation(sigmaSPerm, senderGroup);

        // test
        revealOtherBit(senderE);

        // share own group
        Vector<byte[]> sortedSenderGroup = shareOwn(senderGroup);
        revealOtherGroup(sortedSenderGroup);

        // receiver方osn后的输出，组织为sorter的输入
        Vector<byte[]> mergedSortInput = merge(Arrays.asList(senderE, splitGroup));

        // prepare psorter input, with shared e and group of receiver
        SquareZ2Vector[] psorterInput = Arrays.stream(TransposeUtils.transposeSplit(mergedSortInput, (receiverGroupBitLength + 1) * 8))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // psorter
        SquareZ2Vector[] piGiVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{psorterInput},
            null, PlainZ2Vector.createOnes(1), true)).map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        // test
        for (int i = 0; i < piGiVector.length; i++) {
            z2cSender.revealOther(piGiVector[i]);
        }

        // get vector form of piGi
        Vector<byte[]> piGi = TransposeUtils.transposeMergeToVector(Arrays.stream(piGiVector).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));

        // test
        revealOtherLong(piGi.stream().map(v -> BytesUtils.fixedByteArrayLength(v, 8)).collect(Collectors.toCollection(Vector::new)));


        // now apply piGi to receiver's shared agg. the group and e of receiver have already been permuted
        Vector<byte[]> permutedOtherLongs = sharedPermutationSender.permute(piGi, splitLong);
        revealOtherLong(permutedOtherLongs);

        // get receiver's shared agg and e from psorter's input, which have been sorted.
        Vector<byte[]> trans = TransposeUtils.transposeMergeToVector(Arrays.stream(psorterInput).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));

        List<Vector<byte[]>> splitOther = GroupAggUtils.split(trans, new int[]{1, receiverGroupBitLength});
        Vector<byte[]> otherGroup = splitOther.get(1);
        // test
        revealOtherGroup(otherGroup);

        SquareZ2Vector otherBit = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> otherBit.getBitVector().set(i, (splitOther.get(0).get(i)[0] & 1) == 1));
        revealOtherBit(splitOther.get(0));

        // 排序后的group
        Vector<byte[]> doubleSortedSenderGroup = sharedPermutationSender.permute(piGi, sortedSenderGroup);

        revealOtherGroup(doubleSortedSenderGroup);

        // merge group
        Vector<byte[]> mergedTwoGroup = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupBitLength)
            .put(doubleSortedSenderGroup.get(i)).put(otherGroup.get(i)).array()).collect(Collectors.toCollection(Vector::new));

        // b2a
        SquareZ2Vector[] transposed = Arrays.stream(TransposeUtils.transposeSplit(permutedOtherLongs, Long.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZlVector otherAggB2a = b2aSender.b2a(transposed);
        // mux with e
//        otherAggB2a = zlMuxSender.mux(ownBit, otherAggB2a);
        zlcSender.revealOther(otherAggB2a);
        revealOtherGroup(mergedTwoGroup);
        // agg
        PrefixAggOutput agg = prefixAggSender.agg(mergedTwoGroup, otherAggB2a);
        revealOtherGroup(agg.getGroupings());
        Preconditions.checkArgument(agg.getNum() == num, "size of output not correct");
        // reveal
        zlcSender.revealOther(agg.getAggs());

        return null;
    }

    protected Vector<byte[]> shareOwn(Vector<byte[]> input) {
        Vector<byte[]> ownShares = IntStream.range(0, input.size()).mapToObj(i -> {
            byte[] bytes = new byte[input.get(0).length];
            secureRandom.nextBytes(bytes);
            return bytes;
        }).collect(Collectors.toCollection(Vector::new));

        List<byte[]> otherShares = IntStream.range(0, input.size()).mapToObj(i -> BytesUtils.xor(input.get(i), ownShares.get(i))).collect(Collectors.toList());

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_SHARES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));

        return ownShares;
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

//    @Override
//    public GroupAggOut groupAgg(String[][] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
//        assert aggField == null;
//        // set input
//        //
//        // merge group
//        String[] mergedGroup = mergeString(groupField);
//        Vector<byte[]> mergedGroupBytes =Arrays.stream(mergedGroup).map(String::getBytes).collect(Collectors.toCollection(Vector::new));
//        // sigma_s
//        int[] sigmaSPerm = obtainPerms(mergedGroup);
//        // osn1 byteLength是分组长度和字段长度
//        receiveGroupByteLength();
//        sendGroupByteLength();
//        OsnPartyOutput osnPartyOutput1 = osnReceiver.osn(sigmaSPerm, otherGroupByteLength  + 1);
//        // stable sorting TODO
//        Vector<byte[]> piGi =null;
//        // apply sigmaS to own group
//        Vector<byte[]> permutedGroup = BenesNetworkUtils.permutation(sigmaSPerm,
//            mergedGroupBytes);
//        // shared permutation
//        sharedPermutationSender.permute(piGi, permutedGroup);
//        // osn2
//        int[] rou = genRandomPerm(num);
//        OsnPartyOutput osnPartyOutput2 = osnReceiver.osn(rou, piGi.get(0).length);
//        Vector<byte[]> permsPiGi = BenesNetworkUtils.permutation(rou, piGi);
//        Vector<byte[]> alpha = IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(permsPiGi.get(i), osnPartyOutput2.getShare(i))).collect(Collectors.toCollection(Vector::new));
//        // phi
//        int[] phi = genRandomPerm(num);
//        // send beta  TODO
//        int[] beta = null;
//        sendBeta(beta);
//        // osn3
//        int[] phiReverse = ShuffleUtils.reversePermutation(phi);
//        OsnPartyOutput osnPartyOutput3 = osnReceiver.osn(combinePerm(phiReverse,beta), Long.BYTES);
//        // osn4
//        OsnPartyOutput osnPartyOutput4 = osnSender.osn(ShuffleUtils.reversePermutation(rou), );
//
//
//
//        return null;
//    }

//    protected void receiveGroupByteLength() {
//        DataPacketHeader senderDataSizeHeader = new DataPacketHeader(
//            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_GROUP_BYTE_LENGTH.ordinal(), extraInfo,
//            otherParties()[0].getPartyId(), ownParty().getPartyId()
//        );
//        List<byte[]> senderDataSizePayload = rpc.receive(senderDataSizeHeader).getPayload();
//        otherGroupByteLength = ByteBuffer.wrap(senderDataSizePayload.get(0)).getInt();
//    }

    protected void sendGroupByteLength() {
        List<byte[]> receiverDataSizePayload = Collections.singletonList(ByteBuffer.allocate(4).putInt(senderGroupByteLength).array());
        DataPacketHeader receiverDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GROUP_BYTE_LENGTH.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverDataSizeHeader, receiverDataSizePayload));
        extraInfo++;
    }


    protected void sendBeta(int[] beta) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * beta.length);
        for (int i = 0; i < beta.length; i++) {
            buffer.putInt(beta[i]);
        }
        List<byte[]> receiverDataSizePayload = Collections.singletonList(buffer.array());
        DataPacketHeader receiverDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverDataSizeHeader, receiverDataSizePayload));
        extraInfo++;
    }

//    private void revealOtherOutput(PrefixAggOutput prefixAggOutput) {
//        List<byte[]> revealOtherOutputPayload = new ArrayList<>(prefixAggOutput.getGroupings());
//        DataPacketHeader revealOtherOutputHeader = new DataPacketHeader(
//            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_OUTPUT.ordinal(), extraInfo,
//            ownParty().getPartyId(), otherParties()[0].getPartyId()
//        );
//        rpc.send(DataPacket.fromByteArrayList(revealOtherOutputHeader, revealOtherOutputPayload));
//        zlcSender.revealOther(prefixAggOutput.getAggs());
//    }
}
