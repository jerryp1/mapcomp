package edu.alibaba.mpc4j.s2pc.opf.groupagg.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
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
    /**
     * Prefix aggregation type. // TODO 可以放到抽象类
     */
    private final PrefixAggTypes prefixAggType;
    /**
     * Zl
     */
    private final Zl zl;

    public BitmapGroupAggReceiver(Rpc receiverRpc, Party senderParty, BitmapGroupAggConfig config) {
        super(BitmapGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        plainAndReceiver = PlainAndFactory.createReceiver(receiverRpc, senderParty, config.getPlainAndConfig());
        zlMaxReceiver = ZlMaxFactory.createReceiver(receiverRpc, senderParty, config.getZlMaxConfig());
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
        zlMuxReceiver.init(2 * maxNum * totalGroupNum);
        plainAndReceiver.init(maxNum * totalGroupNum);

        zlcReceiver.init(1);
        zlMaxReceiver.init(maxL, maxNum * totalGroupNum);

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
        // secret share agg
        SquareZlVector aggShare = zlcReceiver.shareOwn(ZlVector.create(zl, Arrays.stream(aggField).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new)));

        // and 没有merge
        SquareZ2Vector[] allBitmapShare = new SquareZ2Vector[totalGroupNum];
        for (int i = 0; i < senderGroupNum; i++) {
            for (int j = 0; j < receiverGroupNum; j++) {
                allBitmapShare[i * senderGroupNum + j] = plainAndReceiver.and(bitmaps[j]);
            }
        }

        // 和bitmap mux
        SquareZlVector[] bitmapWithAgg = new SquareZlVector[totalGroupNum];
        for (int i = 0; i < totalGroupNum; i++) {
            bitmapWithAgg[i] = zlMuxReceiver.mux(allBitmapShare[i], aggShare);
        }

        // 和e mux TODO 这里可以复用上面的结果
        SquareZlVector[] result = new SquareZlVector[totalGroupNum];
        for (int i = 0; i < totalGroupNum; i++) {
            result[i] = zlMuxReceiver.mux(e, bitmapWithAgg[i]);
        }

        // agg
        agg(result);

        // reveal
        BigInteger[] r = new BigInteger[totalGroupNum];
        for (int i = 0; i < totalGroupNum; i++) {
            r[i] = zlcReceiver.revealOwn(result[i]).getElement(0);
        }
        return new GroupAggOut(totalDistinctGroup.toArray(new String[0]), r);
    }

    private void agg(SquareZlVector[] input) throws MpcAbortException {
        switch (prefixAggType) {
            case SUM:
                for (int i = 0; i < totalGroupNum; i++) {
                    input[i] = localSum(input[i]);
                }
                break;
            case MAX:
                for (int i = 0; i < totalGroupNum; i++) {
                    input[i] = zlMaxReceiver.max(input[i]);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid " + PrefixAggTypes.class.getSimpleName() + ": " + prefixAggType.name());
        }
    }

    SquareZlVector localSum(SquareZlVector input) {
        BigInteger result = Arrays.stream(input.getZlVector().getElements()).reduce(BigInteger.ZERO, zl::add);
        return SquareZlVector.create(zl, new BigInteger[]{result}, false);
    }
}
