package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;


/**
 * NC-BitOt协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/08/11
 */
public abstract class AbstractNcBitOtSender extends AbstractSecureTwoPartyPto implements NcBitOtSender {
    /**
     * 配置项
     */
    private final NcBitOtConfig config;
    /**
     * 数量
     */
    protected int num;

    protected AbstractNcBitOtSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, NcBitOtConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public NcBitOtFactory.NcBitOtType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int num) {
        assert num > 0 && num <= config.maxAllowNum()
                : "num must be in range : (0, " + config.maxAllowNum() + "]: " + num;
        this.num = num;
        initialized = false;
    }

    protected void setPtoInput() {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        extraInfo++;
    }
}
