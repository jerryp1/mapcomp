package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * abstract Zl mux party.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public abstract class AbstractZlMuxParty extends AbstractTwoPartyPto implements ZlMuxParty {
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

    public AbstractZlMuxParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMuxConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZ2Vector xi, SquareZlVector yi) {
        MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi.getNum());
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        zl = yi.getZl();
        byteL = zl.getByteL();
    }

    @Override
    public SquareZlVector[] mux(SquareZ2Vector[] xiArray, SquareZlVector[] yiArray) throws MpcAbortException {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // check
        Arrays.stream(xiArray).forEach(x -> Preconditions.checkArgument(!x.isPlain(), "Mux is only supported for inputs in secret state."));
        Arrays.stream(yiArray).forEach(y -> Preconditions.checkArgument(!y.isPlain(), "Mux is only supported for inputs in secret state."));
        // merge
        SquareZ2Vector mergedXiArray = SquareZ2Vector.create(BitVectorFactory.merge(Arrays.stream(xiArray)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new)), false);
//        mergedXiArray = SquareZ2Vector.create(BitVectorFactory.create(mergedXiArray.getBitVector().bitNum(), mergedXiArray.getBitVector().getBytes()), false);
        SquareZlVector mergedZlArray = SquareZlVector.create(ZlVector.merge(Arrays.stream(yiArray)
            .map(SquareZlVector::getZlVector).toArray(ZlVector[]::new)), false);
        stopWatch.stop();
        long mux1 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        // mux
        stopWatch.reset();
        stopWatch.start();
        SquareZlVector mergedZiArray = mux(mergedXiArray, mergedZlArray);
        stopWatch.stop();
        long mux2 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        // split
        stopWatch.reset();
        stopWatch.start();
        int[] nums = Arrays.stream(xiArray)
            .mapToInt(SquareZ2Vector::getNum).toArray();
        SquareZlVector[] result = Arrays.stream(ZlVector.split(mergedZiArray.getZlVector(), nums))
            .map(z -> SquareZlVector.create(z, false)).toArray(SquareZlVector[]::new);

        stopWatch.stop();
        long mux3 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        System.out.println("### mux1: " + mux1 + " ms.");
        System.out.println("### mux2: " + mux2 + " ms.");
        System.out.println("### mux3: " + mux3 + " ms.");

        return result;
    }
}
