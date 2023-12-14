package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside.OnesideGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;

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
 * Mix group aggregation receiver. Receiver is assumed to always has group attributes.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class OnesideGroupAggReceiver extends AbstractGroupAggParty {
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
    /**
     * B2a receiver.
     */
    private final B2aParty b2aReceiver;
    /**
     * Zl Drelu receiver.
     */
    private final ZlDreluParty zlDreluReceiver;
    /**
     * Prefix aggregation type.
     */
    private final PrefixAggTypes aggType;
    /**
     * Group indicator.
     */
    private BitVector groupIndicator;
    /**
     * Sender's distinct group.
     */
    protected List<String> senderDistinctGroup;
    /**
     * Aggregation attribute in zl.
     */
    private SquareZlVector aggZl;
    /**
     * Summation in zl.
     */
    private SquareZlVector sumZl;

    public OnesideGroupAggReceiver(Rpc receiverRpc, Party senderParty, OneSideGroupAggConfig config) {
        super(OnesideGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        plainPayloadMuxSender = PlainPlayloadMuxFactory.createSender(receiverRpc, senderParty, config.getPlainPayloadMuxConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
        b2aReceiver = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
        zlDreluReceiver = ZlDreluFactory.createReceiver(receiverRpc, senderParty, config.getZlDreluConfig());
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
        zlDreluReceiver.init(maxL, maxNum);
        // generate distinct group
        senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector intersFlagE) throws MpcAbortException {
        setPtoInput(groupField, aggField, intersFlagE);
        if (havingState) {
            getSum();
        }
        if (aggField != null) {
            // receiver has groups
            group();
        } else {
            // sender has groups
            groupWithSenderAgg();
        }
        // agg
        return agg();
    }

    private void getSum() throws MpcAbortException {
        SquareZlVector mul = plainPayloadMuxSender.mux(e, aggAttr, Long.SIZE);
        BigInteger sum = Arrays.stream(mul.getZlVector().getElements()).reduce(BigInteger.ZERO, (a, b) -> zl.add(a, b));
        sumZl = SquareZlVector.create(zl, IntStream.range(0, num).mapToObj(i -> sum).toArray(BigInteger[]::new), false);
    }

    private void group() throws MpcAbortException {
        // obtain sorting permutation
        int[] perms = obtainPerms(groupAttr);
        // apply perms to group and agg
        groupAttr = GroupAggUtils.applyPermutation(groupAttr, perms);
        aggAttr = GroupAggUtils.applyPermutation(aggAttr, perms);
        e = GroupAggUtils.applyPermutation(e, perms);
        // osn
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(perms, 1);
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, 1);
        // xor own share to meet permutation
        e = SquareZ2Vector.create(transposed[transposed.length - 1].getBitVector().xor(e.getBitVector()), false);
        // mul1
        aggZl = plainPayloadMuxSender.mux(e, aggAttr, Long.SIZE);
    }

    private void groupWithSenderAgg() throws MpcAbortException {
        // obtain sorting permutation
        int[] perms = obtainPerms(groupAttr);
        // apply perms to group and agg
        groupAttr = GroupAggUtils.applyPermutation(groupAttr, perms);
        e = GroupAggUtils.applyPermutation(e, perms);
        // osn
        int payloadByteLen = dummyPayload ? CommonUtils.getByteLength(1) + 2 * Long.BYTES :
            CommonUtils.getByteLength(1) + Long.BYTES;
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(perms, payloadByteLen);
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, payloadByteLen * Byte.SIZE);
        // xor own share to meet permutation
        e = SquareZ2Vector.create(transposed[0].getBitVector().xor(e.getBitVector()), false);
        // get aggs
        SquareZ2Vector[] aggZ2 = new SquareZ2Vector[Long.SIZE];
        System.arraycopy(transposed, Byte.SIZE, aggZ2, 0, Long.SIZE);
        aggZl = b2aReceiver.b2a(aggZ2);
        // mul1
        aggZl = zlMuxReceiver.mux(e, aggZl);
        if (dummyPayload) {
            zlMuxReceiver.mux(e, b2aReceiver.b2a(aggZ2));
        }
    }

    private GroupAggOut agg() throws MpcAbortException {
        // agg
        ZlVector plainAgg = aggregate(groupAttr, aggZl);

        int[] groupIndex = getGroupIndexes(groupIndicator);
        // arrange
        BigInteger[] aggResult = new BigInteger[groupIndex.length];
        String[] groupResult = new String[groupIndex.length];
        for (int j = 0; j < groupIndex.length; j++) {
            aggResult[j] = plainAgg.getElement(groupIndex[j]);
            groupResult[j] = groupAttr[groupIndex[j]];
        }
        return new GroupAggOut(groupResult, aggResult);
    }

    private int[] getGroupIndexes(BitVector indicator) {
        return IntStream.range(0, num).filter(indicator::get).toArray();
    }

    private ZlVector aggregate(String[] permutedGroup, SquareZlVector agg) throws MpcAbortException {
        switch (aggType) {
            case SUM:
                return sumAgg(permutedGroup, agg);
            case MAX:
                throw new IllegalArgumentException("Do not support max for one-side.");
            default:
                throw new IllegalArgumentException("Invalid " + PrefixAggTypes.class.getSimpleName() + ": " + aggType.name());
        }
    }

    private ZlVector sumAgg(String[] groupField, SquareZlVector aggField) throws MpcAbortException {
        Zl zl = aggField.getZl();
        // agg
        PrefixAggOutput agg = prefixAggReceiver.agg(groupField, aggField);
        if (dummyPayload) {
            PrefixAggOutput dummy = prefixAggReceiver.agg(groupField, aggField);
        }
        // reveal
        groupIndicator = z2cReceiver.revealOwn(agg.getIndicator());
        // if q11 then compare
        SquareZlVector aggTemp = agg.getAggs();
        if (havingState) {
            SquareZ2Vector compare = zlDreluReceiver.drelu(zlcReceiver.sub(aggTemp, sumZl));
            BitVector c = z2cReceiver.revealOwn(compare);
            groupIndicator = groupIndicator.and(c);
        }
        ZlVector aggResult = zlcReceiver.revealOwn(aggTemp);
        // subtraction
        int[] indexes = obtainIndexes(groupIndicator);
        BigInteger[] result = aggResult.getElements();
        for (int i = 0; i < indexes.length - 1; i++) {
            result[indexes[i]] = zl.sub(result[indexes[i]], result[indexes[i + 1]]);
        }
        return ZlVector.create(zl, result);
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
