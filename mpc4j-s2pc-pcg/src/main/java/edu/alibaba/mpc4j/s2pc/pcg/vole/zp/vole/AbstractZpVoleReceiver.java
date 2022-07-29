package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole.ZpVoleFactory.*;

import java.math.BigInteger;

/**
 * ZpVole接收方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/06/13
 */
public abstract class AbstractZpVoleReceiver extends AbstractSecureTwoPartyPto implements ZpVoleReceiver {
    /**
     * 配置项
     */
    private final ZpVoleConfig config;
    /**
     * 关联值Δ
     */
    protected BigInteger delta;
    /**
     * 素数域Zp
     */
    protected BigInteger prime;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;

    protected AbstractZpVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, ZpVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public ZpVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(BigInteger prime, BigInteger delta, int maxNum) {
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        this.prime = prime;
        BigInteger maxValidDelta = BigInteger.ONE.shiftLeft(prime.bitLength() - 1);
        assert BigIntegerUtils.greaterOrEqual(delta, BigInteger.ZERO) && BigIntegerUtils.less(delta, maxValidDelta)
            : "Δ must be in range [0, " + maxValidDelta + "): " + delta;
        this.delta = delta;
        assert maxNum > 0 : "maxNum must be greater than 0";
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        extraInfo++;
    }
}
