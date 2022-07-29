package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.vole;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64Factory;

/**
 * Zp64Vole发送方抽象类
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public abstract class AbstractZp64VoleReceiver extends AbstractSecureTwoPartyPto implements Zp64VoleReceiver {
    /**
     * 配置项
     */
    private final Zp64VoleConfig config;
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

    protected AbstractZp64VoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Zp64VoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public Zp64VoleFactory.Zp64VoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(long prime, long delta, int maxNum) {
        zp64 = Zp64Factory.createInstance(envType, prime);
        assert zp64.validateRangeElement(delta) : "Δ must be in range [0, " + zp64.getRangeBound() + "): " + delta;
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

