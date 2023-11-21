package edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting.BitmapSortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationSender;
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
 * Bitmap assist sorting-based group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/20
 */
public class BitmapSortingGroupAggSender extends AbstractGroupAggParty {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
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
     * Plain bit mux sender.
     */
    private final PlainPayloadMuxParty plainPayloadMuxReceiver;
    /**
     * Permutation receiver of reverse order.
     */
    private final PermutationReceiver reversePermutationReceiver;
    /**
     * Permutation sender.
     */
    private final PermutationSender permutationSender;
    /**
     * Permutation generation sender.
     */
    private final PermGenParty permGenSender;
    /**
     * A2b sender.
     */
    private final A2bParty a2bSender;
    /**
     * Own bit split.
     */
    private Vector<byte[]> eByte;
    /**
     * Server distinct groups.
     */
    private List<String> senderDistinctGroup;
    /**
     * bitmap shares
     */
    private SquareZ2Vector[] senderBitmapShares;

    private Vector<byte[]> piG0;

    private Vector<byte[]> rho;

    public BitmapSortingGroupAggSender(Rpc senderRpc, Party receiverParty, BitmapSortingGroupAggConfig config) {
        super(BitmapSortingGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        sharedPermutationSender = SharedPermutationFactory.createSender(senderRpc, receiverParty, config.getSharedPermutationConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        reversePermutationReceiver = PermutationFactory.createReceiver(senderRpc, receiverParty, config.getReversePermutationConfig());
        permutationSender = PermutationFactory.createSender(senderRpc, receiverParty, config.getPermutationConfig());
        permGenSender = PermGenFactory.createSender(senderRpc, receiverParty, config.getPermGenConfig());
        a2bSender = A2bFactory.createSender(senderRpc, receiverParty, config.getA2bConfig());
//        addSubPtos(osnReceiver);
//        addSubPtos(zlMuxSender);
//        addSubPtos(sharedPermutationSender);
//        addSubPtos(prefixAggSender);
//        addSubPtos(z2cSender);
//        addSubPtos(zlcSender);
//        addSubPtos(b2aSender);
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnSender.init(maxNum);
        zlMuxSender.init(maxNum);

        prefixAggSender.init(maxL, maxNum);
        z2cSender.init(maxL * maxNum);
        zlcSender.init(1);
        b2aSender.init(maxL, maxNum);
        reversePermutationReceiver.init(maxL, maxNum);
        sharedPermutationSender.init(maxNum);
        permutationSender.init(maxL, maxNum);
        permGenSender.init(maxNum, maxL);
        a2bSender.init(maxL, maxNum);
        plainPayloadMuxReceiver.init(maxNum);

        // generate distinct group
        senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));

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
        // bitmap
        bitmap();
        // sort, output pig0
        sort();
        // permute1, permute receiver's group,agg and sigmaB using pig0, output rho
        permute1();
        // permute2, permute sender's group using rho
        permute2();
        // permute3, permute e
        permute3();
        // merge group
        Vector<byte[]> mergedTwoGroup = mergeGroup();
        // b2a
        SquareZlVector otherAggB2a = b2a();
        // ### test
        zlcSender.revealOther(otherAggB2a);
        revealOtherGroup(mergedTwoGroup);
        z2cSender.revealOther(e);
        // agg
        aggregation(mergedTwoGroup, otherAggB2a, e);
        return null;
    }


