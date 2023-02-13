package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;

import java.math.BigInteger;

/**
 * Zp-核VOLE接收方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/06/13
 */
public abstract class AbstractZpCoreVoleReceiver extends AbstractTwoPartyPto implements ZpCoreVoleReceiver {
    /**
     * 关联值Δ
     */
    protected BigInteger delta;
    /**
     * 素数域Zp
     */
    protected Zp zp;
    /**
     * 有限域比特长度
     */
    protected int l;
    /**
     * 质数字节长度
     */
    protected int primeByteLength;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;

    protected AbstractZpCoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, ZpCoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(BigInteger prime, BigInteger delta, int maxNum) {
        zp = ZpFactory.createInstance(envType, prime);
        l = zp.getL();
        primeByteLength = zp.getPrimeByteLength();
        Preconditions.checkArgument(
            zp.validateRangeElement(delta),
            "Δ must be in range [0, %s): %s", zp.getRangeBound(), delta
        );
        this.delta = delta;
        assert maxNum > 0 : "max num must be greater than 0: " + maxNum;
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
