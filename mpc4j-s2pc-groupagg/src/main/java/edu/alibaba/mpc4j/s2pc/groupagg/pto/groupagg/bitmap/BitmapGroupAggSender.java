package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;

/**
 * Bitmap group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public class BitmapGroupAggSender extends AbstractGroupAggParty {
    /**
     * Zl mux party.
     */
    private final ZlMuxParty zlMuxSender;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * Zl circuit sender.
     */
    private final ZlcParty zlcSender;
    /**
     * Plain and sender.
     */
    private final PlainAndParty plainAndSender;
    /**
     * Zl max sender.
     */
    private final ZlMaxParty zlMaxSender;
    private final PlainPayloadMuxParty plainPayloadMuxReceiver;
    /**
     * Prefix aggregation type.
     */
    private final PrefixAggTypes prefixAggType;
    /**
     * Zl
     */
    private final Zl zl;
    private final Z2MuxParty z2MuxParty;
    protected List<String> senderDistinctGroup;
    protected List<String> receiverDistinctGroup;
    protected List<String> totalDistinctGroup;
    protected int BATCH_SIZE = 8;

    public BitmapGroupAggSender(Rpc senderRpc, Party receiverParty, BitmapGroupAggConfig config) {
        super(BitmapGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        plainAndSender = PlainAndFactory.createSender(senderRpc, receiverParty, config.getPlainAndConfig());
        zlMaxSender = ZlMaxFactory.createSender(senderRpc, receiverParty, config.getZlMaxConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        z2MuxParty = Z2MuxFactory.createSender(senderRpc, receiverParty, config.getZ2MuxConfig());
        addMultipleSubPtos(zlMuxSender, z2cSender, zlcSender, plainAndSender, zlMaxSender, plainPayloadMuxReceiver, z2MuxParty);
        prefixAggType = config.getPrefixAggConfig().getPrefixType();
        zl = config.getZl();
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        zlMuxSender.init(maxNum * BATCH_SIZE);
        plainAndSender.init(maxNum * BATCH_SIZE);
        z2cSender.init(maxNum * BATCH_SIZE);
        zlcSender.init(1);
        zlMaxSender.init(maxL, maxNum * BATCH_SIZE);
        plainPayloadMuxReceiver.init(maxNum * BATCH_SIZE);
        long totalMuxNum = ((long) maxNum) << (senderGroupBitLength + receiverGroupBitLength);
        int maxMuxInput = (int) Math.min(Integer.MAX_VALUE, totalMuxNum);
        z2MuxParty.init(maxMuxInput );
        // generate distinct group
        senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));
        receiverDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(receiverGroupBitLength));
        totalDistinctGroup = new ArrayList<>();
        for (int i = 0; i < senderGroupNum; i++) {
            for (int j = 0; j < receiverGroupNum; j++) {
                totalDistinctGroup.add(senderDistinctGroup.get(i).concat(receiverDistinctGroup.get(j)));
            }
        }

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    public static int AGG_TIME = 0;

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        // 假定receiver拥有agg
        assert aggField == null;
        setPtoInput(groupField, aggField, e);

        stopWatch.start();
        groupTripleNum = TRIPLE_NUM;
        // gen bitmap
        BitVector[] bitmaps = genVerticalBitmap(groupField, senderDistinctGroup);

        // and 没有merge
        SquareZ2Vector[] allBitmapShare = new SquareZ2Vector[totalGroupNum];
        for (int i = 0; i < senderGroupNum; i++) {
            for (int j = 0; j < receiverGroupNum; j++) {
                allBitmapShare[i * receiverGroupNum + j] = plainAndSender.and(bitmaps[i]);
            }
        }
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        BigInteger[] result = new BigInteger[totalGroupNum];
        // AND with e with mux
        allBitmapShare = z2MuxParty.mux(e, allBitmapShare);


        int batchNum = CommonUtils.getUnitNum(totalGroupNum, BATCH_SIZE);
        for (int i = 0; i < batchNum; i++) {
            int currentNum = i == batchNum - 1 ? totalGroupNum - i * BATCH_SIZE : BATCH_SIZE;
            SquareZ2Vector[] tempBitmap = new SquareZ2Vector[currentNum];
            for (int j = 0; j < currentNum; j++) {
                tempBitmap[j] = allBitmapShare[i * BATCH_SIZE + j];
            }

            stopWatch.start();
            // MUX with bitmap
            SquareZlVector[] bitmapWithAgg = plainPayloadMuxReceiver.mux(tempBitmap, null, zl.getL());
            stopWatch.stop();
            groupStep2Time += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();

            // agg
            stopWatch.start();
            long tempTripleNum = TRIPLE_NUM;
            for (int j = 0; j < bitmapWithAgg.length; j++) {
                bitmapWithAgg[j] = agg(bitmapWithAgg[j]);
            }
            stopWatch.stop();
            aggTripleNum += TRIPLE_NUM - tempTripleNum;
            aggTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();

            // reveal
            zlcSender.revealOther(bitmapWithAgg);
        }
//        for (int i = 0; i < totalGroupNum; i++) {
//            stopWatch.start();
//            // MUX with bitmap
//            SquareZlVector bitmapWithAgg = plainPayloadMuxReceiver.mux(allBitmapShare[i], null, zl.getL());
//            stopWatch.stop();
//            groupStep2Time += stopWatch.getTime(TimeUnit.MILLISECONDS);
//            stopWatch.reset();
//
//            // agg
//            stopWatch.start();
//            long tempTripleNum = TRIPLE_NUM;
//            bitmapWithAgg = agg(bitmapWithAgg);
//            stopWatch.stop();
//            aggTripleNum += TRIPLE_NUM - tempTripleNum;
//            aggTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
//            stopWatch.reset();
//            // reveal
//            zlcSender.revealOther(bitmapWithAgg);
//        }
        return new GroupAggOut(totalDistinctGroup.toArray(new String[0]), result);
    }

//    private SquareZlVector[] agg(SquareZlVector[] input) {
//        int[] nums = Arrays.stream(input)
//            .mapToInt(SquareZlVector::getNum).toArray();
//        SquareZlVector temp = SquareZlVector.create(ZlVector.merge(Arrays.stream(input)
//            .map(v -> v.getZlVector()).toArray(ZlVector[]::new)), false);
//        return Arrays.stream(ZlVector.splitWithPadding(temp.getZlVector(), nums))
//            .map(z -> SquareZlVector.create(z, false)).toArray(SquareZlVector[]::new);
//    }

    private SquareZlVector agg(SquareZlVector input) throws MpcAbortException {
        switch (prefixAggType) {
            case SUM:
                return localSum(input);
            case MAX:
                return zlMaxSender.max(input);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixAggTypes.class.getSimpleName() + ": " + prefixAggType.name());
        }
    }

    SquareZlVector localSum(SquareZlVector input) {
        BigInteger result = Arrays.stream(input.getZlVector().getElements()).parallel().reduce(BigInteger.ZERO, zl::add);
        return SquareZlVector.create(zl, new BigInteger[]{result}, false);
    }
}
