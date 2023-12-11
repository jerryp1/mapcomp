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
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;

/**
 * Bitmap assist sorting-based group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/20
 */
public class BitmapSortingGroupAggSender extends AbstractGroupAggParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapSortingGroupAggSender.class);

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
    private final Z2MuxParty z2MuxParty;
    /**
     * Own bit split.
     */
    private Vector<byte[]> eByte;
    /**
     * Server distinct groups.
     */
    private List<String> senderDistinctGroup;

    private Map<String, Integer> senderGroupMap;
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
        z2MuxParty = Z2MuxFactory.createSender(senderRpc, receiverParty, config.getZ2MuxConfig());
        addMultipleSubPtos(osnSender, zlMuxSender, sharedPermutationSender, prefixAggSender, z2cSender, zlcSender,
            b2aSender, plainPayloadMuxReceiver, reversePermutationReceiver, permutationSender, permGenSender, a2bSender, z2MuxParty);
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
        permGenSender.init(maxNum, senderGroupNum);
        a2bSender.init(maxL, maxNum);
        plainPayloadMuxReceiver.init(maxNum);
        long totalMuxNum = ((long) maxNum) <<(senderGroupBitLength);
        int maxMuxInput = (int) Math.min(Integer.MAX_VALUE, totalMuxNum);
        z2MuxParty.init(maxMuxInput);

        // generate distinct group
        senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));
        senderGroupMap = new HashMap<>();
        for (int i = 0; i < senderGroupNum; i++) {
            senderGroupMap.put(senderDistinctGroup.get(i), i);
        }

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
        // group
        if (aggField == null) {
            group();
        } else {
            groupWithSenderAgg();
        }
        // agg
        agg();
        return null;
    }

    private void group() throws MpcAbortException {
        // bitmap
        stopWatch.start();
        groupTripleNum = TRIPLE_NUM;
        LOGGER.info("bitmap");
        bitmap();
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // sort, output pig0
        stopWatch.start();
        LOGGER.info("sort");
        sort();
        stopWatch.stop();
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute1, permute receiver's group,agg and sigmaB using pig0, output rho
        stopWatch.start();
        LOGGER.info("permute1");
        permute1();
        stopWatch.stop();
        groupStep3Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute2, permute sender's group using rho
        stopWatch.start();
        LOGGER.info("permute2");
        permute2();
        stopWatch.stop();
        groupStep4Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute3, permute e
        stopWatch.start();
        LOGGER.info("permute3");
        permute3();
        stopWatch.stop();
        groupStep5Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
    }

    private void groupWithSenderAgg() throws MpcAbortException {
        // bitmap
        stopWatch.start();
        groupTripleNum = TRIPLE_NUM;
        LOGGER.info("bitmap");
        bitmap();
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // sort, output pig0
        stopWatch.start();
        LOGGER.info("sort");
        sort();
        stopWatch.stop();
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute1, permute receiver's group,agg and sigmaB using pig0, output rho
        stopWatch.start();
        LOGGER.info("permute1");
        permute1WithSenderAgg();
        stopWatch.stop();
        groupStep3Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute2, permute sender's group using rho
        stopWatch.start();
        LOGGER.info("permute2");
        permute2WithSenderAgg();
        stopWatch.stop();
        groupStep4Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute3, permute e
        stopWatch.start();
        LOGGER.info("permute3");
        permute3();
        stopWatch.stop();
        groupStep5Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
    }

    private void agg() throws MpcAbortException {
        // merge group
        Vector<byte[]> mergedTwoGroup = mergeGroup();
        // b2a
//        SquareZlVector otherAggB2a = b2a();
        SquareZ2Vector[] otherAggB2a = getAggAttr();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
        // ### test
//        zlcSender.revealOther(otherAggB2a);
//        revealOtherGroup(mergedTwoGroup);
//        z2cSender.revealOther(e);
        // agg
        stopWatch.start();
        aggTripleNum = TRIPLE_NUM;
        LOGGER.info("agg");
        aggregation(mergedTwoGroup, otherAggB2a, e);
        stopWatch.stop();
        aggTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        aggTripleNum = TRIPLE_NUM - aggTripleNum;
    }

    private void bitmap() throws MpcAbortException {
        // gen bitmap
        LOGGER.info("bitmap0");

        Vector<byte[]> bitmaps = genBitmap(groupAttr, e);
        // osn
        LOGGER.info("osn0");
        OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, bitmaps.get(0).length);
        // transpose
        LOGGER.info("transpose0");
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        senderBitmapShares = Arrays.stream(transposed, 1, transposed.length).toArray(SquareZ2Vector[]::new);
        e = transposed[0];
        // and
        LOGGER.info("transpose1");

        int threashold = 500;
        int unit = 32;
