package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

/**
 * NC-LOT协议接收方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/0816
 */
public abstract class AbstractNcLotReceiver extends AbstractSecureTwoPartyPto implements NcLotReceiver {
    /**
     * 配置项
     */
    private final NcLotConfig config;
    /**
     * 数量。
     */
    protected int num;
    /**
     * 最大选择值比特长度。
     */
    protected int inputBitLength;

    protected AbstractNcLotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, NcLotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }


    @Override
    public NcLotFactory.NcLotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int inputBitLength, int num) {
        assert num > 0 && num <= config.maxAllowNum()
                : "num must be in range: (0, " + config.maxAllowNum() + "]: " + num;
        this.num = num;
        this.inputBitLength = inputBitLength;
        initialized = false;
    }

    protected void setPtoInput() {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        extraInfo++;
    }
}
