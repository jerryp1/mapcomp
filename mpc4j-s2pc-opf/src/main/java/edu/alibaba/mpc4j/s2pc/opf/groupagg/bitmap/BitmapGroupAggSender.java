package edu.alibaba.mpc4j.s2pc.opf.groupagg.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
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
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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
    /**
     * Prefix aggregation type.
     */
    private final PrefixAggTypes prefixAggType;
    /**
     * Zl
     */
    private final Zl zl;

    public BitmapGroupAggSender(Rpc senderRpc, Party receiverParty, BitmapGroupAggConfig config) {
        super(BitmapGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        plainAndSender = PlainAndFactory.createSender(senderRpc, receiverParty, config.getPlainAndConfig());
        zlMaxSender = ZlMaxFactory.createSender(senderRpc, receiverParty, config.getZlMaxConfig());
        prefixAggType = config.getPrefixAggConfig().getPrefixType();
        zl = config.getZl();
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        zlMuxSender.init(2 * maxNum * totalGroupNum);
        plainAndSender.init(maxNum * totalGroupNum);

        zlcSender.init(1);
        zlMaxSender.init(maxL, maxNum * totalGroupNum);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        // 假定receiver拥有agg
        assert aggField == null;
        setPtoInput(groupField, aggField, e);
        // gen bitmap
        BitVector[] bitmaps = genVerticalBitmap(groupField, senderDistinctGroup);
        // share long field
        SquareZlVector aggShare = zlcSender.shareOther(num);

        // and 没有merge
        SquareZ2Vector[] allBitmapShare = new SquareZ2Vector[totalGroupNum];
        for (int i = 0; i < senderGroupNum; i++) {
            for (int j = 0; j < receiverGroupNum; j++) {
                allBitmapShare[i * senderGroupNum + j] = plainAndSender.and(bitmaps[i]);
            }
        }

        // 和bitmap mux
        SquareZlVector[] bitmapWithAgg = new SquareZlVector[totalGroupNum];
        for (int i = 0; i < totalGroupNum; i++) {
            bitmapWithAgg[i] = zlMuxSender.mux(allBitmapShare[i], aggShare);
        }

        // 和e mux TODO 这里可以复用上面的结果
        SquareZlVector[] result = new SquareZlVector[totalGroupNum];
        for (int i = 0; i < totalGroupNum; i++) {
            result[i] = zlMuxSender.mux(e, bitmapWithAgg[i]);
        }

        // agg
        agg(result);

        // reveal
        for (int i = 0; i < totalGroupNum; i++) {
            zlcSender.revealOther(result[i]);
        }
        // sender
        return null;
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
                    input[i] = zlMaxSender.max(input[i]);
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
