package edu.alibaba.mpc4j.s2pc.opf.groupagg.mix;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mix group aggregation receiver.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class MixGroupAggReceiver extends AbstractGroupAggParty {
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Plain payload mux receiver.
     */
    private final PlainPayloadMuxParty plainPayloadMuxSender;
    /**
     * Zl mux receiver.
     */
    private final ZlMuxParty zlMuxReceiver;
    /**
     * Z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;
    /**
     * Zl circuit receiver.
     */
    private final ZlcParty zlcReceiver;
    /**
     * Prefix aggregation party.
     */
    private final PrefixAggParty prefixAggReceiver;
    private final B2aParty b2aReceiver;
//    private final Z2MuxParty z2MuxReceiver;
    /**
     * Prefix aggregation type.
     */
    private final PrefixAggTypes aggType;

    private BitVector groupIndicator;

    private String[] resultReceiverGroup;

    protected List<String> senderDistinctGroup;

    private SquareZ2Vector[] bitmapShares;

    private SquareZlVector aggZl;

    public MixGroupAggReceiver(Rpc receiverRpc, Party senderParty, MixGroupAggConfig config) {
        super(MixGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        plainPayloadMuxSender = PlainPlayloadMuxFactory.createSender(receiverRpc, senderParty, config.getPlainPayloadMuxConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
        b2aReceiver = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
//        z2MuxReceiver = Z2MuxFactory.createReceiver(receiverRpc, senderParty, config.getZ2MuxConfig());
//        addMultipleSubPtos(osnReceiver);
//        addMultipleSubPtos(plainPayloadMuxSender);
//        addSubPtos(zlMuxReceiver);
//        addSubPtos(z2cReceiver);
//        addSubPtos(zlcReceiver);
//        addSubPtos(prefixAggReceiver);
        secureRandom = new SecureRandom();
        aggType = config.getAggType();
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnReceiver.init(maxNum);
        plainPayloadMuxSender.init(maxNum);
        zlMuxReceiver.init(maxNum);
        z2cReceiver.init(maxNum);
        zlcReceiver.init(1);
        prefixAggReceiver.init(maxL, maxNum);
        b2aReceiver.init(maxL, maxNum);
//        z2MuxReceiver.init(maxNum);
        // generate distinct group
        senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

//    public static long MIX_TIME_AGG = 0;
//    public static long MIX_TRIPLE_AGG = 0;

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector intersFlagE) throws MpcAbortException {
        setPtoInput(groupField, aggField, intersFlagE);
        // group
        if (aggField != null) {
            group();
        } else {
            groupWithSenderAgg();
        }
        // agg
        return agg();
    }

    private void group() throws MpcAbortException {
        // obtain sorting permutation
        int[] perms = obtainPerms(groupAttr);
        // apply perms to group and agg
        groupAttr = GroupAggUtils.applyPermutation(groupAttr, perms);
        aggAttr = GroupAggUtils.applyPermutation(aggAttr, perms);
        e = GroupAggUtils.applyPermutation(e, perms);
        // osn
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(perms, CommonUtils.getByteLength(senderGroupNum + 1));
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        bitmapShares = Arrays.stream(transposed, 0, transposed.length - 1).toArray(SquareZ2Vector[]::new);
        // xor own share to meet permutation
        e = SquareZ2Vector.create(transposed[transposed.length - 1].getBitVector().xor(e.getBitVector()), false);
        // mul1
        aggZl = plainPayloadMuxSender.mux(e, aggAttr, 64);
    }

    private void groupWithSenderAgg() throws MpcAbortException {
        // obtain sorting permutation
        int[] perms = obtainPerms(groupAttr);
        // apply perms to group and agg
        groupAttr = GroupAggUtils.applyPermutation(groupAttr, perms);
//        aggAttr = GroupAggUtils.applyPermutation(aggAttr, perms);
        e = GroupAggUtils.applyPermutation(e, perms);
        // osn
        int payloadByteLen = CommonUtils.getByteLength(senderGroupNum + 1) + Long.BYTES;
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(perms, payloadByteLen);
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, payloadByteLen * Byte.SIZE);
        // bitmap
        bitmapShares = Arrays.stream(transposed, 0, senderGroupNum).toArray(SquareZ2Vector[]::new);
        // xor own share to meet permutation
        e = SquareZ2Vector.create(transposed[senderGroupNum].getBitVector().xor(e.getBitVector()), false);
        // get aggs
        SquareZ2Vector[] aggZ2 = new SquareZ2Vector[Long.SIZE];
        System.arraycopy(transposed, CommonUtils.getByteLength(senderGroupNum + 1) * Byte.SIZE, aggZ2, 0, Long.SIZE);
        aggZl = b2aReceiver.b2a(aggZ2);
        // mul1
        aggZl = zlMuxReceiver.mux(e, aggZl);
    }

    private GroupAggOut agg() throws MpcAbortException {
        ZlVector[] plainAgg = new ZlVector[senderGroupNum];
        for (int i = 0; i < senderGroupNum; i++) {
            SquareZlVector mul = zlMuxReceiver.mux(bitmapShares[i], aggZl);
            plainAgg[i] = aggregate(groupAttr, mul);
        }
        // indexes of valid position
        int[] groupIndex = getGroupIndexes(groupIndicator);
        // arrange
        BigInteger[] aggResult = new BigInteger[senderGroupNum * groupIndex.length];
        String[] groupResult = new String[senderGroupNum * groupIndex.length];
        resultReceiverGroup = groupAttr;
        for (int i = 0; i < senderGroupNum; i++) {
            for (int j = 0; j < groupIndex.length; j++) {
                aggResult[i * groupIndex.length + j] = plainAgg[i].getElement(groupIndex[j]);
                groupResult[i * groupIndex.length + j] = senderDistinctGroup.get(i).concat(resultReceiverGroup[groupIndex[j]]);
            }
        }
        return new GroupAggOut(groupResult, aggResult);
    }

    private int[] getGroupIndexes(BitVector indicator) {
        return IntStream.range(0, num).filter(indicator::get).toArray();
    }

    private ZlVector aggregate(String[] permutedGroup, SquareZlVector agg) throws MpcAbortException {
        // agg
        switch (aggType) {
            case SUM:
                return sumAgg(permutedGroup, agg);
            case MAX:
                return maxAgg(permutedGroup, agg);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixAggTypes.class.getSimpleName() + ": " + aggType.name());
        }
    }

    private ZlVector sumAgg(String[] groupField, SquareZlVector aggField) throws MpcAbortException {
        Zl zl = aggField.getZl();
        // agg
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField);
        // reveal
        groupIndicator = z2cReceiver.revealOwn(agg.getIndicator());
        ZlVector aggResult = zlcReceiver.revealOwn(agg.getAggs());
        // subtraction
        int[] indexes = obtainIndexes(groupIndicator);
        BigInteger[] result = aggResult.getElements();
        for (int i = 0; i < indexes.length - 1; i++) {
            result[indexes[i]] = zl.sub(result[indexes[i]], result[indexes[i + 1]]);
        }
        return ZlVector.create(zl, result);
    }

    private ZlVector maxAgg(String[] groupField, SquareZlVector aggField) throws MpcAbortException {
        // agg
        PrefixAggOutput prefixAggOutput = prefixAggReceiver.agg(groupField, aggField);
        // reveal
        groupIndicator = z2cReceiver.revealOwn(prefixAggOutput.getIndicator());
        return zlcReceiver.revealOwn(prefixAggOutput.getAggs());
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
}
