package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * abstract plain mux party.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public abstract class AbstractPlainMuxParty extends AbstractTwoPartyPto implements PlainMuxParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPlainMuxParty.class);
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num
     */
    protected int num;
    /**
     * Zl instance
     */
    protected Zl zl;
    /**
     * l in bytes
     */
    protected int byteL;
    /**
     * input bits
     */
    protected SquareZ2Vector inputBits;
    /**
     * input
     */
    protected long[] inputs;

    public AbstractPlainMuxParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PlainMuxConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZ2Vector xi, long[] yi) {
        assert zl.getL() >= Long.SIZE;
        if (yi != null) {
            MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi.length);
        }
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        byteL = zl.getByteL();
        inputBits = xi;
        inputs = yi;
    }

//    @Override
//    public SquareZlVector[] mux(SquareZ2Vector[] xiArray, long[] y0) throws MpcAbortException {
//
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        // check
//        Arrays.stream(xiArray).forEach(x -> Preconditions.checkArgument(!x.isPlain(), "Mux is only supported for inputs in secret state."));
//        // merge
//        SquareZ2Vector mergedXiArray = SquareZ2Vector.create(BitVectorFactory.merge(Arrays.stream(xiArray)
//            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new)), false);
//        SquareZlVector mergedZlArray = SquareZlVector.create(ZlVector.merge(Arrays.stream(y0)
//            .map(SquareZlVector::getZlVector).toArray(ZlVector[]::new)), false);
//        stopWatch.stop();
//        long mux1 = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        // mux
//        stopWatch.reset();
//        stopWatch.start();
//        SquareZlVector mergedZiArray = mux(mergedXiArray, mergedZlArray);
//        stopWatch.stop();
//        long mux2 = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        // split
//        stopWatch.reset();
//        stopWatch.start();
//        int[] nums = Arrays.stream(xiArray)
//            .mapToInt(SquareZ2Vector::getNum).toArray();
//        SquareZlVector[] result = Arrays.stream(ZlVector.split(mergedZiArray.getZlVector(), nums))
//            .map(z -> SquareZlVector.create(z, false)).toArray(SquareZlVector[]::new);
//
//        stopWatch.stop();
//        long mux3 = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        LOGGER.info("### mux1: " + mux1 + " ms.");
//        LOGGER.info("### mux2: " + mux2 + " ms.");
//        LOGGER.info("### mux3: " + mux3 + " ms.");
//
//        return result;
//    }
}
