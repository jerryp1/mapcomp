package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

/**
 * abstract 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public abstract class AbstractLnotReceiver extends AbstractTwoPartyPto implements LnotReceiver {
    /**
     * the config
     */
    protected final LnotConfig config;
    /**
     * choice bit length
     */
    protected int l;
    /**
     * choice byte length
     */
    protected int byteL;
    /**
     * maximal choice
     */
    protected int n;
    /**
     * max round num
     */
    protected int maxRoundNum;
    /**
     * update num
     */
    protected long updateNum;
    /**
     * choice array
     */
    protected int[] choiceArray;
    /**
     * num
     */
    protected int num;

    public AbstractLnotReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, LnotConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(int l, int maxRoundNum, int updateNum) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, IntUtils.MAX_L);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        n = 1 << l;
        MathPreconditions.checkPositiveInRangeClosed("maxRoundNum", maxRoundNum, config.maxBaseNum(l));
        this.maxRoundNum = maxRoundNum;
        MathPreconditions.checkGreaterOrEqual("updateNum", updateNum, maxRoundNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int[] choiceArray) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", choiceArray.length, maxRoundNum);
        this.choiceArray = choiceArray;
        num = choiceArray.length;
        extraInfo++;
    }
}
