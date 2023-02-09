package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * Abstract secure two-party protocol.
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
public abstract class AbstractSecureTwoPartyPto extends AbstractTwoPartyPto implements SecurePto {
    /**
     * environment
     */
    protected final EnvType envType;
    /**
     * secure random state
     */
    protected SecureRandom secureRandom;
    /**
     * parallel computing
     */
    public boolean parallel;
    /**
     * 是否完成初始化
     */
    protected boolean initialized;

    protected AbstractSecureTwoPartyPto(PtoDesc ptoDesc, Rpc rpc, Party otherParty, SecurePtoConfig config) {
        super(ptoDesc, rpc, otherParty);
        envType = config.getEnvType();
        secureRandom = new SecureRandom();
        parallel = false;
        initialized = false;
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public boolean getParallel() {
        return parallel;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }
}
