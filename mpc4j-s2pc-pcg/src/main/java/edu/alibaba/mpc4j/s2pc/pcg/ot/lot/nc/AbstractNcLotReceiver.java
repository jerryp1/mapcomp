package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * NC-2^l选1-OT协议接收方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/0816
 */
public abstract class AbstractNcLotReceiver extends AbstractTwoPartyPto implements NcLotReceiver {
    /**
     * 配置项
     */
    private final NcLotConfig config;
    /**
     * 数量
     */
    protected int num;
    /**
     * 输入比特长度
     */
    protected int inputBitLength;
    /**
     * 输入字节长度
     */
    protected int inputByteLength;

    protected AbstractNcLotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, NcLotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int inputBitLength, int num) {
        MathPreconditions.checkPositive("inputBitLength", inputBitLength);
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxAllowNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkReadyState();
        extraInfo++;
    }
}
