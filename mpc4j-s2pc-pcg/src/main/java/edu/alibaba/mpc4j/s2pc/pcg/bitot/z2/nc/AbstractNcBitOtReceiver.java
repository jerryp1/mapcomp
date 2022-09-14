package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

/**
 * NC-BitOt协议接收方。
 *
 * @author Hanwen Feng
 */
public abstract class AbstractNcBitOtReceiver extends AbstractSecureTwoPartyPto implements NcBitOtReceiver {
    /**
     * 配置项
     */
    private final NcBitOtConfig config;
    /**
     * 数量
     */
    protected int num;


    protected AbstractNcBitOtReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, NcBitOtConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public NcBitOtFactory.NcBitOtType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int num) {
        assert num > 0 && num <= config.maxAllowNum()
                : "num must be in range: (0, " + config.maxAllowNum() + "]: " + num;
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