    private void bitmap() throws MpcAbortException {
        // gen bitmap
        Vector<byte[]> bitmaps = genBitmap(groupAttr, e);
        // osn
        OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, bitmaps.get(0).length);
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        senderBitmapShares = Arrays.stream(transposed, 1, transposed.length).toArray(SquareZ2Vector[]::new);
        e = transposed[0];
        // and
        for (int i = 0; i < senderGroupNum; i++) {
            senderBitmapShares[i] = z2cSender.and(senderBitmapShares[i], e);
        }
        // mux
//        SquareZlVector aggShareVector = plainPayloadMuxReceiver.mux(e, null);
//        aggShare = Arrays.stream(aggShareVector.getZlVector().getElements()).map(v ->
//            LongUtils.longToByteArray(v.longValue())).collect(Collectors.toCollection(Vector::new));
//        revealOtherLong(aggShare);
    }

    private void sort() throws MpcAbortException {
        // merge e and bitmap as input
        SquareZ2Vector[] permInput = new SquareZ2Vector[senderGroupNum + 1];
        permInput[0] = e;
        System.arraycopy(senderBitmapShares, 0, permInput, 1, senderGroupNum);
        // sort
        SquareZlVector perm = permGenSender.sort(senderBitmapShares);
        // a2b
        SquareZ2Vector[] permB = a2bSender.a2b(perm);
        // transpose
        piG0 = TransposeUtils.transposeMergeToVector(Arrays.stream(permB).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));

        revealOtherLong(piG0);
    }

    private void permute1() throws MpcAbortException {
        // permute
        Vector<byte[]> output = reversePermutationReceiver.permute(piG0, receiverGroupByteLength + Long.BYTES + Integer.BYTES);
        // split
        List<Vector<byte[]>> split = GroupAggUtils.split(output, new int[]{receiverGroupByteLength, Long.BYTES, Integer.BYTES});
        receiverGroupShare = split.get(0);
        aggShare = split.get(1);
        rho = split.get(2);
    }

    private void permute2() throws MpcAbortException {
        senderGroupShare = GroupAggUtils.binaryStringToBytes(groupAttr);
        senderGroupShare = permutationSender.permute(rho, senderGroupShare);
    }

    private void permute3() throws MpcAbortException {
        Vector<byte[]> eByte = IntStream.range(0, num).mapToObj(i ->
            ByteBuffer.allocate(1).put(e.getBitVector().get(i) ? (byte) 1 : (byte) 0).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> permutedE = sharedPermutationSender.permute(rho, eByte);
        e = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> e.getBitVector().set(i, (permutedE.get(i)[0] & 1) == 1));
    }

    private SquareZ2Vector[] shareOwnBitmap(BitVector[] bitmap) {
        byte[][] ownShares = IntStream.range(0, senderGroupNum).mapToObj(i -> {
            byte[] bytes = new byte[CommonUtils.getByteLength(num)];
            secureRandom.nextBytes(bytes);
            return bytes;
        }).toArray(byte[][]::new);

        List<byte[]> otherShares = IntStream.range(0, senderGroupNum).mapToObj(i -> BytesUtils.xor(bitmap[i].getBytes(), ownShares[i])).collect(Collectors.toList());

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_BITMAP_SHARES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));

        return Arrays.stream(ownShares).map(v -> SquareZ2Vector.create(num, v, false)).toArray(SquareZ2Vector[]::new);
    }

    private Vector<byte[]> mergeGroup() {
        // merge group
        return IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(senderGroupShare.get(i)).put(receiverGroupShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
    }

    private SquareZlVector b2a() throws MpcAbortException {
        // b2a
        SquareZ2Vector[] transposed = Arrays.stream(TransposeUtils.transposeSplit(aggShare, Long.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        return zlMuxSender.mux(e, b2aSender.b2a(transposed));
    }

    private void aggregation(Vector<byte[]> groupField, SquareZlVector aggField, SquareZ2Vector flag) throws MpcAbortException {
        PrefixAggOutput agg = prefixAggSender.agg(groupField, aggField, null);
        // reveal
        zlcSender.revealOther(agg.getAggs());
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

    /**
     * Generate horizontal bitmaps.
     *
     * @param group group.
     * @return vertical bitmaps.
     */
    private Vector<byte[]> genBitmap(String[] group, SquareZ2Vector e) {
        return IntStream.range(0, group.length).mapToObj(i -> {
            byte[] bytes = new byte[CommonUtils.getByteLength(senderGroupNum + 1)];
            BinaryUtils.setBoolean(bytes, senderDistinctGroup.indexOf(group[i]) + 1, true);
            BinaryUtils.setBoolean(bytes, 0, e.getBitVector().get(i));
            return bytes;
        }).collect(Collectors.toCollection(Vector::new));
    }
}
