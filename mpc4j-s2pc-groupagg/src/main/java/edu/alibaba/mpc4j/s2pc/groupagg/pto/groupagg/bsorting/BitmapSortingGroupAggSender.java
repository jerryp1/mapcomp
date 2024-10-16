package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bsorting;

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
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationSender;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.PermGenFactory;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.PermGenParty;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bsorting.BitmapSortingGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;
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
 */
public class BitmapSortingGroupAggSender extends AbstractGroupAggParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapSortingGroupAggSender.class);
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
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
     * z2 mux sender.
     */
    private final Z2MuxParty z2MuxSender;
    /**
     * A map relation between group value and its index.
     */
    private Map<String, Integer> senderGroupMap;
    /**
     * bitmap shares
     */
    private SquareZ2Vector[] senderBitmapShares;

    private Vector<byte[]> piG0;

    private Vector<byte[]> rho;

    private int maxBatchNum;

    public BitmapSortingGroupAggSender(Rpc senderRpc, Party receiverParty, BitmapSortingGroupAggConfig config) {
        super(BitmapSortingGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        sharedPermutationSender = SharedPermutationFactory.createSender(senderRpc, receiverParty, config.getSharedPermutationConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        reversePermutationReceiver = PermutationFactory.createReceiver(senderRpc, receiverParty, config.getReversePermutationConfig());
        permutationSender = PermutationFactory.createSender(senderRpc, receiverParty, config.getPermutationConfig());
        permGenSender = PermGenFactory.createSender(senderRpc, receiverParty, config.getPermGenConfig());
        a2bSender = A2bFactory.createSender(senderRpc, receiverParty, config.getA2bConfig());
        z2MuxSender = Z2MuxFactory.createSender(senderRpc, receiverParty, config.getZ2MuxConfig());

        maxBatchNum = config.getMaxBatchNum();
        addMultipleSubPtos(zlcSender, osnSender, sharedPermutationSender, prefixAggSender, z2cSender,
            reversePermutationReceiver, permutationSender, permGenSender, a2bSender, z2MuxSender);
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        z2cSender.init(maxL * maxNum);
        prefixAggSender.init(maxL, maxNum);
        osnSender.init(maxNum);
        zlcSender.init(1);
        reversePermutationReceiver.init(maxL, maxNum);
        sharedPermutationSender.init(maxNum);
        permutationSender.init(maxL, maxNum);
        permGenSender.init(maxNum, senderGroupNum);
        a2bSender.init(maxL, maxNum);
        long totalMuxNum = ((long) maxNum) << (senderGroupBitLength);
        int maxMuxInput = (int) Math.min(Integer.MAX_VALUE, totalMuxNum);
        z2MuxSender.init(maxMuxInput);

        // generate distinct group
        List<String> senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));
        senderGroupMap = new HashMap<>(senderGroupNum);
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
        SquareZ2Vector[] otherAggB2a = getAggAttr();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
        // ### test
        // zlcSender.revealOther(otherAggB2a);
        // revealOtherGroup(mergedTwoGroup);
        // z2cSender.revealOther(e);
        // agg
        stopWatch.start();
        aggTripleNum = TRIPLE_NUM;
        LOGGER.info("agg");
        aggregation(mergedTwoGroup, otherAggB2a);
        stopWatch.stop();
        aggTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        aggTripleNum = TRIPLE_NUM - aggTripleNum;
    }

    private void bitmap() throws MpcAbortException {
        // gen bitmap
        Vector<byte[]> bitmaps = genBitmap(groupAttr, e);
        SquareZ2Vector[] transposed = new SquareZ2Vector[senderGroupNum + 1];
        int byteLen = bitmaps.get(0).length;
        if (bitmaps.size() * byteLen > maxBatchNum) {
            int byteLenSingle = Math.max(maxBatchNum / bitmaps.size(), 1);
            byte[][] bitmapsArray = bitmaps.toArray(new byte[0][]);
            for (int endIndex = byteLen; endIndex > 0; endIndex -= byteLenSingle) {
                int startIndex = Math.max(endIndex - byteLenSingle, 0);
                int finalEndIndex = endIndex;
                int bitCountNum = startIndex == 0 ? senderGroupNum + 1 - (byteLen - endIndex) * 8 : byteLenSingle * 8;
                int destIndex = startIndex == 0 ? 0 : senderGroupNum + 1 - (byteLen - startIndex) * 8;

                LOGGER.info("startIndex:{}, endIndex:{}, bitCountNum:{}, destIndex:{}, byteLenSingle:{}",
                    startIndex, endIndex, bitCountNum, destIndex, byteLenSingle);

                Vector<byte[]> input = Arrays.stream(bitmapsArray)
                    .map(ea -> Arrays.copyOfRange(ea, startIndex, finalEndIndex))
                    .collect(Collectors.toCollection(Vector::new));
                // osn
                OsnPartyOutput osnPartyOutput = osnSender.osn(input, endIndex - startIndex);
                // transpose
                SquareZ2Vector[] tmp = GroupAggUtils.transposeOsnResult(osnPartyOutput, bitCountNum);
                System.arraycopy(tmp, 0, transposed, destIndex, bitCountNum);
            }
        } else {
            // osn
            OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, bitmaps.get(0).length);
            // transpose
            transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        }

        senderBitmapShares = Arrays.stream(transposed, 1, transposed.length).toArray(SquareZ2Vector[]::new);
        e = transposed[0];
        // and
        senderBitmapShares = z2MuxSender.mux(e, senderBitmapShares);
    }

    private void sort() throws MpcAbortException {
        // sort
        SquareZlVector perm = permGenSender.sort(senderBitmapShares);
        // a2b
        SquareZ2Vector[] permB = a2bSender.a2b(perm);
        // transpose
        piG0 = TransposeUtils.transposeMergeToVector(Arrays.stream(permB).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
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
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        Vector<byte[]> input = intStream.mapToObj(i -> ByteBuffer.allocate(senderGroupByteLength + Long.BYTES)
            .put(senderGroupShare.get(i)).put(aggShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> output = permutationSender.permute(rho, input);
        List<Vector<byte[]>> split = GroupAggUtils.split(output, new int[]{senderGroupByteLength, Long.BYTES});
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

        List<byte[]> otherShares = IntStream.range(0, senderGroupNum)
            .mapToObj(i -> BytesUtils.xor(bitmap[i].getBytes(), ownShares[i])).collect(Collectors.toList());

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_BITMAP_SHARES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));

        return Arrays.stream(ownShares).map(v -> SquareZ2Vector.create(num, v, false)).toArray(SquareZ2Vector[]::new);
    }

    private Vector<byte[]> mergeGroup() {
        // merge group
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        return intStream.mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(senderGroupShare.get(i)).put(receiverGroupShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
    }

    private void aggregation(Vector<byte[]> groupField, SquareZ2Vector[] aggField) throws MpcAbortException {
        PrefixAggOutput agg = prefixAggSender.agg(groupField, aggField, e);
        // reveal
        // zlcSender.revealOther(agg.getAggs());
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
