package edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
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
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Plain payload mux sender.
     */
    private final PlainPayloadMuxParty plainPayloadMuxSender;
    /**
     * Plain bit mux party.
     */
    private final PlainBitMuxParty plainBitMuxSender;
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
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(senderRpc, receiverParty, config.getOsnConfig());
        plainPayloadMuxSender = PlainPlayloadMuxFactory.createSender(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        plainBitMuxSender = PlainBitMuxFactory.createSender(senderRpc, receiverParty, config.getPlainBitMuxConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        sharedPermutationSender = SharedPermutationFactory.createSender(senderRpc, receiverParty, config.getSharedPermutationConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
        z2IntegerCircuit = new Z2IntegerCircuit(z2cSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        osnSender.init(maxNum);
        osnReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[][] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        assert aggField == null;
        // TODO 可能需要一个将string映射为定长结果的
        // set input
        //
        // merge group
        String[] mergedGroup = mergeString(groupField);
        // 包括了group和e
        Vector<byte[]> mergedGroupAndEBytes = IntStream.range(0, num).mapToObj(i -> ByteBuffer.wrap(mergedGroup[i].getBytes())
            .put(e.getBitVector().get(i) ? 1 : (byte) 0).array()).collect(Collectors.toCollection(Vector::new));
        // sigma_s
        int[] sigmaSPerm = obtainPerms(mergedGroup);
        receiveGroupByteLength();
        sendGroupByteLength();
        // apply
        Vector<byte[]> permutedGroupAndE = BenesNetworkUtils.permutation(sigmaSPerm, mergedGroupAndEBytes);
        // share
        Vector<byte[]> ownGroupAndE = shareOwn(permutedGroupAndE);
        // osn1, sender用sigma置换receiver的other group, long and bits
        Vector<byte[]> osnOutput1 = osnReceiver.osn(sigmaSPerm, otherGroupByteLength + Long.BYTES + 1).getShare();
        // pi 这里也要输入ei进行排序
        // split
        List<Vector<byte[]>> splits = split(osnOutput1, new int[]{otherGroupByteLength, Long.BYTES, 1});
        Vector<byte[]> splitGroup = splits.get(0);
        Vector<byte[]> splitLong = splits.get(1);
        Vector<byte[]> splitBit = splits.get(2);
        Vector<byte[]> mergedSortInput = merge(Arrays.asList(splitBit, splitGroup));
        SquareZ2Vector[] psorterInput = Arrays.stream(TransposeUtils.transposeSplit(mergedSortInput, (ownGroupByteLength + 1) * 8))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // 输入e和group进行排序
        SquareZ2Vector[] piGiVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{psorterInput},
            null, PlainZ2Vector.createOnes(num), true)).map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        // 转置piGiVector获得pi置换
        Vector<byte[]> piGi = TransposeUtils.transposeMergeToVector(Arrays.stream(piGiVector).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        ;
        // shared permutation, 这里做两次osn
        // 由于在排序时，对方的e和group已经排序，所以这里只需要排序long
        Vector<byte[]> permutedOtherLongs = sharedPermutationSender.permute(piGi, splitLong);
        // 由psorterInput获得对方receiver的bit和group.
        Vector<byte[]> trans = TransposeUtils.transposeMergeToVector(Arrays.stream(psorterInput).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));

        List<Vector<byte[]>> splitOther = split(trans, new int[]{1, otherGroupByteLength});
        Vector<byte[]> otherGroup = splitOther.get(1);
        SquareZ2Vector otherBit = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> otherBit.getBitVector().set(i, splitOther.get(0).get(i)[0] == (byte) 1));

        // 需要拆解为group,bit
        Vector<byte[]> permutedOwnInfos = sharedPermutationSender.permute(piGi, ownGroupAndE);
        List<Vector<byte[]>> splitOwn = split(permutedOwnInfos, new int[]{otherGroupByteLength, 1});
        Vector<byte[]> ownGroup = splitOwn.get(0);
        SquareZ2Vector ownBit = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> ownBit.getBitVector().set(i, splitOwn.get(1).get(i)[0] == (byte) 1));

        // merge group
        Vector<byte[]> mergedTwoGroup = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(ownGroupByteLength + otherGroupByteLength)
            .put(ownGroup.get(i)).put(otherGroup.get(i)).array()).collect(Collectors.toCollection(Vector::new));

        // b2a
        SquareZ2Vector[] transposed = Arrays.stream(TransposeUtils.transposeSplit(permutedOtherLongs.toArray(new byte[0][]), Long.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZlVector otherAggB2a = b2aSender.b2a(transposed);

        // agg
        PrefixAggOutput agg = prefixAggSender.agg(mergedTwoGroup, otherAggB2a);
        //  用置换后的e(ownBit)来mux
        agg.setAggs(zlMuxSender.mux(ownBit, agg.getAggs()));
        // reveal
        revealOtherOutput(agg);
        // TODO 最后输出可以用一个map来表示
        // TODO 输入时，应该把双方的group分组信息都作为明文输入进来
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

    protected void receiveGroupByteLength() {
        DataPacketHeader senderDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_GROUP_BYTE_LENGTH.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderDataSizePayload = rpc.receive(senderDataSizeHeader).getPayload();
        otherGroupByteLength = ByteBuffer.wrap(senderDataSizePayload.get(0)).getInt();
    }

    protected void sendGroupByteLength() {
        List<byte[]> receiverDataSizePayload = Collections.singletonList(ByteBuffer.allocate(4).putInt(ownGroupByteLength).array());
        DataPacketHeader receiverDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GROUP_BYTE_LENGTH.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverDataSizeHeader, receiverDataSizePayload));
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
    }

    private void revealOtherOutput(PrefixAggOutput prefixAggOutput) {
        List<byte[]> revealOtherOutputPayload = new ArrayList<>(prefixAggOutput.getGroupings());
        DataPacketHeader revealOtherOutputHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_OUTPUT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(revealOtherOutputHeader, revealOtherOutputPayload));
        zlcSender.revealOther(prefixAggOutput.getAggs());
    }
}
