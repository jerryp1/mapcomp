package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

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
    protected boolean parallel;
    /**
     * 是否完成初始化
     */
    protected boolean initialized;
    /**
     * sub secure protocols
     */
    protected List<SecurePto> subSecurePtos;

    protected AbstractSecureTwoPartyPto(PtoDesc ptoDesc, Rpc rpc, Party otherParty, SecurePtoConfig config) {
        super(ptoDesc, rpc, otherParty);
        envType = config.getEnvType();
        secureRandom = new SecureRandom();
        subSecurePtos = new ArrayList<>(MAX_SUB_PROTOCOL_NUM);
        parallel = false;
        initialized = false;
    }

    protected void addSecureSubPtos(SecurePto subSecurePto) {
        subSecurePtos.add(subSecurePto);
        MathPreconditions.checkLessOrEqual("# of sub-secure protocols", subSecurePtos.size(), MAX_SUB_PROTOCOL_NUM);
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
        // set sub-protocols
        for (SecurePto subSecurePto : subSecurePtos) {
            subSecurePto.setParallel(parallel);
        }
    }

    @Override
    public void setSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
        // set sub-protocols
        for (SecurePto subSecurePto : subSecurePtos) {
            subSecurePto.setSecureRandom(secureRandom);
        }
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
