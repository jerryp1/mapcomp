package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Bitmap group aggregation receiver.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public class BitmapGroupAggReceiver extends AbstractGroupAggParty {
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
     * Plain and receiver.
     */
    private final PlainAndParty plainAndReceiver;
    /**
     * Zl max receiver.
     */
    private final ZlMaxParty zlMaxReceiver;
    private final PlainPayloadMuxParty plainPayloadMuxSender;
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

    public BitmapGroupAggReceiver(Rpc receiverRpc, Party senderParty, BitmapGroupAggConfig config) {
        super(BitmapGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        plainAndReceiver = PlainAndFactory.createReceiver(receiverRpc, senderParty, config.getPlainAndConfig());
        zlMaxReceiver = ZlMaxFactory.createReceiver(receiverRpc, senderParty, config.getZlMaxConfig());
        plainPayloadMuxSender = PlainPlayloadMuxFactory.createSender(receiverRpc, senderParty, config.getPlainPayloadMuxConfig());
        z2MuxParty = Z2MuxFactory.createReceiver(receiverRpc, senderParty, config.getZ2MuxConfig());
        addMultipleSubPtos(zlMuxReceiver, z2cReceiver, zlcReceiver, plainAndReceiver, zlMaxReceiver, plainPayloadMuxSender, z2MuxParty);
        prefixAggType = config.getPrefixAggConfig().getPrefixType();
        secureRandom = new SecureRandom();
        zl = config.getZl();
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zlMuxReceiver.init(maxNum * BATCH_SIZE);
        plainAndReceiver.init(Integer.MAX_VALUE);
        z2cReceiver.init(maxNum * BATCH_SIZE);
        zlcReceiver.init(1);
        zlMaxReceiver.init(maxL, maxNum * BATCH_SIZE);
        plainPayloadMuxSender.init(maxNum * BATCH_SIZE);
        long totalMuxNum = ((long) maxNum) << (senderGroupBitLength + receiverGroupBitLength);
        int maxMuxInput = (int) Math.min(Integer.MAX_VALUE, totalMuxNum);
        z2MuxParty.init(maxMuxInput);
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


    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        // 假定receiver拥有agg
        assert aggField != null;
        // set input
        setPtoInput(groupField, aggField, e);
        // bitmap
        BitVector[] bitmaps = genVerticalBitmap(groupField, receiverDistinctGroup);

        // and 没有merge
        SquareZ2Vector[] allBitmapShare = new SquareZ2Vector[totalGroupNum];
        for (int i = 0; i < senderGroupNum; i++) {
            int smallBatch = Math.min(Math.max((1<<24) / bitmaps[0].bitNum(), 1), receiverGroupNum);
            int batchNum = receiverGroupNum / smallBatch;
            for(int j = 0; j < batchNum; j++){
                int startIndex = j * smallBatch;
                int endIndex = Math.min(startIndex + smallBatch, receiverGroupNum);
                BitVector[] tmpInput = Arrays.copyOfRange(bitmaps, startIndex, endIndex);
                System.arraycopy(plainAndReceiver.and(tmpInput), 0, allBitmapShare, i * receiverGroupNum + startIndex, tmpInput.length);
            }
//            for (int j = 0; j < receiverGroupNum; j++) {
//                allBitmapShare[i * receiverGroupNum + j] = plainAndReceiver.and(bitmaps[j]);
//            }
        }

        BigInteger[] result = new BigInteger[totalGroupNum];
        // AND with e with mux
        allBitmapShare = z2MuxParty.mux(e, allBitmapShare);

        int batchNum = CommonUtils.getUnitNum(totalGroupNum, BATCH_SIZE);
        for (int i = 0; i < batchNum; i++) {
            int currentNum = i == batchNum - 1 ? totalGroupNum - i * BATCH_SIZE : BATCH_SIZE;
            SquareZ2Vector[] tempBitmap = new SquareZ2Vector[currentNum];
            System.arraycopy(allBitmapShare, i * BATCH_SIZE, tempBitmap, 0, currentNum);
            // MUX with bitmap
            SquareZlVector[] bitmapWithAgg = plainPayloadMuxSender.mux(tempBitmap, aggAttr, zl.getL());
            // agg
            for (int j = 0; j < bitmapWithAgg.length; j++) {
                bitmapWithAgg[j] = agg(bitmapWithAgg[j]);
            }
            // reveal
            ZlVector[] results = zlcReceiver.revealOwn(bitmapWithAgg);
            for (int j = 0; j < results.length; j++) {
                result[j + i * BATCH_SIZE] = results[j].getElement(0);
            }
        }
//        for (int i = 0; i < totalGroupNum; i++) {
//            // MUX with bitmap
//            SquareZlVector bitmapWithAgg = plainPayloadMuxSender.mux(allBitmapShare[i], aggAttr, zl.getL());
//            // agg
//            bitmapWithAgg = agg(bitmapWithAgg);
//            // reveal
//            result[i] = zlcReceiver.revealOwn(bitmapWithAgg).getElement(0);
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
                return zlMaxReceiver.max(input);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixAggTypes.class.getSimpleName() + ": " + prefixAggType.name());
        }
    }

    SquareZlVector localSum(SquareZlVector input) {
        BigInteger result = Arrays.stream(input.getZlVector().getElements()).parallel().reduce(BigInteger.ZERO, zl::add);
        return SquareZlVector.create(zl, new BigInteger[]{result}, false);
    }
}
