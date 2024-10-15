package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.omix;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes.AggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside.OneSideGroupFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside.OneSideGroupParty;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.*;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.omix.OptimizedMixGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;

/**
 * Optimized mix group aggregation sender.
 */
public class OptimizedMixGroupAggSender extends AbstractGroupAggParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizedMixGroupAggSender.class);
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Plain payload mux sender.
     */
    private final PlainPayloadMuxParty plainPayloadMuxReceiver;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * Zl circuit party.
     */
    private final ZlcParty zlcSender;
    /**
     * To be invoked when aggregation is sum
     */
    private final GroupAggParty groupAggSender;
    /**
     * one side group receiver
     */
    private final OneSideGroupParty oneSideGroupSender;
    /**
     * Z2 mux sender.
     */
    private final Z2MuxParty z2MuxSender;
    /**
     * A map relation between group value and its index.
     */
    private Map<String, Integer> senderGroupMap;
    /**
     * Type of aggregation
     */
    private final PrefixAggTypes aggType;
    /**
     * Aggregation attribute in z2.
     */
    private SquareZ2Vector[] aggZ2;
    /**
     * Secret shares of bitmap.
     */
    private SquareZ2Vector[] bitmapShares;

    public OptimizedMixGroupAggSender(Rpc senderRpc, Party receiverParty, OptimizedMixGroupAggConfig config) {
        super(OptimizedMixGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        groupAggSender = GroupAggFactory.createSender(senderRpc, receiverParty,
            new MixGroupAggConfig.Builder(config.getZl(), config.isSilent(), config.getAggType()).build());
        oneSideGroupSender = OneSideGroupFactory.createSender(senderRpc, receiverParty, config.getOneSideGroupConfig());
        z2MuxSender = Z2MuxFactory.createSender(senderRpc, receiverParty, config.getZ2MuxConfig());
        aggType = config.getAggType();
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnSender.init(maxNum);
        plainPayloadMuxReceiver.init(maxNum);
        z2cSender.init(maxNum);
        zlcSender.init(1);
        if (aggType.equals(PrefixAggTypes.SUM)) {
            groupAggSender.init(properties);
        }
        oneSideGroupSender.init(1, maxNum, maxL);
        z2MuxSender.init(maxNum);
        // generate distinct group
        List<String> senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));
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
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        if (aggType.equals(PrefixAggTypes.SUM)) {
            return groupAggSender.groupAgg(groupField, aggField, e);
        } else {
            return groupAggOpti(groupField, aggField, e);
        }
    }

    public GroupAggOut groupAggOpti(String[] groupField, long[] aggField, SquareZ2Vector intersFlagE) throws MpcAbortException {
        setPtoInput(groupField, aggField, intersFlagE);
        // group
        if (aggField == null) {
            group();
        } else {
            groupWithAgg();
        }
        // agg
        agg();
        return null;
    }

    private void group() throws MpcAbortException {
        // gen bitmap
        Vector<byte[]> bitmaps = genBitmap(groupAttr, e);
        // osn
        groupTripleNum = TRIPLE_NUM;
        stopWatch.start();
        LOGGER.info("osn1");
        OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, bitmaps.get(0).length);
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        bitmapShares = Arrays.stream(transposed, 0, transposed.length - 1).toArray(SquareZ2Vector[]::new);
        e = transposed[transposed.length - 1];

        // mul1
        stopWatch.start();
        LOGGER.info("mux1");

        aggZ2 = plainPayloadMuxReceiver.muxB(e, null, zl.getL());
        stopWatch.stop();
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
    }

    private void groupWithAgg() throws MpcAbortException {
        // gen bitmap
        Vector<byte[]> bitmaps = genBitmapWithAgg(groupAttr, e, aggAttr);
        // osn
        groupTripleNum = TRIPLE_NUM;
        stopWatch.start();
        int payloadByteLen = CommonUtils.getByteLength(senderGroupNum + 1) + Long.BYTES;
        LOGGER.info("osn1");
        OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, payloadByteLen);
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, payloadByteLen * Byte.SIZE);
        bitmapShares = Arrays.stream(transposed, 0, senderGroupNum).toArray(SquareZ2Vector[]::new);
        e = transposed[senderGroupNum];
        // get aggs
        aggZ2 = new SquareZ2Vector[Long.SIZE];
        System.arraycopy(transposed, CommonUtils.getByteLength(senderGroupNum + 1) * Byte.SIZE, aggZ2, 0, Long.SIZE);

        // mul1
        stopWatch.start();
        LOGGER.info("mux1");
        aggZ2 = z2MuxSender.mux(e, aggZ2);
        stopWatch.stop();
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
    }

    private void agg() throws MpcAbortException {
        aggTripleNum = TRIPLE_NUM;
        SquareZ2Vector[][] mul = new SquareZ2Vector[senderGroupNum][];
        LOGGER.info("mux2");
        for (int i = 0; i < senderGroupNum; i++) {
            stopWatch.start();
            mul[i] = z2MuxSender.mux(bitmapShares[i], aggZ2);
            aggTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.stop();
            stopWatch.reset();
        }
        // prefix agg
        stopWatch.start();
        LOGGER.info("agg");
        aggregate(mul, e);
        stopWatch.stop();
        aggTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        aggTripleNum = TRIPLE_NUM - aggTripleNum;
    }

    private void aggregate(SquareZ2Vector[][] agg, SquareZ2Vector e) throws MpcAbortException {
        // agg
        maxAgg(agg, e);
    }

    private void maxAgg(SquareZ2Vector[][] aggField, SquareZ2Vector e) throws MpcAbortException {
        AggTypes[] types = IntStream.range(0, senderGroupNum).mapToObj(i -> AggTypes.MAX).toArray(AggTypes[]::new);
        SquareZ2Vector[] es = IntStream.range(0, senderGroupNum).mapToObj(i -> e).toArray(SquareZ2Vector[]::new);

        SquareZ2Vector[][] aggResultZ2Share = oneSideGroupSender.groupAgg(aggField, es, types, null);
        // reveal
        for (int j = 0; j < senderGroupNum; j++) {
            for (int i = 0; i < zl.getL(); i++) {
                z2cSender.revealOther(aggResultZ2Share[j][i]);
            }
        }
    }

    /**
     * Generate horizontal bitmaps.
     *
     * @param group group.
     * @return vertical bitmaps.
     */
    private Vector<byte[]> genBitmap(String[] group, SquareZ2Vector e) {
        IntStream intStream = parallel ? IntStream.range(0, group.length).parallel() : IntStream.range(0, group.length);
        return intStream.mapToObj(i -> {
            byte[] bytes = new byte[CommonUtils.getByteLength(senderGroupNum + 1)];
            BinaryUtils.setBoolean(bytes, senderGroupMap.get(group[i]), true);
            BinaryUtils.setBoolean(bytes, senderGroupNum, e.getBitVector().get(i));
            return bytes;
        }).collect(Collectors.toCollection(Vector::new));
    }

    /**
     * Generate horizontal bitmaps.
     *
     * @param group group.
     * @return vertical bitmaps.
     */
    private Vector<byte[]> genBitmapWithAgg(String[] group, SquareZ2Vector e, long[] aggAtt) {
        int payloadByteLen = CommonUtils.getByteLength(senderGroupNum + 1) + Long.BYTES;
        IntStream intStream = parallel ? IntStream.range(0, group.length).parallel() : IntStream.range(0, group.length);
        return intStream.mapToObj(i -> {
            ByteBuffer buffer = ByteBuffer.allocate(payloadByteLen);
            byte[] bytes = new byte[CommonUtils.getByteLength(senderGroupNum + 1)];
            BinaryUtils.setBoolean(bytes, senderGroupMap.get(group[i]), true);
            BinaryUtils.setBoolean(bytes, senderGroupNum, e.getBitVector().get(i));
            buffer.put(bytes);
            buffer.put(LongUtils.longToByteArray(aggAtt[i]));
            return buffer.array();
        }).collect(Collectors.toCollection(Vector::new));
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
}
