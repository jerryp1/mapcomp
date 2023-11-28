package edu.alibaba.mpc4j.s2pc.opf.groupagg.omix;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupFactory.AggTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.*;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.omix.OptimizedMixGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
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
 * Optimized mix group aggregation receiver.
 *
 * @author Li Peng
 * @date 2023/11/25
 */
public class OptimizedMixGroupAggReceiver extends AbstractGroupAggParty {
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
     * group agg receiver.
     */
    private final GroupAggParty groupAggReceiver;

    private final OneSideGroupParty oneSideGroupReceiver;
    private final Z2MuxParty z2MuxReceiver;
    /**
     * Prefix aggregation type.
     */
    private final PrefixAggTypes aggType;

    private BitVector groupIndicator;

    protected List<String> senderDistinctGroup;
    /**
     * indexes of valid position
     */
    private int[] groupIndex;

    public OptimizedMixGroupAggReceiver(Rpc receiverRpc, Party senderParty, OptimizedMixGroupAggConfig config) {
        super(OptimizedMixGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        plainPayloadMuxSender = PlainPlayloadMuxFactory.createSender(receiverRpc, senderParty, config.getPlainPayloadMuxConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
        groupAggReceiver = GroupAggFactory.createReceiver(receiverRpc, senderParty, new MixGroupAggConfig.Builder(config.getZl(), config.isSilent(), config.getAggType()).build());
        oneSideGroupReceiver = OneSideGroupFactory.createReceiver(receiverRpc, senderParty, config.getOneSideGroupConfig());
        z2MuxReceiver = Z2MuxFactory.createReceiver(receiverRpc, senderParty, config.getZ2MuxConfig());
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
        if (aggType.equals(PrefixAggTypes.SUM)) {
            groupAggReceiver.init(properties);
        }
        oneSideGroupReceiver.init(1, maxNum, maxL);
        z2MuxReceiver.init(maxNum);
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
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        if (aggType.equals(PrefixAggTypes.SUM)) {
            return groupAggReceiver.groupAgg(groupField, aggField, e);
        } else {
            return groupAggOpti(groupField, aggField, e);
        }
    }

    public GroupAggOut groupAggOpti(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        // 假定receiver拥有agg
        assert aggField != null;
        setPtoInput(groupField, aggField, e);
        // obtain sorting permutation
        int[] perms = obtainPerms(groupField);
        // apply perms to group and agg
        String[] permutedGroup = GroupAggUtils.applyPermutation(groupField, perms);
        aggField = GroupAggUtils.applyPermutation(aggField, perms);
        e = GroupAggUtils.applyPermutation(e, perms);
        // osn
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(perms, CommonUtils.getByteLength(senderGroupNum + 1));
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        SquareZ2Vector[] bitmapShares = Arrays.stream(transposed, 0, transposed.length - 1).toArray(SquareZ2Vector[]::new);
        // xor own share to meet permutation
        e = SquareZ2Vector.create(transposed[transposed.length - 1].getBitVector().xor(e.getBitVector()), false);
        // mul1
        BitVector[] tranposedLong = TransposeUtils.transposeSplit(aggField, zl.getL());
        SquareZ2Vector[] mul1 = plainPayloadMuxSender.muxB(e, tranposedLong, zl.getL());
        ZlVector[] plainAgg = new ZlVector[senderGroupNum];
        for (int i = 0; i < senderGroupNum; i++) {
            SquareZ2Vector[] mul = z2MuxReceiver.mux(bitmapShares[i], mul1);
            plainAgg[i] = aggregate(permutedGroup, mul, e);
        }
        // arrange
        BigInteger[] aggResult = new BigInteger[senderGroupNum * groupIndex.length];
        String[] groupResult = new String[senderGroupNum * groupIndex.length];
        for (int i = 0; i < senderGroupNum; i++) {
            for (int j = 0; j < groupIndex.length; j++) {
                aggResult[i * groupIndex.length + j] = plainAgg[i].getElement(groupIndex[j]);
                groupResult[i * groupIndex.length + j] = senderDistinctGroup.get(i).concat(permutedGroup[groupIndex[j]]);
            }
        }
        return new GroupAggOut(groupResult, aggResult);
    }

    private int[] getGroupIndexes(BitVector indicator) {
        return IntStream.range(0, num).filter(indicator::get).toArray();
    }

    private ZlVector aggregate(String[] permutedGroup, SquareZ2Vector[] agg, SquareZ2Vector e) throws MpcAbortException {
        // agg
        return maxAgg(permutedGroup, agg, e);
    }

    private ZlVector maxAgg(String[] groupField, SquareZ2Vector[] aggField, SquareZ2Vector e) throws MpcAbortException {
        // compute indicator
        groupIndicator = obtainIndicator(groupField);

        // agg
        SquareZ2Vector[] aggResultZ2Share = oneSideGroupReceiver.groupAgg(aggField, e, AggTypes.MAX, groupIndicator);
        groupIndex = oneSideGroupReceiver.getResPosFlag(groupIndicator);
        // reveal
        BitVector[] aggResultZ2 = new BitVector[zl.getL()];
        for (int i = 0; i < zl.getL(); i++) {
            aggResultZ2[i] = z2cReceiver.revealOwn(aggResultZ2Share[i]);
        }
        // transpose
        BigInteger[] results = TransposeUtils.transposeMergeToVector(aggResultZ2).stream()
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger).toArray(BigInteger[]::new);

        return ZlVector.create(zl, results);
    }

    private BitVector obtainIndicator(String[] x) {
        BitVector plainIndicator = BitVectorFactory.createZeros(x.length);
        IntStream.range(0, x.length - 1).forEach(i -> plainIndicator.set(i + 1, !x[i].equals(x[i + 1])));
        plainIndicator.set(0, true);
        return plainIndicator;
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
