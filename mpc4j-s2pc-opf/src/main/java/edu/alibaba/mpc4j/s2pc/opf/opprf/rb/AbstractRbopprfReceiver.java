package edu.alibaba.mpc4j.s2pc.opf.opprf.rb;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * abstract Related-Batch OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public abstract class AbstractRbopprfReceiver extends AbstractTwoPartyPto implements RbopprfReceiver {
    /**
     * max batch size
     */
    protected int maxBatchSize;
    /**
     * max point num
     */
    protected int maxPointNum;
    /**
     * the input / output bit length
     */
    protected int l;
    /**
     * the input / output byte length
     */
    protected int byteL;
    /**
     * batch size
     */
    protected int batchSize;
    /**
     * the batched input array.
     */
    protected byte[][] inputArray;
    /**
     * the number of target programmed points
     */
    protected int pointNum;

    protected AbstractRbopprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, RbopprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int maxBatchSize, int maxPointNum) {
        MathPreconditions.checkGreater("max batch size", maxBatchSize, 1);
        this.maxBatchSize = maxBatchSize;
        MathPreconditions.checkPositive("max point num", maxPointNum);
        this.maxPointNum = maxPointNum;
        initState();
    }

    protected void setPtoInput(int l, byte[][] inputArray, int pointNum) {
        checkInitialized();
        // check l
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // check batch size
        batchSize = inputArray.length;
        MathPreconditions.checkGreater("batch size", batchSize, 1);
        MathPreconditions.checkLessOrEqual("batch size", batchSize, maxBatchSize);
        // we do not even require that input array are distinct.
        this.inputArray = inputArray;
        // check point num
        MathPreconditions.checkPositive("point num", pointNum);
        MathPreconditions.checkLessOrEqual("point num", pointNum, maxPointNum);
        this.pointNum = pointNum;
        extraInfo++;
    }
}
