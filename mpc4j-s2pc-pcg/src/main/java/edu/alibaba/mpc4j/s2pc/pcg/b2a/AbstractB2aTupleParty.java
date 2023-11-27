package edu.alibaba.mpc4j.s2pc.pcg.b2a;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * abstract Z2 multiplication triple generation party.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public abstract class AbstractB2aTupleParty extends AbstractTwoPartyPto implements B2aTupleParty {
    /**
     * config
     */
    protected final B2aTupleConfig config;
    /**
     * update num
     */
    protected long updateNum;
    /**
     * num
     */
    protected int num;
    /**
     * byte num
     */
    protected int byteNum;
    /**
     * zl
     */
    protected Zl zl;

    public AbstractB2aTupleParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, B2aTupleConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        this.zl = config.getZl();
    }

    protected void setInitInput(int updateNum) {
        MathPreconditions.checkPositive("updateNum", updateNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo += num;
    }
}
