package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * NC-LOT发送方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/08/16
 */
public abstract class AbstractNcLotSender extends AbstractSecureTwoPartyPto implements NcLotSender{
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

    protected AbstractNcLotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, NcLotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
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
