package edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
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
import edu.alibaba.mpc4j.s2pc.opf.groupagg.osorting.OptimizedSortingGroupAggReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationReceiver;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Bitmap assist sorting-based group aggregation receiver.
 *
 * @author Li Peng
 * @date 2023/11/20
 */
public class BitmapSortingGroupAggReceiver extends AbstractGroupAggParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapSortingGroupAggReceiver.class);
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
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
     * Plain bit mux sender.
     */
    private final PlainPayloadMuxParty plainPayloadMuxSender;
    /**
     * Permutation sender of reverse order.
     */
    private final PermutationSender reversePermutationSender;
    /**
     * Permutation receiver.
     */
    private final PermutationReceiver permutationReceiver;
    /**
     * Permutation generation protocol receiver.
     */
    private final PermGenParty permGenReceiver;
    /**
     * A2b receiver
     */
    private final A2bParty a2bReceiver;
    /**
     * Own bit split.
     */
    private Vector<byte[]> eByte;
    /**
     * Prefix aggregation type.
     */
    private final PrefixAggTypes prefixAggType;
    /**
     * bitmap shares
     */
    private SquareZ2Vector[] senderBitmapShares;
    /**
     * sigmaB
     */
    private int[] sigmaB;

    private Vector<byte[]> piG0;

    private Vector<byte[]> rho;

    public BitmapSortingGroupAggReceiver(Rpc receiverRpc, Party senderParty, BitmapSortingGroupAggConfig config) {
        super(BitmapSortingGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        sharedPermutationReceiver = SharedPermutationFactory.createReceiver(receiverRpc, senderParty, config.getSharedPermutationConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        b2aReceiver = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
        plainPayloadMuxSender = PlainPlayloadMuxFactory.createSender(receiverRpc, senderParty, config.getPlainPayloadMuxConfig());
        reversePermutationSender = PermutationFactory.createSender(receiverRpc, senderParty, config.getReversePermutationConfig());
        permutationReceiver = PermutationFactory.createReceiver(receiverRpc, senderParty, config.getPermutationConfig());
        permGenReceiver = PermGenFactory.createReceiver(receiverRpc, senderParty, config.getPermGenConfig());
        a2bReceiver = A2bFactory.createReceiver(receiverRpc, senderParty, config.getA2bConfig());
//        addSubPtos(osnSender);
//        addSubPtos(zlMuxReceiver);
//        addSubPtos(sharedPermutationReceiver);
//        addSubPtos(prefixAggReceiver);
//        addSubPtos(z2cReceiver);
//        addSubPtos(zlcReceiver);
//        addSubPtos(b2aReceiver);
        prefixAggType = config.getPrefixAggConfig().getPrefixType();
        secureRandom = new SecureRandom();
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnReceiver.init(maxNum);
        zlMuxReceiver.init(maxNum);

        prefixAggReceiver.init(maxL, maxNum);
        z2cReceiver.init(maxL * maxNum);
        zlcReceiver.init(1);
        b2aReceiver.init(maxL, maxNum);
        reversePermutationSender.init(maxL, maxNum);
        sharedPermutationReceiver.init(maxNum);
        permutationReceiver.init(maxL, maxNum);
        permGenReceiver.init(maxNum, senderGroupNum);
        a2bReceiver.init(maxL, maxNum);
        plainPayloadMuxSender.init(maxNum);

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

        // bitmap
        stopWatch.start();
        bitmap();
        LOGGER.info("bitmap done");
        stopWatch.stop();
        long bitmapT = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // sort
        stopWatch.start();
        sort();
        LOGGER.info("sorting done");
        stopWatch.stop();
        long sortT = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute1, permute receiver's group,agg and sigmaB using pig0, output rho
        stopWatch.start();
        permute1();
        LOGGER.info("permute1 done");
        stopWatch.stop();
        long permute1T = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute2, permute sender's group using rho
        stopWatch.start();
        permute2();
        LOGGER.info("permute2 done");
        stopWatch.stop();
        long permute2T = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // permute3, permute e
        stopWatch.start();
        permute3();
        LOGGER.info("permute3 done");
        stopWatch.stop();
        long permute3T = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // merge group
        Vector<byte[]> mergedTwoGroup = mergeGroup();
        // b2a
        stopWatch.start();
        SquareZlVector receiverAggAs = b2a();
        stopWatch.stop();
        long b2aT = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // ### test
//        String[] groupResult = revealBothGroup(mergedTwoGroup);
//        ZlVector zlVector = zlcReceiver.revealOwn(receiverAggAs);
//        BitVector eB = z2cReceiver.revealOwn(e);
        // aggregation
        stopWatch.start();
        GroupAggOut out = aggregation(mergedTwoGroup, receiverAggAs, e);
        stopWatch.stop();
        long agg1T = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        LOGGER.info("bitmapT:{},sortT:{},permute1T:{},permute2T:{},permute3:{},b2aT:{},aggT:{}",bitmapT,sortT,permute1T,permute2T,permute3T,b2aT,agg1T);
        return out;
    }


    private void bitmap() throws MpcAbortException {
        // obtain sorting permutation
        sigmaB = obtainPerms(groupAttr);
        // apply perms to group and agg
        groupAttr = GroupAggUtils.applyPermutation(groupAttr, sigmaB);
        aggAttr = GroupAggUtils.applyPermutation(aggAttr, sigmaB);
        e = GroupAggUtils.applyPermutation(e, sigmaB);
        // osn
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(sigmaB, CommonUtils.getByteLength(senderGroupNum + 1));
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        senderBitmapShares = Arrays.stream(transposed, 1, transposed.length).toArray(SquareZ2Vector[]::new);
        // xor own share to meet permutation
        e = SquareZ2Vector.create(transposed[0].getBitVector().xor(e.getBitVector()), false);
//      // and
        for (int i = 0; i < senderGroupNum; i++) {
            senderBitmapShares[i] = z2cReceiver.and(senderBitmapShares[i], e);
        }
//        BitVector test = z2cReceiver.revealOwn(e);
//        System.out.println();
    }

    private void sort() throws MpcAbortException {
        // sort
        SquareZlVector perm = permGenReceiver.sort(senderBitmapShares);
        // a2b
        SquareZ2Vector[] permB = a2bReceiver.a2b(perm);
        // transpose
        piG0 = TransposeUtils.transposeMergeToVector(Arrays.stream(permB).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));

        long[] test = revealOwnLong(piG0);
    }

    private void permute1() throws MpcAbortException {
        // permute receiver's group,agg and sigmaB
        Vector<byte[]> groupBytes = GroupAggUtils.binaryStringToBytes(groupAttr);
        Vector<byte[]> input = IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(receiverGroupByteLength + Long.BYTES + Integer.BYTES)
            .put(groupBytes.get(i)).put(LongUtils.longToByteArray(aggAttr[i])).putInt(sigmaB[i]).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> output = reversePermutationSender.permute(piG0, input);
        // split
        List<Vector<byte[]>> split = GroupAggUtils.split(output, new int[]{receiverGroupByteLength, Long.BYTES, Integer.BYTES});
        receiverGroupShare = split.get(0);
        aggShare = split.get(1);
        rho = split.get(2);
    }

    private void permute2() throws MpcAbortException {
        senderGroupShare = permutationReceiver.permute(rho, senderGroupByteLength);
    }

    private void permute3() throws MpcAbortException {
        Vector<byte[]> eByte = IntStream.range(0, num).mapToObj(i ->
            ByteBuffer.allocate(1).put(e.getBitVector().get(i) ? (byte) 1 : (byte) 0).array()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> permutedE = sharedPermutationReceiver.permute(rho, eByte);
        e = SquareZ2Vector.createZeros(num, false);
        IntStream.range(0, num).forEach(i -> e.getBitVector().set(i, (permutedE.get(i)[0] & 1) == 1));
    }

    private Vector<byte[]> mergeGroup() {
        return IntStream.range(0, num).mapToObj(i -> ByteBuffer.allocate(totalGroupByteLength)
            .put(senderGroupShare.get(i)).put(receiverGroupShare.get(i)).array()).collect(Collectors.toCollection(Vector::new));
    }

    private SquareZlVector b2a() throws MpcAbortException {
        // b2a, transfer agg to arithmetic share
        SquareZ2Vector[] transposed = Arrays.stream(TransposeUtils.transposeSplit(aggShare, Long.SIZE))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        return zlMuxReceiver.mux(e, b2aReceiver.b2a(transposed));
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
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField, null);
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
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField, null);
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

    protected Vector<byte[]> shareOther() {
        DataPacketHeader receiveSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_SHARES.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiveSharesPayload = rpc.receive(receiveSharesHeader).getPayload();
        return new Vector<>(receiveSharesPayload);
    }

    protected SquareZ2Vector[] shareOtherBitmap() {
        DataPacketHeader receiveSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_BITMAP_SHARES.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiveSharesPayload = rpc.receive(receiveSharesHeader).getPayload();
        return receiveSharesPayload.stream().map(v -> SquareZ2Vector.create(num, v, false)).toArray(SquareZ2Vector[]::new);
    }

    private int[] getGroupIndexes(BitVector indicator) {
        return IntStream.range(0, num).filter(indicator::get).toArray();
    }
}
