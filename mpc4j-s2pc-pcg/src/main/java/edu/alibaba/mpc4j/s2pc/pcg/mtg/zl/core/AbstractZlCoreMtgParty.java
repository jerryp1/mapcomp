package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;

/**
 * 核l比特布三元组生成协议。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public abstract class AbstractZlCoreMtgParty extends AbstractTwoPartyPto implements ZlCoreMtgParty {
    /**
     * 配置项
     */
    protected final ZlCoreMtgConfig config;
    /**
     * 比特长度
     */
    protected int l;
    /**
     * 取模所用的遮掩值
     */
    protected BigInteger mask;
    /**
     * 字节长度
     */
    protected int byteL;
    /**
     * 最大数量
     */
    protected int maxNum;
    /**
     * 数量
     */
    protected int num;

    public AbstractZlCoreMtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlCoreMtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        l = config.getL();
        mask = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        byteL = CommonUtils.getByteLength(l);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxNum", maxNum, config.maxAllowNum());
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkReadyState();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }
}