//        senderBitmapShares = new SquareZ2Vector[senderGroupNum];
        if (senderGroupNum > threashold) {
            int num = CommonUtils.getUnitNum(senderGroupNum, unit);
            for (int i = 0; i < num; i++) {
                int len = (i == num - 1) ? senderGroupNum - i * unit : unit;
                SquareZ2Vector[] temp = new SquareZ2Vector[len];
                System.arraycopy(senderBitmapShares, i * unit , temp, 0, len);
                LOGGER.info("i:" + i);
                temp = z2MuxParty.mux(e, temp);
                System.arraycopy(temp, 0 , senderBitmapShares, i * unit, len);
            }
        }
//        senderBitmapShares = z2MuxParty.mux(e, senderBitmapShares);
//        for (int i = 0; i < senderGroupNum; i++) {
//            senderBitmapShares[i] = z2cSender.and(senderBitmapShares[i], e);
//        }
//        z2cSender.revealOther(e);
//        System.out.println(123);
    }

    private void sort() throws MpcAbortException {
        // sort
        SquareZlVector perm = permGenSender.sort(senderBitmapShares);
        // a2b
        SquareZ2Vector[] permB = a2bSender.a2b(perm);
        // transpose
        piG0 = TransposeUtils.transposeMergeToVector(Arrays.stream(permB).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));

//        revealOtherLong(piG0);
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

    private void permute1WithSenderAgg() throws MpcAbortException {
        // permute
        Vector<byte[]> output = reversePermutationReceiver.permute(piG0, receiverGroupByteLength + Integer.BYTES);
        // split
        List<Vector<byte[]>> split = GroupAggUtils.split(output, new int[]{receiverGroupByteLength, Integer.BYTES});
        receiverGroupShare = split.get(0);
        rho = split.get(1);
    }

    private void permute2() throws MpcAbortException {
        senderGroupShare = GroupAggUtils.binaryStringToBytes(groupAttr);
        senderGroupShare = permutationSender.permute(rho, senderGroupShare);
    }

    private void permute2WithSenderAgg() throws MpcAbortException {
        senderGroupShare = GroupAggUtils.binaryStringToBytes(groupAttr);
        aggShare = Arrays.stream(aggAttr).mapToObj(LongUtils::longToByteArray).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> input = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(senderGroupByteLength + Long.BYTES)
            .put(senderGroupShare.get(i)).put(aggShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> output = permutationSender.permute(rho, input);
        List<Vector<byte[]>> split = GroupAggUtils.split(output,new int[]{senderGroupByteLength,  Long.BYTES});
        senderGroupShare = split.get(0);
        aggShare = split.get(1);
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

//    private void aggregation(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException {
//        PrefixAggOutput agg = prefixAggSender.agg(groupField, aggField, null);
//        // reveal
////        zlcSender.revealOther(agg.getAggs());
//        z2cSender.revealOther(agg.getAggsBinary());
//        revealOtherGroup(agg.getGroupings());
//        z2cSender.revealOther(agg.getIndicator());
//
//        Preconditions.checkArgument(agg.getNum() == num, "size of output not correct");
//    }

    private void aggregation(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException {
        PrefixAggOutput agg = prefixAggSender.agg(groupField, aggField, null);
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
            BinaryUtils.setBoolean(bytes, senderGroupMap.get(group[i]) + 1, true);
            BinaryUtils.setBoolean(bytes, 0, e.getBitVector().get(i));
            return bytes;
        }).collect(Collectors.toCollection(Vector::new));
    }
}
