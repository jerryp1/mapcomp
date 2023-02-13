package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;

/**
 * Zp64-核VOLE发送方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public abstract class AbstractZp64CoreVoleReceiver extends AbstractTwoPartyPto implements Zp64CoreVoleReceiver {
    /**
     * 关联值Δ
     */
    protected long delta;
    /**
     * Zp64
     */
    protected Zp64 zp64;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;

    protected AbstractZp64CoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Zp64CoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(long prime, long delta, int maxNum) {
        zp64 = Zp64Factory.createInstance(envType, prime);
        Preconditions.checkArgument(
            zp64.validateRangeElement(delta),
            "Δ must be in range [0, %s): %s", zp64.getRangeBound(), delta
        );
        this.delta = delta;
        MathPreconditions.checkPositive("maxNum", maxNum);
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

