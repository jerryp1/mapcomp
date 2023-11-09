package edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggUtils;
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
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mix group aggregation receiver.
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
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Plain payload mux receiver.
     */
    private final PlainPayloadMuxParty plainPayloadMuxReceiver;
    /**
     * Plain bit mux receiver.
     */
    private final PlainBitMuxParty plainBitMuxReceiver;
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

    public SortingGroupAggReceiver(Rpc receiverRpc, Party senderParty, SortingGroupAggConfig config) {
        super(SortingGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnSender = OsnFactory.createSender(receiverRpc, senderParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(receiverRpc, senderParty, config.getPlainPayloadMuxConfig());
        plainBitMuxReceiver = PlainBitMuxFactory.createReceiver(receiverRpc, senderParty, config.getPlainBitMuxConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        sharedPermutationReceiver = SharedPermutationFactory.createReceiver(receiverRpc, senderParty, config.getSharedPermutationConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        b2aReceiver = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
        z2IntegerCircuit = new Z2IntegerCircuit(z2cReceiver);
        secureRandom = new SecureRandom();
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        osnReceiver.init(maxNum);
        osnSender.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    // tranpose 4, split 3
    //
    @Override
    public GroupAggOut groupAgg(String[] groupField, final long[] aggField, final SquareZ2Vector e) throws MpcAbortException {
        // TODO 这里之前要想办法让两边数据填充到一样的长度并对其
        assert aggField != null;
        // set input
        setPtoInput(groupField, aggField, e);
        // merge group
        Vector<byte[]> mergedGroupBytes = Arrays.stream(groupField).map(String::getBytes).collect(Collectors.toCollection(Vector::new));
//        sendGroupByteLength();
//        receiveGroupByteLength();
        Vector<byte[]> senderGroupsAndE = shareOther();
        // osn1
        Vector<byte[]> osnInput1 = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(receiverGroupByteLength + Long.BYTES + 1)
            .put(mergedGroupBytes.get(i)).put(LongUtils.longToByteArray(aggField[i]))
            .put(e.getBitVector().get(i) ? 1 : (byte) 0).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> osnOutput1 = osnSender.osn(osnInput1, receiverGroupByteLength + Long.BYTES + 1).getShare();
        // 拆解
        List<Vector<byte[]>> splitOsn1 = GroupAggUtils.split(osnOutput1, new int[]{receiverGroupByteLength, Long.BYTES, 1});
        Vector<byte[]> ownGroupSplit = splitOsn1.get(0);
        Vector<byte[]> ownPayloadSplit = splitOsn1.get(1);
        Vector<byte[]> ownBitSplit = splitOsn1.get(2);
        Vector<byte[]> mergedPsorterInput = merge(Arrays.asList(ownBitSplit, ownGroupSplit));
        SquareZ2Vector[] psorterInput = Arrays.stream(TransposeUtils.transposeSplit(mergedPsorterInput, (receiverGroupByteLength + 1) * 8))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        // stable sorting ,输入receiver的e和group
        SquareZ2Vector[] piGiVector = Arrays.stream(z2IntegerCircuit.psort(new SquareZ2Vector[][]{psorterInput},
            null, PlainZ2Vector.createOnes(num), true)).map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        // 获得 stable sorting的置换
        Vector<byte[]> piGi = TransposeUtils.transposeMergeToVector(Arrays.stream(piGiVector).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        // 置换long
        Vector<byte[]> permutedOwnPayload = sharedPermutationReceiver.permute(piGi, ownPayloadSplit);
        // 由psorterInput获得bit和group.
        Vector<byte[]> trans = TransposeUtils.transposeMergeToVector(Arrays.stream(psorterInput).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        List<Vector<byte[]>> splitOwn = GroupAggUtils.split(trans, new int[]{1, receiverGroupByteLength});
        Vector<byte[]> ownGroup = splitOwn.get(1);
        SquareZ2Vector ownBit = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> ownBit.getBitVector().set(i, splitOwn.get(0).get(i)[0] == (byte) 1));
        // split 拆解为group,bit
        Vector<byte[]> permutedOtherInfos = sharedPermutationReceiver.permute(piGi, senderGroupsAndE);
        List<Vector<byte[]>> splitOther = GroupAggUtils.split(permutedOtherInfos, new int[]{senderGroupByteLength, 1});
        Vector<byte[]> otherGroup = splitOther.get(0);
        SquareZ2Vector otherBit = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> otherBit.getBitVector().set(i, splitOther.get(1).get(i)[0] == (byte) 1));
        // merge group
        Vector<byte[]> mergedTwoGroup = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(otherGroup.get(i)).put(ownGroup.get(i)).array()).collect(Collectors.toCollection(Vector::new));
        // b2a
        SquareZ2Vector[] transposed = Arrays.stream(TransposeUtils.transposeSplit(permutedOwnPayload.toArray(new byte[0][]), Long.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZlVector otherAggB2a = b2aReceiver.b2a(transposed);

        // agg
        PrefixAggOutput agg = prefixAggReceiver.agg(mergedTwoGroup, otherAggB2a);
        // 用置换后的e(ownBit)来mux
        agg.setAggs(zlMuxReceiver.mux(ownBit, agg.getAggs()));
        return null;
    }

//    @Override
//    public GroupAggOut groupAgg(String[][] groupField, final long[] aggField, final SquareZ2Vector e) throws MpcAbortException {
//        assert aggField !=null;
//        // set input
//        //
//        // merge group
//        String[] mergedGroup = mergeString(groupField);
//        // osn1 放入group、payload、e
//        Vector<byte[]> osnInput1 = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(ownGroupByteLength + 1)
//            .put(mergedGroup[i].getBytes())).put(e.getBitVector().get(i) ? 1 : (byte)0).array()).collect(Collectors.toCollection(Vector::new));
//        sendGroupByteLength();
//        receiveGroupByteLength();
//        OsnPartyOutput osnPartyOutput1 = osnSender.osn(osnInput1, ownGroupByteLength + Long.BYTES);
//        // 取出
//        Vector<byte[]> groupByte = new Vector<>();
//        Vector<byte[]> longByte = new Vector<>();
//
//        // stable sorting
//        Vector<byte[]> piGi =null;
//        // shared permutation, input with empty input byte array.
//        Vector<byte[]> emptyInput = IntStream.range(0, num).mapToObj(i -> new byte[otherGroupByteLength]).collect(Collectors.toCollection(Vector::new));
//        Vector<byte[]> permOut = sharedPermutationReceiver.permute(piGi, emptyInput);
//        // osn2
//        OsnPartyOutput osnPartyOutput2 = osnSender.osn(piGi, piGi.get(0).length);
//        Vector<byte[]> alpha = IntStream.range(0, num).mapToObj(osnPartyOutput2::getShare).collect(Collectors.toCollection(Vector::new));
//        // receive beta
//        int[] beta = receiveBeta();
//        Vector<byte[]> u1 = BenesNetworkUtils.permutation(beta, Arrays.stream(aggField).mapToObj(LongUtils::longToByteArray).collect(Collectors.toCollection(Vector::new)));
//        // osn3
//        Vector<byte[]> osnPartyOutput3 = osnSender.osn(u1,Long.BYTES).getShare();
//        // osn4
//        Vector<byte[]> osnPartyOutput4 = osnReceiver.osn();
//
//        // group indicator.
//
//
//
//        return null;
//    }

    protected void sendGroupByteLength() {
        List<byte[]> receiverDataSizePayload = Collections.singletonList(ByteBuffer.allocate(4).putInt(receiverGroupByteLength).array());
        DataPacketHeader receiverDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_GROUP_BYTE_LENGTH.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverDataSizeHeader, receiverDataSizePayload));
    }

//    protected void receiveGroupByteLength() {
//        DataPacketHeader senderDataSizeHeader = new DataPacketHeader(
//            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GROUP_BYTE_LENGTH.ordinal(), extraInfo,
//            otherParties()[0].getPartyId(), ownParty().getPartyId()
//        );
//        List<byte[]> senderDataSizePayload = rpc.receive(senderDataSizeHeader).getPayload();
//        otherGroupByteLength = ByteBuffer.wrap(senderDataSizePayload.get(0)).getInt();
//    }

//    protected int[] receiveBeta() {
//        DataPacketHeader senderDataSizeHeader = new DataPacketHeader(
//            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_BETA.ordinal(), extraInfo,
//            otherParties()[0].getPartyId(), ownParty().getPartyId()
//        );
//        List<byte[]> senderDataSizePayload = rpc.receive(senderDataSizeHeader).getPayload();
//        ByteBuffer buffer = ByteBuffer.wrap(senderDataSizePayload.get(0));
//        int[] result = new int[num];
//        for (int i = 0; i < num; i++) {
//            result[i] = buffer.getInt();
//        }
//        return result;
//    }

    protected Vector<byte[]> shareOther() {
        DataPacketHeader receiveSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_SHARES.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiveSharesPayload = rpc.receive(receiveSharesHeader).getPayload();
        return new Vector<>(receiveSharesPayload);
    }

    private GroupAggOut revealOwnOutput(PrefixAggOutput prefixAggOutput) throws MpcAbortException {
        DataPacketHeader revealOutputHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_SHARES.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> revealOutputPayload = rpc.receive(revealOutputHeader).getPayload();
        String[] group = IntStream.range(0, num).mapToObj(i ->
            new String(BytesUtils.xor(revealOutputPayload.get(i), prefixAggOutput.getGroupings().get(i)))).toArray(String[]::new);
        ZlVector agg = zlcReceiver.revealOwn(prefixAggOutput.getAggs());
        return new GroupAggOut(group, agg.getElements());
    }
}
