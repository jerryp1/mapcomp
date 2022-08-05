package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * Z2-RVOLE发送方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public abstract class AbstractZ2RvoleSender extends AbstractSecureTwoPartyPto implements Z2RvoleSender {
    /**
     * 配置项
     */
    private final Z2RvoleConfig config;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;
    /**
     * 字节数量
     */
    protected int byteNum;
    /**
     * x
     */
    protected byte[] x;

    protected AbstractZ2RvoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Z2RvoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public Z2RvoleFactory.Z2RvoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxNum) {
        assert maxNum > 0: "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(byte[] x, int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum;
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        assert x.length == byteNum && BytesUtils.isReduceByteArray(x, num);
        this.x = BytesUtils.clone(x);
        extraInfo++;
    }
}
