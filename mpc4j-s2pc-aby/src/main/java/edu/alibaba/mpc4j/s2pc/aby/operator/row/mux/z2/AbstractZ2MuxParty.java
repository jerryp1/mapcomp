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

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract Z2 mux party.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
public abstract class AbstractZ2MuxParty extends AbstractTwoPartyPto implements Z2MuxParty {
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
        // check
        Arrays.stream(f).forEach(x -> Preconditions.checkArgument(!x.isPlain(), "Mux is only supported for inputs in secret state."));
        Arrays.stream(xi).forEach(each -> Arrays.stream(each).forEach(x ->
            Preconditions.checkArgument(!x.isPlain(), "Mux is only supported for inputs in secret state.")));
        // merge
        SquareZ2Vector mergedXiArray = SquareZ2Vector.mergeWithPadding(f);
        SquareZ2Vector[] mergeArray = IntStream.range(0, xi[0].length).mapToObj(i -> {
            SquareZ2Vector[] tmp = Arrays.stream(xi).map(each -> each[i]).toArray(SquareZ2Vector[]::new);
            return SquareZ2Vector.mergeWithPadding(tmp);
        }).toArray(SquareZ2Vector[]::new);
        // mux
        SquareZ2Vector[] mergedZiArray = mux(mergedXiArray, mergeArray);
        // split
        int[] nums = Arrays.stream(f).mapToInt(SquareZ2Vector::getNum).toArray();
        SquareZ2Vector[][] tmp = Arrays.stream(mergedZiArray).map(each -> each.splitWithPadding(nums)).toArray(SquareZ2Vector[][]::new);
        return IntStream.range(0, xi.length).mapToObj(i ->
            Arrays.stream(tmp).map(x -> x[i]).toArray(SquareZ2Vector[]::new)).toArray(SquareZ2Vector[][]::new);
    }
}
