package edu.alibaba.mpc4j.s2pc.opf.groupagg.omix;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupFactory.AggTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.*;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.omix.OptimizedMixGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;
import org.apache.commons.lang3.time.StopWatch;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;

/**
 * Optimized mix group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/25
 */
public class OptimizedMixGroupAggSender extends AbstractGroupAggParty {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Plain payload mux sender.
     */
    private final PlainPayloadMuxParty plainPayloadMuxReceiver;
    /**
     * Zl mux party.
     */
    private final ZlMuxParty zlMuxSender;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * Zl circuit party.
     */
    private final ZlcParty zlcSender;
    /**
     * prefix aggregate sender
     */
    private final PrefixAggParty prefixAggSender;

    private final GroupAggParty groupAggSender;

    private final OneSideGroupParty oneSideGroupSender;

    private boolean hasSetGroupSet = false;

    protected List<String> senderDistinctGroup;

    private PrefixAggTypes aggType;

    public OptimizedMixGroupAggSender(Rpc senderRpc, Party receiverParty, OptimizedMixGroupAggConfig config) {
        super(OptimizedMixGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        groupAggSender = GroupAggFactory.createSender(senderRpc, receiverParty,
            new MixGroupAggConfig.Builder(config.getZl(), config.isSilent(), config.getAggType()).build());
        oneSideGroupSender = OneSideGroupFactory.createSender(senderRpc, receiverParty, config.getOneSideGroupConfig());
        aggType = config.getAggType();


//        addMultipleSubPtos(osnSender);
//        addMultipleSubPtos(plainPayloadMuxReceiver);
//        addSubPtos(zlMuxSender);
//        addSubPtos(z2cSender);
//        addSubPtos(zlcSender);
//        addSubPtos(prefixAggSender);
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnSender.init(maxNum);
        plainPayloadMuxReceiver.init(maxNum);
        zlMuxSender.init(maxNum);
        z2cSender.init(maxNum);
        zlcSender.init(1);
        prefixAggSender.init(maxL, maxNum);
        if (aggType.equals(PrefixAggTypes.SUM)) {
            groupAggSender.init(properties);
        }
        oneSideGroupSender.init(1, maxNum, maxL);
        // generate distinct group
        senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    public static long OSN_TIME = 0;
    public static long AGG_TIME = 0;
    public static long MUX_TIME = 0;

    public static long MIX_TIME_AGG = 0;
    public static long MIX_TRIPLE_AGG = 0;

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        if (aggType.equals(PrefixAggTypes.SUM)) {
            return groupAggSender.groupAgg(groupField, aggField, e);
        } else {
            return groupAggOpti(groupField, aggField, e);
        }
    }

    public GroupAggOut groupAggOpti(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        StopWatch stopWatch = new StopWatch();
        assert aggField == null;
        setPtoInput(groupField, aggField, e);
        // gen bitmap
        Vector<byte[]> bitmaps = genBitmap(groupField, e);
        // osn
        stopWatch.start();
        OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, bitmaps.get(0).length);
        stopWatch.stop();
        OSN_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        SquareZ2Vector[] bitmapShares = Arrays.stream(transposed, 0, transposed.length - 1).toArray(SquareZ2Vector[]::new);
        e = transposed[transposed.length - 1];

        // mul1
        stopWatch.start();
        SquareZlVector mul1 = plainPayloadMuxReceiver.mux(e, null, 64);
        stopWatch.stop();
        MUX_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long tripleNum = TRIPLE_NUM;
        for (int i = 0; i < senderGroupNum; i++) {
            stopWatch.start();
            SquareZlVector mul = zlMuxSender.mux(bitmapShares[i], mul1);
            stopWatch.stop();
            MUX_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // prefix agg
            stopWatch.start();
            aggregate(mul1, e);
            stopWatch.stop();
            AGG_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
            MIX_TIME_AGG += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
        }
        MIX_TRIPLE_AGG = TRIPLE_NUM - tripleNum;
        return null;
    }

    private void aggregate(SquareZlVector agg, SquareZ2Vector e) throws MpcAbortException {
        // agg
        maxAgg(agg, e);
    }

    private void maxAgg(SquareZlVector aggField, SquareZ2Vector e) throws MpcAbortException {

        // agg to z2
        SquareZ2Vector[] aggZ2 = Arrays.stream(TransposeUtils.transposeSplit(aggField.getZlVector().getElements(), zl.getL()))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);

        SquareZ2Vector[] aggResultZ2Share = oneSideGroupSender.groupAgg(aggZ2, e, AggTypes.MAX, null);
        // reveal
        for (int i = 0; i < zl.getL(); i++) {
            z2cSender.revealOther(aggResultZ2Share[i]);
        }
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
            BinaryUtils.setBoolean(bytes, senderDistinctGroup.indexOf(group[i]), true);
            BinaryUtils.setBoolean(bytes, senderGroupNum, e.getBitVector().get(i));
            return bytes;
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
