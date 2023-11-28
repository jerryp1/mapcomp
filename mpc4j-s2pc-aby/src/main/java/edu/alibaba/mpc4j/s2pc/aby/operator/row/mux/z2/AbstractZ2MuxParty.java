package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public abstract class AbstractZ2MuxParty extends AbstractTwoPartyPto implements Z2MuxParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractZ2MuxParty.class);
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
    protected int bitLen;
    /**
     * l in bytes
     */
    protected int byteL;

    public AbstractZ2MuxParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Z2MuxConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZ2Vector xi, SquareZ2Vector[] yi) {
        MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi[0].getNum());
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        bitLen = yi.length;
        byteL = CommonUtils.getByteLength(bitLen);
    }

    @Override
    public SquareZ2Vector[][] mux(SquareZ2Vector[] f, SquareZ2Vector[][] xi) throws MpcAbortException {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // check
        Arrays.stream(f).forEach(x -> Preconditions.checkArgument(!x.isPlain(), "Mux is only supported for inputs in secret state."));
        Arrays.stream(xi).forEach(each -> Arrays.stream(each).forEach(x -> Preconditions.checkArgument(!x.isPlain(), "Mux is only supported for inputs in secret state.")));
        // merge
        SquareZ2Vector mergedXiArray = SquareZ2Vector.mergeWithPadding(f);
        SquareZ2Vector[] mergeArray = IntStream.range(0, xi[0].length).mapToObj(i -> {
            SquareZ2Vector[] tmp = Arrays.stream(xi).map(each -> each[i]).toArray(SquareZ2Vector[]::new);
            return SquareZ2Vector.mergeWithPadding(tmp);
        }).toArray(SquareZ2Vector[]::new);
        stopWatch.stop();
        long mux1 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        // mux
        stopWatch.reset();
        stopWatch.start();
        SquareZ2Vector[] mergedZiArray = mux(mergedXiArray, mergeArray);
        stopWatch.stop();
        long mux2 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        // split
        stopWatch.reset();
        stopWatch.start();
        int[] nums = Arrays.stream(f).mapToInt(SquareZ2Vector::getNum).toArray();
        SquareZ2Vector[][] tmp = Arrays.stream(mergedZiArray).map(each -> each.splitWithPadding(nums)).toArray(SquareZ2Vector[][]::new);
        SquareZ2Vector[][] result = IntStream.range(0, xi.length).mapToObj(i ->
            Arrays.stream(tmp).map(x -> x[i]).toArray(SquareZ2Vector[]::new)).toArray(SquareZ2Vector[][]::new);

        stopWatch.stop();
        long mux3 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("### mux1: " + mux1 + " ms.");
        LOGGER.info("### mux2: " + mux2 + " ms.");
        LOGGER.info("### mux3: " + mux3 + " ms.");

        return result;
    }
}
